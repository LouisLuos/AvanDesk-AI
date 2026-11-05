package com.avandesk.api.chamado.service;

import com.avandesk.api.chamado.dto.ChamadoDetalheResponse;
import com.avandesk.api.chamado.dto.EscalonamentoRequest;
import com.avandesk.api.chamado.dto.RetornoFornecedorRequest;
import com.avandesk.api.chamado.entity.Chamado;
import com.avandesk.api.chamado.entity.Escalonamento;
import com.avandesk.api.chamado.entity.MotivoEspera;
import com.avandesk.api.chamado.entity.StatusChamado;
import com.avandesk.api.chamado.entity.TipoEvento;
import com.avandesk.api.chamado.repository.ChamadoRepository;
import com.avandesk.api.chamado.repository.EscalonamentoRepository;
import com.avandesk.api.core.exception.OperacaoNaoPermitidaException;
import com.avandesk.api.core.exception.RecursoNaoEncontradoException;
import com.avandesk.api.core.exception.RegraNegocioException;
import com.avandesk.api.usuario.entity.Usuario;
import com.avandesk.api.usuario.service.UsuarioLogadoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/** HU07: escala o chamado para um fornecedor externo e registra o retorno. O responsável interno não muda (RN07). */
@Service
@RequiredArgsConstructor
public class EscalonamentoService {

    private static final Duration TOLERANCIA_RELOGIO = Duration.ofMinutes(1);

    private final ChamadoRepository chamadoRepository;
    private final EscalonamentoRepository escalonamentoRepository;
    private final EventoHistoricoService eventoHistoricoService;
    private final UsuarioLogadoService usuarioLogadoService;
    private final ChamadoService chamadoService;

    @Transactional
    public ChamadoDetalheResponse escalar(UUID chamadoId, EscalonamentoRequest request) {
        Chamado chamado = buscarChamado(chamadoId);
        if (chamado.getStatus() != StatusChamado.EM_ATENDIMENTO) {
            throw new OperacaoNaoPermitidaException(
                "Só é possível escalar um chamado EM_ATENDIMENTO (status atual: %s).".formatted(chamado.getStatus()));
        }
        if (chamado.getResponsavel() == null) {
            throw new OperacaoNaoPermitidaException("Atribua um responsável antes de escalar o chamado.");
        }
        List<Escalonamento> escalonamentos = chamado.getEscalonamentos();
        if (escalonamentos.stream().anyMatch(Escalonamento::aberto)) {
            throw new OperacaoNaoPermitidaException(
                "O chamado já tem um escalonamento sem retorno. Registre o retorno antes de escalar de novo.");
        }
        Instant enviadoEm = dataOuAgora(request.enviadoEm(), "enviado_em");
        if (enviadoEm.isBefore(chamado.getAbertoEm().truncatedTo(ChronoUnit.MINUTES))) {
            throw new RegraNegocioException("O campo 'enviado_em' não pode ser anterior à abertura do chamado.");
        }
        Instant ultimoRetorno = escalonamentos.stream()
            .map(Escalonamento::getRetornoEm)
            .filter(Objects::nonNull)
            .max(Comparator.naturalOrder())
            .orElse(null);
        if (ultimoRetorno != null && enviadoEm.isBefore(ultimoRetorno)) {
            throw new RegraNegocioException(
                "O campo 'enviado_em' não pode ser anterior ao retorno do escalonamento anterior.");
        }
        Usuario autor = usuarioLogadoService.obterUsuarioLogado();

        Escalonamento escalonamento = new Escalonamento();
        escalonamento.setChamado(chamado);
        escalonamento.setFornecedor(request.fornecedor().trim());
        escalonamento.setProtocoloExterno(textoOuNulo(request.protocoloExterno()));
        escalonamento.setObservacao(textoOuNulo(request.observacao()));
        escalonamento.setResponsavel(chamado.getResponsavel());
        escalonamento.setEnviadoPor(autor);
        escalonamento.setEnviadoEm(enviadoEm);
        escalonamento.setOrigemDado(chamado.getOrigemDado());
        escalonamentoRepository.save(escalonamento);

        escalonamentos.add(escalonamento);

        chamado.setStatus(StatusChamado.AGUARDANDO);
        chamado.setMotivoEspera(MotivoEspera.FORNECEDOR);
        chamado.setEsperaIniciadaEm(enviadoEm);

        Map<String, Object> dados = new LinkedHashMap<>();
        dados.put("status_anterior", StatusChamado.EM_ATENDIMENTO.name());
        dados.put("escalonamento_id", escalonamento.getId().toString());
        dados.put("fornecedor", escalonamento.getFornecedor());
        if (escalonamento.getProtocoloExterno() != null) {
            dados.put("protocolo_externo", escalonamento.getProtocoloExterno());
        }
        eventoHistoricoService.registrar(chamado, TipoEvento.CHAMADO_ESCALADO, autor, dados);

        return chamadoService.detalhar(chamado.getId());
    }

