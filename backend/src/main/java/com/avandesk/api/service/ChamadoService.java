package com.avandesk.api.service;

import com.avandesk.api.config.NexoProperties;
import com.avandesk.api.dto.ChamadoRequest;
import com.avandesk.api.dto.ChamadoResponse;
import com.avandesk.api.entity.Categoria;
import com.avandesk.api.entity.Chamado;
import com.avandesk.api.entity.DirecaoContato;
import com.avandesk.api.entity.RegistroContato;
import com.avandesk.api.entity.SistemaEquipamento;
import com.avandesk.api.entity.Solicitante;
import com.avandesk.api.entity.TipoEvento;
import com.avandesk.api.entity.Unidade;
import com.avandesk.api.entity.Usuario;
import com.avandesk.api.repository.ChamadoRepository;
import com.avandesk.api.repository.RegistroContatoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Map;

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
