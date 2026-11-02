package com.avandesk.api.chamado.service;

import com.avandesk.api.catalogo.entity.Categoria;
import com.avandesk.api.catalogo.entity.SistemaEquipamento;
import com.avandesk.api.catalogo.service.CategoriaService;
import com.avandesk.api.catalogo.service.SistemaEquipamentoService;
import com.avandesk.api.chamado.dto.ChamadoDetalheResponse;
import com.avandesk.api.chamado.dto.ChamadoRequest;
import com.avandesk.api.chamado.dto.ChamadoResponse;
import com.avandesk.api.chamado.dto.RegistroContatoRequest;
import com.avandesk.api.chamado.dto.StatusRequest;
import com.avandesk.api.chamado.entity.Chamado;
import com.avandesk.api.chamado.entity.DirecaoContato;
import com.avandesk.api.chamado.entity.EventoHistorico;
import com.avandesk.api.chamado.entity.MotivoEspera;
import com.avandesk.api.chamado.entity.RegistroContato;
import com.avandesk.api.chamado.entity.Solicitante;
import com.avandesk.api.chamado.entity.StatusChamado;
import com.avandesk.api.chamado.entity.TipoEvento;
import com.avandesk.api.chamado.repository.ChamadoRepository;
import com.avandesk.api.chamado.repository.RegistroContatoRepository;
import com.avandesk.api.core.config.NexoProperties;
import com.avandesk.api.core.exception.OperacaoNaoPermitidaException;
import com.avandesk.api.core.exception.RecursoNaoEncontradoException;
import com.avandesk.api.core.exception.RegraNegocioException;
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
    private static final Duration TOLERANCIA_RELOGIO = Duration.ofMinutes(1);

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
        boolean atribuirAoAutor = Boolean.TRUE.equals(request.atribuirAMim());
        if (atribuirAoAutor) {
            usuarioService.obterAnalistaAtivo(autor.getId());
        }
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
        if (atribuirAoAutor) {
            assumirNoRegistro(chamado, autor);
        }

        return ChamadoResponse.de(chamado);
    }

    @Transactional
    public ChamadoDetalheResponse registrarContato(UUID id, RegistroContatoRequest request) {
        Chamado chamado = buscarParaDetalhe(id);
        validarChamadoParaContato(chamado);
        Instant ocorridoEm = validarOcorridoEm(request.ocorridoEm(), chamado.getAbertoEm());
        String identificadorExterno = textoOuNulo(request.identificadorExterno());
        if (identificadorExterno != null
            && registroContatoRepository.existsByCanalAndIdentificadorExterno(request.canal(), identificadorExterno)) {
            throw new RegraNegocioException(
                "Este contato já foi registrado (identificador externo repetido no canal %s).".formatted(request.canal()));
        }
        Solicitante solicitante = request.solicitante() == null
            ? chamado.getSolicitante()
            : solicitanteService.identificarOuCriar(request.solicitante().nome(), request.solicitante().contato());
        DirecaoContato direcao = request.direcao() == null ? DirecaoContato.RECEBIDO : request.direcao();
        Usuario autor = usuarioLogadoService.obterUsuarioLogado();

        RegistroContato contato = new RegistroContato();
        contato.setChamado(chamado);
        contato.setSolicitante(solicitante);
        contato.setCanal(request.canal());
        contato.setDirecao(direcao);
        contato.setIdentificadorExterno(identificadorExterno);
        contato.setConteudo(request.conteudo().trim());
        contato.setOcorridoEm(ocorridoEm);
        contato.setRegistradoPor(autor);
        contato.setOrigemDado(chamado.getOrigemDado());
        registroContatoRepository.save(contato);

        if (direcao == DirecaoContato.ENVIADO) {
            registrarPrimeiraResposta(chamado);
        }
        Map<String, Object> dados = new LinkedHashMap<>();
        dados.put("registro_contato_id", contato.getId().toString());
        dados.put("canal", contato.getCanal().name());
        dados.put("direcao", direcao.name());
        eventoHistoricoService.registrar(chamado, TipoEvento.REGISTRO_DE_CONTATO_RECEBIDO, autor, dados);

        return montarDetalhe(chamado);
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
        eventoHistoricoService.registrar(chamado, TipoEvento.CHAMADO_ATRIBUIDO, autor,
            dadosDaAtribuicao(responsavel, anterior));

        return montarDetalhe(chamado);
    }

    @Transactional
    public ChamadoDetalheResponse alterarStatus(UUID id, StatusRequest request) {
        Chamado chamado = buscarParaDetalhe(id);
        StatusChamado anterior = chamado.getStatus();
        StatusChamado destino = request.status();
        validarTransicao(anterior, destino);

        Map<String, Object> dados = new LinkedHashMap<>();
        dados.put("status_anterior", anterior.name());
        TipoEvento tipo = switch (destino) {
            case EM_TRIAGEM -> TipoEvento.TRIAGEM_INICIADA;
            case EM_ATENDIMENTO -> iniciarAtendimento(chamado);
            case AGUARDANDO -> colocarEmEspera(chamado, request.motivoEspera(), dados);
            case CANCELADO -> cancelar(chamado, request.motivoCancelamento(), dados);
            default -> throw new OperacaoNaoPermitidaException(
                "O status %s tem operação própria e não pode ser definido por aqui.".formatted(destino));
        };
        Usuario autor = usuarioLogadoService.obterUsuarioLogado();

        chamado.setStatus(destino);
        registrarPrimeiraResposta(chamado);
        eventoHistoricoService.registrar(chamado, tipo, autor, dados);

        return montarDetalhe(chamado);
    }

    private static void validarTransicao(StatusChamado atual, StatusChamado destino) {
        if (atual.encerrado()) {
            throw new OperacaoNaoPermitidaException(
                "Não é possível alterar o status de um chamado %s.".formatted(atual));
        }
        if (atual == destino) {
            throw new OperacaoNaoPermitidaException("O chamado já está com status %s.".formatted(atual));
        }
        if (!atual.podeIrPara(destino)) {
            throw new OperacaoNaoPermitidaException("Não é possível ir de %s para %s.".formatted(atual, destino));
        }
    }

    private static TipoEvento iniciarAtendimento(Chamado chamado) {
        return switch (chamado.getStatus()) {
            case EM_TRIAGEM -> {
                if (chamado.getResponsavel() == null) {
                    throw new OperacaoNaoPermitidaException("Atribua um responsável antes de iniciar o atendimento.");
                }
                yield TipoEvento.CHAMADO_TRIADO;
            }
            case AGUARDANDO -> {
                chamado.setMotivoEspera(null);
                chamado.setEsperaIniciadaEm(null);
                yield TipoEvento.RETORNO_RECEBIDO;
            }
            default -> throw new OperacaoNaoPermitidaException(
                "A retomada de chamado %s ainda não está disponível.".formatted(chamado.getStatus()));
        };
    }

    private static TipoEvento colocarEmEspera(Chamado chamado, MotivoEspera motivo, Map<String, Object> dados) {
        if (motivo == null) {
            throw new OperacaoNaoPermitidaException("Informe o motivo_espera para colocar o chamado em AGUARDANDO.");
        }
        chamado.setMotivoEspera(motivo);
        chamado.setEsperaIniciadaEm(agora());
        dados.put("motivo_espera", motivo.name());
        return TipoEvento.CHAMADO_AGUARDANDO;
    }

    private static TipoEvento cancelar(Chamado chamado, String motivo, Map<String, Object> dados) {
        if (motivo == null || motivo.isBlank()) {
            throw new OperacaoNaoPermitidaException("Informe o motivo_cancelamento para cancelar o chamado.");
        }
        chamado.setMotivoCancelamento(motivo.trim());
        dados.put("motivo_cancelamento", motivo.trim());
        return TipoEvento.CHAMADO_CANCELADO;
    }

    /** Registro por ligação com "atribuir a mim": passa por EM_TRIAGEM até EM_ATENDIMENTO, com todos os eventos. */
    private void assumirNoRegistro(Chamado chamado, Usuario analista) {
        eventoHistoricoService.registrar(chamado, TipoEvento.TRIAGEM_INICIADA, analista,
            Map.of("status_anterior", StatusChamado.ABERTO.name()));
        chamado.setResponsavel(analista);
        eventoHistoricoService.registrar(chamado, TipoEvento.CHAMADO_ATRIBUIDO, analista,
            dadosDaAtribuicao(analista, null));
        eventoHistoricoService.registrar(chamado, TipoEvento.CHAMADO_TRIADO, analista,
            Map.of("status_anterior", StatusChamado.EM_TRIAGEM.name()));
        chamado.setStatus(StatusChamado.EM_ATENDIMENTO);
        registrarPrimeiraResposta(chamado);
    }

    private static Map<String, Object> dadosDaAtribuicao(Usuario responsavel, Usuario anterior) {
        Map<String, Object> dados = new LinkedHashMap<>();
        dados.put("responsavel_id", responsavel.getId().toString());
        dados.put("responsavel_nome", responsavel.getNome());
        if (anterior != null) {
            dados.put("responsavel_anterior_id", anterior.getId().toString());
            dados.put("responsavel_anterior_nome", anterior.getNome());
        }
        return dados;
    }

    private static void validarChamadoParaContato(Chamado chamado) {
        if (chamado.getStatus() == StatusChamado.MESCLADO && chamado.getChamadoPrincipal() != null) {
            throw new OperacaoNaoPermitidaException(
                "Este chamado foi mesclado ao %s. Registre o contato no chamado principal."
                    .formatted(chamado.getChamadoPrincipal().getCodigo()));
        }
        if (chamado.getStatus().encerrado()) {
            throw new OperacaoNaoPermitidaException(
                "Não é possível registrar contato em um chamado %s.".formatted(chamado.getStatus()));
        }
    }
    
    private static Instant validarOcorridoEm(Instant ocorridoEm, Instant abertoEm) {
        Instant agora = agora();
        if (ocorridoEm == null) {
            return agora;
        }
        if (ocorridoEm.isAfter(agora.plus(TOLERANCIA_RELOGIO))) {
            throw new RegraNegocioException("O campo 'ocorrido_em' não pode estar no futuro.");
        }
        if (ocorridoEm.isBefore(abertoEm.truncatedTo(ChronoUnit.MINUTES))) {
            throw new RegraNegocioException("O campo 'ocorrido_em' não pode ser anterior à abertura do chamado.");
        }
        return ocorridoEm.truncatedTo(ChronoUnit.MICROS);
    }

    private static String textoOuNulo(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
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
            chamado.setPrimeiraRespostaEm(agora());
        }
    }

    private static Instant agora() {
        return Instant.now().truncatedTo(ChronoUnit.MICROS);
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