    @Transactional
    public ChamadoDetalheResponse registrarRetorno(UUID chamadoId, UUID escalonamentoId,
                                                   RetornoFornecedorRequest request) {
        Chamado chamado = buscarChamado(chamadoId);
        Escalonamento escalonamento = chamado.getEscalonamentos().stream()
            .filter(e -> e.getId().equals(escalonamentoId))
            .findFirst()
            .orElseThrow(() -> new RecursoNaoEncontradoException("Escalonamento não encontrado neste chamado."));
        if (!escalonamento.aberto()) {
            throw new OperacaoNaoPermitidaException("O retorno deste escalonamento já foi registrado.");
        }
        if (chamado.getStatus() != StatusChamado.AGUARDANDO || chamado.getMotivoEspera() != MotivoEspera.FORNECEDOR) {
            throw new OperacaoNaoPermitidaException(
                "O chamado não está aguardando fornecedor (status atual: %s).".formatted(chamado.getStatus()));
        }
        Instant retornoEm = dataOuAgora(request.retornoEm(), "retorno_em");
        if (retornoEm.isBefore(escalonamento.getEnviadoEm())) {
            throw new RegraNegocioException("O campo 'retorno_em' não pode ser anterior ao envio ao fornecedor.");
        }
        Usuario autor = usuarioLogadoService.obterUsuarioLogado();

        escalonamento.setRetorno(request.retorno().trim());
        escalonamento.setRetornoEm(retornoEm);
        escalonamento.setRetornoRegistradoPor(autor);
        chamado.setStatus(StatusChamado.EM_ATENDIMENTO);
        chamado.setMotivoEspera(null);
        chamado.setEsperaIniciadaEm(null);

        Map<String, Object> dados = new LinkedHashMap<>();
        dados.put("status_anterior", StatusChamado.AGUARDANDO.name());
        dados.put("escalonamento_id", escalonamento.getId().toString());
        dados.put("fornecedor", escalonamento.getFornecedor());
        eventoHistoricoService.registrar(chamado, TipoEvento.RETORNO_RECEBIDO, autor, dados);

        return chamadoService.detalhar(chamado.getId());
    }

    private Chamado buscarChamado(UUID id) {
        return chamadoRepository.findDetalheById(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Chamado não encontrado."));
    }

    /** Sem data, vale agora. Até 1 minuto à frente (diferença de relógio) também vira agora, para não gravar futuro. */
    private static Instant dataOuAgora(Instant data, String campo) {
        Instant agora = Instant.now().truncatedTo(ChronoUnit.MICROS);
        if (data == null) {
            return agora;
        }
        if (data.isAfter(agora.plus(TOLERANCIA_RELOGIO))) {
            throw new RegraNegocioException("O campo '%s' não pode estar no futuro.".formatted(campo));
        }
        return data.isAfter(agora) ? agora : data.truncatedTo(ChronoUnit.MICROS);
    }

    private static String textoOuNulo(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }
}
