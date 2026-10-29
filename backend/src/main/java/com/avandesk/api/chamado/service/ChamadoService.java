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
import com.avandesk.api.core.exception.OperacaoNaoPermitidaException;
import com.avandesk.api.core.exception.RecursoNaoEncontradoException;
import com.avandesk.api.unidade.entity.Unidade;
import com.avandesk.api.unidade.service.UnidadeService;
import com.avandesk.api.usuario.entity.Usuario;
import com.avandesk.api.usuario.service.UsuarioLogadoService;
import com.avandesk.api.usuario.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
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
    private final UsuarioService usuarioService;
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
        return montarDetalhe(buscarParaDetalhe(id));
    }

    @Transactional
    public ChamadoDetalheResponse atribuirResponsavel(UUID id, UUID responsavelId) {
        Chamado chamado = buscarParaDetalhe(id);
        if (chamado.getStatus().encerrado()) {
            throw new OperacaoNaoPermitidaException(
                "Não é possível atribuir um chamado com status %s.".formatted(chamado.getStatus()));
        }
        Usuario responsavel = usuarioService.obterAnalistaAtivo(responsavelId);
        Usuario anterior = chamado.getResponsavel();
        if (anterior != null && anterior.getId().equals(responsavel.getId())) {
            throw new OperacaoNaoPermitidaException("O chamado já está com este responsável.");
        }
        Usuario autor = usuarioLogadoService.obterUsuarioLogado();

        chamado.setResponsavel(responsavel);
        registrarPrimeiraResposta(chamado);

        Map<String, Object> dados = new LinkedHashMap<>();
        dados.put("responsavel_id", responsavel.getId().toString());
        dados.put("responsavel_nome", responsavel.getNome());
        if (anterior != null) {
            dados.put("responsavel_anterior_id", anterior.getId().toString());
            dados.put("responsavel_anterior_nome", anterior.getNome());
        }
        eventoHistoricoService.registrar(chamado, TipoEvento.CHAMADO_ATRIBUIDO, autor, dados);

        return montarDetalhe(chamado);
    }

    private Chamado buscarParaDetalhe(UUID id) {
        return chamadoRepository.findDetalheById(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Chamado não encontrado."));
    }

    private ChamadoDetalheResponse montarDetalhe(Chamado chamado) {
        List<RegistroContato> contatos = registroContatoRepository.findByChamadoIdOrderByOcorridoEmAsc(chamado.getId());
        List<EventoHistorico> eventos = eventoHistoricoService.listarDoChamado(chamado.getId());
        return ChamadoDetalheResponse.de(chamado, contatos, eventos);
    }

    private static void registrarPrimeiraResposta(Chamado chamado) {
        if (chamado.getPrimeiraRespostaEm() == null) {
            chamado.setPrimeiraRespostaEm(Instant.now().truncatedTo(ChronoUnit.MICROS));
        }
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
