package com.avandesk.api.temporeal.service;

import com.avandesk.api.catalogo.repository.CategoriaRepository;
import com.avandesk.api.chamado.dto.ChamadoRequest;
import com.avandesk.api.chamado.dto.ResponsavelResponse;
import com.avandesk.api.chamado.dto.SolicitanteRequest;
import com.avandesk.api.chamado.entity.CanalOrigem;
import com.avandesk.api.chamado.service.ChamadoService;
import com.avandesk.api.temporeal.dto.EventoTempoRealMessage;
import com.avandesk.api.temporeal.dto.TipoEventoTempoReal;
import com.avandesk.api.temporeal.port.EventoTempoRealPort;
import com.avandesk.api.unidade.entity.Unidade;
import com.avandesk.api.unidade.repository.UnidadeRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@SpringBootTest
@WithMockUser(username = "analista1@lumen.local")
class TempoRealIntegracaoTest {

    @Autowired
    private ChamadoService chamadoService;

    @Autowired
    private UnidadeRepository unidadeRepository;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private SimpMessagingTemplate mensageiro;

    @MockitoBean
    private EventoTempoRealPort eventoTempoRealPort;

    private Unidade unidade;

    @BeforeEach
    void criarUnidade() {
        unidade = new Unidade();
        unidade.setNome("Unidade de Teste HU25");
        unidade.setIdentificacao("IT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        unidade = unidadeRepository.save(unidade);
    }

    @AfterEach
    void removerUnidade() {
        unidadeRepository.deleteById(unidade.getId());
    }

    @Test
    void naoDevePublicarNadaQuandoORegistroEDesfeito() {
        UUID categoriaId = categoriaRepository.findByAtivoTrueOrderByNomeAsc().getFirst().getId();

        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            chamadoService.registrar(new ChamadoRequest(unidade.getId(), CanalOrigem.LIGACAO,
                new SolicitanteRequest("Teste HU25", "hu25.it@lumen.local"),
                "Impressora de etiquetas da coleta não imprime.", categoriaId, null, null, true));
            status.setRollbackOnly();
        });

        verify(eventoTempoRealPort, never()).publicar(any());
    }

    @Test
    void deveSerializarAMensagemEmSnakeCaseComoARestDaApi() {
        EventoTempoRealMessage mensagem = new EventoTempoRealMessage(TipoEventoTempoReal.CHAMADO_CRIADO, null,
            new ResponsavelResponse(UUID.randomUUID(), "Analista Dev Um"), Instant.parse("2026-11-26T13:00:00Z"));

        Message<?> convertida = mensageiro.getMessageConverter().toMessage(mensagem, null);

        String json = new String((byte[]) convertida.getPayload(), StandardCharsets.UTF_8);
        assertThat(json).contains("\"ocorrido_em\":\"2026-11-26T13:00:00Z\"").contains("\"tipo\":\"CHAMADO_CRIADO\"");
    }
}
