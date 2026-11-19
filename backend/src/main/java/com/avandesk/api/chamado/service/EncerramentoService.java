package com.avandesk.api.chamado.service;

import com.avandesk.api.chamado.dto.ChamadoDetalheResponse;
import com.avandesk.api.chamado.dto.FechamentoRequest;
import com.avandesk.api.chamado.dto.ItemVerificacaoResponse;
import com.avandesk.api.chamado.dto.VerificacaoEncerramentoResponse;
import com.avandesk.api.chamado.entity.Chamado;
import com.avandesk.api.chamado.entity.StatusChamado;
import com.avandesk.api.chamado.entity.TipoEvento;
import com.avandesk.api.chamado.repository.ChamadoRepository;
import com.avandesk.api.chamado.repository.RegistroContatoRepository;
import com.avandesk.api.core.exception.OperacaoNaoPermitidaException;
import com.avandesk.api.core.exception.RecursoNaoEncontradoException;
import com.avandesk.api.usuario.entity.Usuario;
import com.avandesk.api.usuario.service.UsuarioLogadoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EncerramentoService {

    private final ChamadoRepository chamadoRepository;
    private final RegistroContatoRepository registroContatoRepository;
    private final EventoHistoricoService eventoHistoricoService;
    private final UsuarioLogadoService usuarioLogadoService;
    private final ChamadoService chamadoService;
    private final VerificadorRastreabilidade verificador = new VerificadorRastreabilidade();

    @Transactional(readOnly = true)
    public VerificacaoEncerramentoResponse verificar(UUID chamadoId) {
        Chamado chamado = buscarChamado(chamadoId);
        return verificador.verificar(chamado, registroContatoRepository.existsByChamadoId(chamadoId), null);
    }

    @Transactional
    public ChamadoDetalheResponse fechar(UUID chamadoId, FechamentoRequest request) {
        Chamado chamado = buscarChamado(chamadoId);
        if (chamado.getStatus() != StatusChamado.RESOLVIDO) {
            throw new OperacaoNaoPermitidaException(
                "Só é possível encerrar um chamado RESOLVIDO (status atual: %s).".formatted(chamado.getStatus()));
        }
        String justificativa = request == null ? null : textoOuNulo(request.justificativaSemCausaRaiz());
        VerificacaoEncerramentoResponse verificacao = verificador.verificar(
            chamado, registroContatoRepository.existsByChamadoId(chamadoId), justificativa);
        if (!verificacao.aprovado()) {
            throw new OperacaoNaoPermitidaException(
                "O chamado tem pendências de rastreabilidade.", verificacao.pendencias());
        }
        Usuario autor = usuarioLogadoService.obterUsuarioLogado();

        chamado.setStatus(StatusChamado.FECHADO);
        chamado.setFechadoEm(Instant.now().truncatedTo(ChronoUnit.MICROS));

        Map<String, Object> dados = new LinkedHashMap<>();
        dados.put("status_anterior", StatusChamado.RESOLVIDO.name());
        dados.put("itens_conformes", verificacao.itensConformes());
        dados.put("total_itens", verificacao.totalItens());
        dados.put("itens_verificados", verificacao.itens().stream().map(ItemVerificacaoResponse::codigo).toList());
        if (chamado.getCausaRaiz() == null || chamado.getCausaRaiz().isBlank()) {
            dados.put("justificativa_sem_causa_raiz", justificativa);
        }
        eventoHistoricoService.registrar(chamado, TipoEvento.CHAMADO_FECHADO, autor, dados);

        return chamadoService.detalhar(chamado.getId());
    }

    private Chamado buscarChamado(UUID id) {
        return chamadoRepository.findDetalheById(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Chamado não encontrado."));
    }

    private static String textoOuNulo(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }
}
