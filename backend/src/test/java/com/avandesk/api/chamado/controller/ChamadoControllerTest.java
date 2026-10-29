package com.avandesk.api.chamado.controller;

import com.avandesk.api.catalogo.dto.CategoriaResponse;
import com.avandesk.api.chamado.dto.ChamadoDetalheResponse;
import com.avandesk.api.chamado.dto.ChamadoRequest;
import com.avandesk.api.chamado.dto.ChamadoResponse;
import com.avandesk.api.chamado.dto.ChamadoResumoResponse;
import com.avandesk.api.chamado.dto.EventoHistoricoResponse;
import com.avandesk.api.chamado.dto.FilaChamadosResponse;
import com.avandesk.api.chamado.dto.FiltroChamados;
import com.avandesk.api.chamado.dto.RegistroContatoResponse;
import com.avandesk.api.chamado.dto.ResponsavelResponse;
import com.avandesk.api.chamado.dto.SolicitanteResponse;
import com.avandesk.api.chamado.entity.CanalOrigem;
import com.avandesk.api.chamado.entity.DirecaoContato;
import com.avandesk.api.chamado.entity.OrigemDado;
import com.avandesk.api.chamado.entity.Prioridade;
import com.avandesk.api.chamado.entity.StatusChamado;
import com.avandesk.api.chamado.entity.TipoEvento;
import com.avandesk.api.chamado.service.ChamadoService;
import com.avandesk.api.chamado.service.FilaChamadosService;
import com.avandesk.api.core.config.SecurityConfig;
import com.avandesk.api.core.exception.RecursoNaoEncontradoException;
import com.avandesk.api.core.exception.RegraNegocioException;
import com.avandesk.api.unidade.dto.UnidadeResumoResponse;
import com.avandesk.api.usuario.service.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ChamadoController.class)
@Import(SecurityConfig.class)
class ChamadoControllerTest {

    private static final String CORPO_VALIDO = """
        {
          "unidade_id": "%s",
          "canal_origem": "LIGACAO",
          "categoria_id": "%s",
          "solicitante": { "nome": "Recepção Boa Viagem", "contato": "81999999999" },
          "descricao": "Impressora de etiquetas da coleta não imprime.",
          "registro_iniciado_em": "2026-10-22T14:29:15Z"
        }
        """.formatted(UUID.randomUUID(), UUID.randomUUID());

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ChamadoService chamadoService;

    @MockitoBean
    private FilaChamadosService filaChamadosService;

    @MockitoBean
    private JwtService jwtService;

    @Test
    @WithMockUser
    void deveResponder201NoEnvelopePadraoAoRegistrar() throws Exception {
        UUID id = UUID.randomUUID();
        when(chamadoService.registrar(any(ChamadoRequest.class))).thenReturn(new ChamadoResponse(
            id, "LUM-2026-000001", StatusChamado.ABERTO, Instant.parse("2026-10-22T14:30:00Z"), OrigemDado.TESTE));

        mockMvc.perform(post("/api/v1/chamados").contentType(MediaType.APPLICATION_JSON).content(CORPO_VALIDO))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status_code").value(201))
            .andExpect(jsonPath("$.data.id").value(id.toString()))
            .andExpect(jsonPath("$.data.codigo").value("LUM-2026-000001"))
            .andExpect(jsonPath("$.data.status").value("ABERTO"))
            .andExpect(jsonPath("$.data.origem_dado").value("TESTE"))
            .andExpect(jsonPath("$.data.data_criacao").value(endsWith("Z")));
    }

