package com.avandesk.api.service;

import com.avandesk.api.dto.ChamadoRequest;
import com.avandesk.api.dto.SolicitanteRequest;
import com.avandesk.api.entity.CanalOrigem;
import com.avandesk.api.entity.TipoEvento;
import com.avandesk.api.entity.Unidade;
import com.avandesk.api.repository.CategoriaRepository;
import com.avandesk.api.repository.UnidadeRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Prova, com o PostgreSQL de verdade, que o registro de chamado é tudo ou nada (BE-03):
 * se a gravação de um evento falhar, chamado, solicitante e registro de contato também são desfeitos.
 * O serviço de eventos é trocado por um dublê que falha no segundo evento.
 */
@SpringBootTest
@WithMockUser(username = "analista1@lumen.local")
class ChamadoRegistroRollbackIntegracaoTest {

    @Autowired
    private ChamadoService chamadoService;

    @MockitoBean
    private EventoHistoricoService eventoHistoricoService;

    @Autowired
    private UnidadeRepository unidadeRepository;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Unidade unidade;

    @BeforeEach
    void criarUnidade() {
        unidade = new Unidade();
        unidade.setNome("Unidade de Teste de Integração");
        unidade.setIdentificacao("IT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        unidade = unidadeRepository.save(unidade);
    }

    @AfterEach
    void removerUnidade() {
        unidadeRepository.deleteById(unidade.getId());
    }

    @Test
    void deveDesfazerTudoQuandoAGravacaoDeUmEventoFalha() {
        when(eventoHistoricoService.registrar(any(), eq(TipoEvento.REGISTRO_DE_CONTATO_RECEBIDO), any(), any()))
            .thenThrow(new IllegalStateException("falha simulada ao gravar o evento"));
        String contato = "rollback-" + UUID.randomUUID() + "@lumen.local";
        UUID categoriaId = categoriaRepository.findByAtivoTrueOrderByNomeAsc().getFirst().getId();
        long chamadosAntes = contar("chamados");
        long contatosAntes = contar("registros_contato");

        assertThrows(IllegalStateException.class, () -> chamadoService.registrar(new ChamadoRequest(
            unidade.getId(), CanalOrigem.LIGACAO, new SolicitanteRequest("Teste de Rollback", contato),
            "Impressora de etiquetas da coleta não imprime.", categoriaId, null, null)));

        assertEquals(chamadosAntes, contar("chamados"));
        assertEquals(contatosAntes, contar("registros_contato"));
        assertEquals(0L, jdbcTemplate.queryForObject(
            "SELECT count(*) FROM solicitantes WHERE email = ?", Long.class, contato));
    }

    private long contar(String tabela) {
        return jdbcTemplate.queryForObject("SELECT count(*) FROM " + tabela, Long.class);
    }
}
