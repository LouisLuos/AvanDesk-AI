package com.avandesk.api.chamado.service;

import com.avandesk.api.chamado.dto.ChamadoDetalheResponse;
import com.avandesk.api.chamado.entity.Chamado;
import com.avandesk.api.chamado.entity.MotivoEspera;
import com.avandesk.api.chamado.entity.RegistroContato;
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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * HU03 (RN17): mescla um chamado duplicado (secundário) no principal. O secundário vai para MESCLADO e aponta o
 * principal; os contatos dele passam para o principal guardando a origem, sem apagar nada.
 */
@Service
@RequiredArgsConstructor
public class MesclagemService {

    private final ChamadoRepository chamadoRepository;
    private final RegistroContatoRepository registroContatoRepository;
    private final EventoHistoricoService eventoHistoricoService;
    private final UsuarioLogadoService usuarioLogadoService;
    private final ChamadoService chamadoService;

    @Transactional
    public ChamadoDetalheResponse mesclar(UUID secundarioId, UUID principalId) {
        if (secundarioId.equals(principalId)) {
            throw new OperacaoNaoPermitidaException("Não é possível mesclar um chamado nele mesmo.");
        }
        Map<UUID, Chamado> travados = chamadoRepository.travarParaMesclagem(List.of(secundarioId, principalId))
            .stream()
            .collect(Collectors.toMap(Chamado::getId, Function.identity()));
        Chamado secundario = travados.get(secundarioId);
        if (secundario == null) {
            throw new RecursoNaoEncontradoException("Chamado não encontrado.");
        }
        Chamado principal = travados.get(principalId);
        if (principal == null) {
            throw new RecursoNaoEncontradoException("Chamado principal não encontrado.");
        }
        StatusChamado statusAnterior = secundario.getStatus();
        if (!statusAnterior.podeIrPara(StatusChamado.MESCLADO)) {
            throw new OperacaoNaoPermitidaException(
                "Não é possível mesclar um chamado %s.".formatted(statusAnterior));
        }
        if (principal.getStatus().encerrado()) {
            throw new OperacaoNaoPermitidaException("O chamado principal %s está %s e não pode receber uma mesclagem."
                .formatted(principal.getCodigo(), principal.getStatus()));
        }
        if (secundario.getMotivoEspera() == MotivoEspera.FORNECEDOR) {
            throw new OperacaoNaoPermitidaException("Registre o retorno do fornecedor antes de mesclar o chamado.");
        }
        Usuario autor = usuarioLogadoService.obterUsuarioLogado();

        List<RegistroContato> contatos = registroContatoRepository.findByChamadoIdOrderByOcorridoEmAsc(secundarioId);
        for (RegistroContato contato : contatos) {
            if (contato.getChamadoOriginal() == null) {
                contato.setChamadoOriginal(secundario);
            }
            contato.setChamado(principal);
        }

        Usuario responsavelAnterior = secundario.getResponsavel();
        Usuario responsavelDoPrincipal = principal.getResponsavel();
        boolean trocaResponsavel = responsavelDoPrincipal != null
            && (responsavelAnterior == null || !responsavelAnterior.getId().equals(responsavelDoPrincipal.getId()));
        if (trocaResponsavel) {
            secundario.setResponsavel(responsavelDoPrincipal);
        }
        secundario.setStatus(StatusChamado.MESCLADO);
        secundario.setChamadoPrincipal(principal);
        secundario.setMotivoEspera(null);
        secundario.setEsperaIniciadaEm(null);
        principal.getChamadosMesclados().add(secundario);

        Map<String, Object> dadosMesclagem = new LinkedHashMap<>();
        dadosMesclagem.put("status_anterior", statusAnterior.name());
        dadosMesclagem.put("chamado_principal_id", principal.getId().toString());
        dadosMesclagem.put("chamado_principal_codigo", principal.getCodigo());
        dadosMesclagem.put("registros_contato_movidos", contatos.size());
        if (secundario.getPrioridadeFinal() != null) {
            dadosMesclagem.put("prioridade", secundario.getPrioridadeFinal().name());
        }
        if (trocaResponsavel) {
            dadosMesclagem.put("responsavel_id", responsavelDoPrincipal.getId().toString());
            dadosMesclagem.put("responsavel_nome", responsavelDoPrincipal.getNome());
            if (responsavelAnterior != null) {
                dadosMesclagem.put("responsavel_anterior_id", responsavelAnterior.getId().toString());
                dadosMesclagem.put("responsavel_anterior_nome", responsavelAnterior.getNome());
            }
        }
        eventoHistoricoService.registrar(secundario, TipoEvento.CHAMADO_MESCLADO, autor, dadosMesclagem);

        for (RegistroContato contato : contatos) {
            Map<String, Object> dadosRelato = new LinkedHashMap<>();
            dadosRelato.put("registro_contato_id", contato.getId().toString());
            dadosRelato.put("canal", contato.getCanal().name());
            dadosRelato.put("chamado_mesclado_id", secundario.getId().toString());
            dadosRelato.put("chamado_mesclado_codigo", secundario.getCodigo());
            eventoHistoricoService.registrar(principal, TipoEvento.RELATO_VINCULADO, autor, dadosRelato);
        }

        return chamadoService.detalhar(secundarioId);
    }
}
