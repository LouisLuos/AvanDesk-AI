package com.avandesk.api.chamado.service;

import com.avandesk.api.catalogo.entity.Categoria;
import com.avandesk.api.catalogo.entity.SistemaEquipamento;
import com.avandesk.api.catalogo.service.CategoriaService;
import com.avandesk.api.catalogo.service.SistemaEquipamentoService;
import com.avandesk.api.chamado.dto.ChamadoDetalheResponse;
import com.avandesk.api.chamado.dto.ChamadoRequest;
import com.avandesk.api.chamado.dto.ChamadoResponse;
import com.avandesk.api.chamado.entity.Chamado;
import com.avandesk.api.chamado.entity.DirecaoContato;
import com.avandesk.api.chamado.entity.EventoHistorico;
import com.avandesk.api.chamado.entity.RegistroContato;
import com.avandesk.api.chamado.entity.Solicitante;
import com.avandesk.api.chamado.entity.TipoEvento;
import com.avandesk.api.chamado.repository.ChamadoRepository;
import com.avandesk.api.chamado.repository.RegistroContatoRepository;
import com.avandesk.api.core.config.NexoProperties;
import com.avandesk.api.core.exception.RecursoNaoEncontradoException;
import com.avandesk.api.unidade.entity.Unidade;
import com.avandesk.api.unidade.service.UnidadeService;
import com.avandesk.api.usuario.entity.Usuario;
import com.avandesk.api.usuario.service.UsuarioLogadoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChamadoService {

    private static final ZoneId FUSO_DO_CODIGO = ZoneId.of("America/Recife");
    private static final Duration LIMITE_INICIO_REGISTRO = Duration.ofHours(2);

    private final ChamadoRepository chamadoRepository;
    private final RegistroContatoRepository registroContatoRepository;
    private final EventoHistoricoService eventoHistoricoService;
    private final UnidadeService unidadeService;
    private final CategoriaService categoriaService;
    private final SistemaEquipamentoService sistemaEquipamentoService;
    private final SolicitanteService solicitanteService;
    private final UsuarioLogadoService usuarioLogadoService;
    private final NexoProperties nexoProperties;

    @Transactional
    public ChamadoResponse registrar(ChamadoRequest request) {
        Unidade unidade = unidadeService.obterAtivaParaNovoChamado(request.unidadeId());
        Categoria categoria = categoriaService.obterAtivaParaNovoChamado(request.categoriaId());
        SistemaEquipamento sistema = request.sistemaEquipamentoId() == null
            ? null
            : sistemaEquipamentoService.obterAtivoParaNovoChamado(request.sistemaEquipamentoId());
        Usuario autor = usuarioLogadoService.obterUsuarioLogado();
        Solicitante solicitante = solicitanteService.identificarOuCriar(
            request.solicitante().nome(), request.solicitante().contato());


        Instant abertoEm = Instant.now().truncatedTo(ChronoUnit.MICROS);

        Chamado chamado = new Chamado();
        chamado.setCodigo(gerarCodigo(abertoEm));
        chamado.setUnidade(unidade);
        chamado.setSolicitante(solicitante);
        chamado.setCanalOrigem(request.canalOrigem());
        chamado.setDescricao(request.descricao().trim());
        chamado.setCategoria(categoria);
        chamado.setSistemaEquipamento(sistema);
        chamado.setCriadoPor(autor);
        chamado.setAbertoEm(abertoEm);
        chamado.setRegistroIniciadoEm(validarInicioRegistro(request.registroIniciadoEm(), abertoEm));
        chamado.setOrigemDado(nexoProperties.origemDado());
        chamadoRepository.save(chamado);

        RegistroContato contato = new RegistroContato();
        contato.setChamado(chamado);
        contato.setSolicitante(solicitante);
        contato.setCanal(chamado.getCanalOrigem());
        contato.setDirecao(DirecaoContato.RECEBIDO);
        contato.setConteudo(chamado.getDescricao());
        contato.setOcorridoEm(abertoEm);
        contato.setRegistradoPor(autor);
        contato.setOrigemDado(chamado.getOrigemDado());
        registroContatoRepository.save(contato);

        eventoHistoricoService.registrar(chamado, TipoEvento.CHAMADO_ABERTO, autor,
            Map.of("canal", chamado.getCanalOrigem().name()));
        eventoHistoricoService.registrar(chamado, TipoEvento.REGISTRO_DE_CONTATO_RECEBIDO, autor,
            Map.of("registro_contato_id", contato.getId().toString()));

        return ChamadoResponse.de(chamado);
    }


    @Transactional(readOnly = true)
    public ChamadoDetalheResponse detalhar(UUID id) {
        Chamado chamado = chamadoRepository.findDetalheById(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Chamado não encontrado."));
        List<RegistroContato> contatos = registroContatoRepository.findByChamadoIdOrderByOcorridoEmAsc(id);
        List<EventoHistorico> eventos = eventoHistoricoService.listarDoChamado(id);
        return ChamadoDetalheResponse.de(chamado, contatos, eventos);
    }

    private String gerarCodigo(Instant abertoEm) {
        int ano = abertoEm.atZone(FUSO_DO_CODIGO).getYear();
        return "LUM-%d-%06d".formatted(ano, chamadoRepository.proximoNumeroCodigo());
    }

    private static Instant validarInicioRegistro(Instant inicio, Instant abertoEm) {
        if (inicio == null) {
            return null;
        }
        if (inicio.isAfter(abertoEm) || inicio.isBefore(abertoEm.minus(LIMITE_INICIO_REGISTRO))) {
            log.warn("registro_iniciado_em descartado: fora da janela de 2 h antes da abertura ({} s de diferenca)",
                Duration.between(inicio, abertoEm).toSeconds());
            return null;
        }
        return inicio;
    }
}
