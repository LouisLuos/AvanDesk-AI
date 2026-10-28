package com.avandesk.api.chamado.controller;

import com.avandesk.api.core.config.SecurityConfig;
import com.avandesk.api.chamado.dto.ChamadoRequest;
import com.avandesk.api.chamado.dto.ChamadoResponse;
import com.avandesk.api.chamado.entity.OrigemDado;
import com.avandesk.api.chamado.entity.StatusChamado;
import com.avandesk.api.chamado.service.ChamadoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.hamcrest.Matchers.endsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
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
}