    @Test
    @WithMockUser
    void deveResponder200ComDetalheContatosEHistoricoEmSnakeCase() throws Exception {
        UUID id = UUID.randomUUID();
        UUID unidadeId = UUID.randomUUID();
        UUID categoriaId = UUID.randomUUID();
        UUID contatoId = UUID.randomUUID();
        UUID eventoId = UUID.randomUUID();
        Instant abertoEm = Instant.parse("2026-10-22T14:30:00Z");

        ChamadoDetalheResponse detalhe = new ChamadoDetalheResponse(
            id,
            "LUM-2026-000001",
            StatusChamado.ABERTO,
            null,
            "Impressora de etiquetas da coleta não imprime.",
            CanalOrigem.LIGACAO,
            new UnidadeResumoResponse(unidadeId, "U.03 Boa Viagem"),
            new CategoriaResponse(categoriaId, "Equipamento de apoio"),
            null,
            new SolicitanteResponse("Recepção Boa Viagem", "+5581999999999"),
            null,
            null,
            abertoEm,
            null,
            OrigemDado.TESTE,
            List.of(new RegistroContatoResponse(
                contatoId,
                CanalOrigem.LIGACAO,
                DirecaoContato.RECEBIDO,
                "Impressora de etiquetas da coleta não imprime.",
                abertoEm,
                new SolicitanteResponse("Recepção Boa Viagem", "+5581999999999"),
                "Analista Dev Um"
            )),
            List.of(new EventoHistoricoResponse(
                eventoId,
                TipoEvento.CHAMADO_ABERTO,
                abertoEm,
                "Analista Dev Um",
                Map.of("canal", "LIGACAO")
            ))
        );

        when(chamadoService.detalhar(id)).thenReturn(detalhe);

        mockMvc.perform(get("/api/v1/chamados/{id}", id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status_code").value(200))
            .andExpect(jsonPath("$.data.id").value(id.toString()))
            .andExpect(jsonPath("$.data.codigo").value("LUM-2026-000001"))
            .andExpect(jsonPath("$.data.status").value("ABERTO"))
            .andExpect(jsonPath("$.data.canal_origem").value("LIGACAO"))
            .andExpect(jsonPath("$.data.unidade.nome").value("U.03 Boa Viagem"))
            .andExpect(jsonPath("$.data.responsavel").value(nullValue()))
            .andExpect(jsonPath("$.data.registros_contato[0].id").value(contatoId.toString()))
            .andExpect(jsonPath("$.data.historico[0].id_evento").value(eventoId.toString()))
            .andExpect(jsonPath("$.data.historico[0].tipo").value("CHAMADO_ABERTO"));
    }

    @Test
    @WithMockUser
    void deveResponder404QuandoChamadoNaoExistirAoDetalhar() throws Exception {
        UUID id = UUID.randomUUID();
        when(chamadoService.detalhar(id))
            .thenThrow(new RecursoNaoEncontradoException("Chamado não encontrado."));

        mockMvc.perform(get("/api/v1/chamados/{id}", id))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status_code").value(404))
            .andExpect(jsonPath("$.message").value("Chamado não encontrado."));
    }

    @Test
    @WithMockUser
    void deveRecusarChamadoSemUnidadeComAMensagemDoContrato() throws Exception {
        String semUnidade = CORPO_VALIDO.replaceFirst("\"unidade_id\": \"[^\"]+\",", "");

        mockMvc.perform(post("/api/v1/chamados").contentType(MediaType.APPLICATION_JSON).content(semUnidade))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Erro de validação"))
            .andExpect(jsonPath("$.errors[0]").value("Campo 'unidade_id' é obrigatório."));

        verifyNoInteractions(chamadoService);
    }

    @Test
    @WithMockUser
    void deveRecusarCategoriaAusente() throws Exception {
        String semCategoria = CORPO_VALIDO.replaceFirst("\"categoria_id\": \"[^\"]+\",", "");

        mockMvc.perform(post("/api/v1/chamados").contentType(MediaType.APPLICATION_JSON).content(semCategoria))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors[0]").value("Campo 'categoria_id' é obrigatório."));

        verifyNoInteractions(chamadoService);
    }

    @Test
    @WithMockUser
    void deveRecusarDescricaoCurta() throws Exception {
        String descricaoCurta = CORPO_VALIDO.replace("Impressora de etiquetas da coleta não imprime.", "Quebrou");

        mockMvc.perform(post("/api/v1/chamados").contentType(MediaType.APPLICATION_JSON).content(descricaoCurta))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors[0]").value("Campo 'descricao' deve ter entre 10 e 2000 caracteres."));

        verifyNoInteractions(chamadoService);
    }

    @Test
    @WithMockUser
    void deveRecusarCanalForaDaLista() throws Exception {
        String canalInvalido = CORPO_VALIDO.replace("\"LIGACAO\"", "\"OUTRO\"");

        mockMvc.perform(post("/api/v1/chamados").contentType(MediaType.APPLICATION_JSON).content(canalInvalido))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Requisição inválida"));

        verifyNoInteractions(chamadoService);
    }

    @Test
    void deveExigirLogin() throws Exception {
        mockMvc.perform(post("/api/v1/chamados").contentType(MediaType.APPLICATION_JSON).content(CORPO_VALIDO))
            .andExpect(status().isUnauthorized());

        verifyNoInteractions(chamadoService);
    }

    // ---------- atribuir responsável (HU12) ----------

    @Test
    @WithMockUser
    void deveAtribuirResponsavelComSucesso() throws Exception {
        UUID chamadoId = UUID.randomUUID();
        UUID responsavelId = UUID.randomUUID();
        ChamadoDetalheResponse resposta = new ChamadoDetalheResponse(
            chamadoId, "LUM-2026-000001", StatusChamado.ABERTO, null,
            "Impressora de etiquetas da coleta não imprime.", CanalOrigem.LIGACAO,
            new UnidadeResumoResponse(UUID.randomUUID(), "U.03 Boa Viagem"),
            new CategoriaResponse(UUID.randomUUID(), "Equipamento de apoio"),
            null, new SolicitanteResponse("Recepção Boa Viagem", "+5581999999999"),
            new ResponsavelResponse(responsavelId, "Analista Dev Um"), null,
            Instant.parse("2026-10-22T14:30:00Z"), Instant.parse("2026-10-22T14:35:00Z"),
            OrigemDado.TESTE, List.of(), List.of()
        );

        when(chamadoService.atribuirResponsavel(chamadoId, responsavelId)).thenReturn(resposta);

        mockMvc.perform(patch("/api/v1/chamados/" + chamadoId + "/responsavel")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"responsavel_id": "%s"}
                    """.formatted(responsavelId)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status_code").value(200))
            .andExpect(jsonPath("$.message").value("Responsável atribuído com sucesso"))
            .andExpect(jsonPath("$.data.id").value(chamadoId.toString()))
            .andExpect(jsonPath("$.data.responsavel.id").value(responsavelId.toString()))
            .andExpect(jsonPath("$.data.responsavel.nome").value("Analista Dev Um"));

        verify(chamadoService).atribuirResponsavel(chamadoId, responsavelId);
    }

    @Test
    @WithMockUser
    void deveRecusarAtribuirSemResponsavelIdCom400() throws Exception {
        UUID chamadoId = UUID.randomUUID();

        mockMvc.perform(patch("/api/v1/chamados/" + chamadoId + "/responsavel")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status_code").value(400));

        verifyNoInteractions(chamadoService);
    }

    // ---------- fila de chamados (HU14) ----------

    @Test
    @WithMockUser
    void deveListarAFilaNoEnvelopePadraoEmSnakeCase() throws Exception {
        UUID id = UUID.randomUUID();
        Instant abertoEm = Instant.parse("2026-10-22T14:30:00Z");
        ChamadoResumoResponse linha = new ChamadoResumoResponse(
            id, "LUM-2026-000162", "Impressora de etiquetas parada",
            new UnidadeResumoResponse(UUID.randomUUID(), "U.03 Boa Viagem"),
            new CategoriaResponse(UUID.randomUUID(), "Equipamento de apoio"),
            CanalOrigem.LIGACAO, StatusChamado.ABERTO, null, null,
            abertoEm, abertoEm);
        FilaChamadosResponse fila = new FilaChamadosResponse(
            List.of(linha), 0, 20, 1, 1, new EnumMap<>(Map.of(StatusChamado.ABERTO, 1L)));

        when(filaChamadosService.listar(any(FiltroChamados.class), eq(false), eq(0), eq(20)))
            .thenReturn(fila);

        mockMvc.perform(get("/api/v1/chamados"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status_code").value(200))
            .andExpect(jsonPath("$.data.pagina").value(0))
            .andExpect(jsonPath("$.data.tamanho").value(20))
            .andExpect(jsonPath("$.data.total_itens").value(1))
            .andExpect(jsonPath("$.data.total_paginas").value(1))
            .andExpect(jsonPath("$.data.totais_por_status.ABERTO").value(1))
            .andExpect(jsonPath("$.data.itens[0].id").value(id.toString()))
            .andExpect(jsonPath("$.data.itens[0].codigo").value("LUM-2026-000162"))
            .andExpect(jsonPath("$.data.itens[0].unidade.nome").value("U.03 Boa Viagem"))
            .andExpect(jsonPath("$.data.itens[0].categoria.nome").value("Equipamento de apoio"))
            .andExpect(jsonPath("$.data.itens[0].responsavel").value(nullValue()))
            .andExpect(jsonPath("$.data.itens[0].aberto_em").value("2026-10-22T14:30:00Z"));
    }

    @Test
    @WithMockUser
    void devePassarFiltrosParaOServico() throws Exception {
        UUID unidadeId = UUID.randomUUID();
        UUID responsavelId = UUID.randomUUID();
        when(filaChamadosService.listar(any(), eq(true), eq(1), eq(50))).thenReturn(paginaVazia());

        mockMvc.perform(get("/api/v1/chamados")
                .param("page", "1")
                .param("size", "50")
                .param("status", "ABERTO", "EM_TRIAGEM")
                .param("unidade_id", unidadeId.toString())
                .param("prioridade", "ALTA")
                .param("canal_origem", "WHATSAPP")
                .param("responsavel_id", responsavelId.toString())
                .param("sem_responsavel", "true")
                .param("sem_prioridade", "true")
                .param("meus", "true")
                .param("codigo", "lum-2026-000162"))
            .andExpect(status().isOk());

        ArgumentCaptor<FiltroChamados> filtro = ArgumentCaptor.forClass(FiltroChamados.class);
        verify(filaChamadosService).listar(filtro.capture(), eq(true), eq(1), eq(50));
        assertEquals(new FiltroChamados(List.of(StatusChamado.ABERTO, StatusChamado.EM_TRIAGEM), unidadeId,
            Prioridade.ALTA, CanalOrigem.WHATSAPP, responsavelId, true, true, "lum-2026-000162"), filtro.getValue());
    }

    @Test
    @WithMockUser
    void deveUsarPagina0Tamanho20ESemFiltrosPorPadrao() throws Exception {
        when(filaChamadosService.listar(any(), eq(false), eq(0), eq(20))).thenReturn(paginaVazia());

        mockMvc.perform(get("/api/v1/chamados")).andExpect(status().isOk());

        verify(filaChamadosService).listar(
            new FiltroChamados(null, null, null, null, null, false, false, null), false, 0, 20);
    }

    @Test
    @WithMockUser
    void deveRecusarStatusInexistenteCom400() throws Exception {
        mockMvc.perform(get("/api/v1/chamados").param("status", "XPTO"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status_code").value(400))
            .andExpect(jsonPath("$.message").value("Requisição inválida"));

        verifyNoInteractions(filaChamadosService);
    }

    @Test
    @WithMockUser
    void deveRecusarUnidadeQueNaoEUuidCom400() throws Exception {
        mockMvc.perform(get("/api/v1/chamados").param("unidade_id", "boa-viagem"))
            .andExpect(status().isBadRequest());

        verifyNoInteractions(filaChamadosService);
    }

    @Test
    @WithMockUser
    void deveDevolver400DoServicoQuandoOTamanhoEstaForaDoLimite() throws Exception {
        when(filaChamadosService.listar(any(), eq(false), eq(0), eq(500)))
            .thenThrow(new RegraNegocioException("O parâmetro 'size' deve estar entre 1 e 100."));

        mockMvc.perform(get("/api/v1/chamados").param("size", "500"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors[0]").value("O parâmetro 'size' deve estar entre 1 e 100."));
    }

    @Test
    void deveExigirLoginParaListar() throws Exception {
        mockMvc.perform(get("/api/v1/chamados")).andExpect(status().isUnauthorized());

        verifyNoInteractions(filaChamadosService);
    }

    private static FilaChamadosResponse paginaVazia() {
        return new FilaChamadosResponse(List.of(), 0, 20, 0, 0, new EnumMap<>(StatusChamado.class));
    }
}
