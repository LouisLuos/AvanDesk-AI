package com.avandesk.api.chamado.dto;

import com.avandesk.api.chamado.entity.CanalOrigem;
import com.avandesk.api.chamado.entity.Chamado;
import com.avandesk.api.chamado.entity.Prioridade;
import com.avandesk.api.unidade.entity.Unidade;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChamadoResumoResponseTest {

    @Test
    void deveUsarAPrimeiraLinhaDaDescricaoComoTitulo() {
        String descricao = "  Impressora de etiquetas parada  \nJá tentamos reiniciar.\nSem sucesso.";

        assertEquals("Impressora de etiquetas parada", ChamadoResumoResponse.titulo(descricao));
    }

    @Test
    void deveManterTituloCurtoSemCortar() {
        assertEquals("Rede instável na recepção", ChamadoResumoResponse.titulo("Rede instável na recepção"));
    }

    @Test
    void deveCortarTituloLongoEmOitentaCaracteresComReticencias() {
        String descricao = "A".repeat(120);

        String titulo = ChamadoResumoResponse.titulo(descricao);

        assertEquals(ChamadoResumoResponse.TAMANHO_TITULO, titulo.length());
        assertTrue(titulo.endsWith("…"));
    }

    @Test
    void naoDeveCortarTituloComExatamenteOitentaCaracteres() {
        String descricao = "B".repeat(ChamadoResumoResponse.TAMANHO_TITULO);

        assertEquals(descricao, ChamadoResumoResponse.titulo(descricao));
    }

    @Test
    void devePreencherPrioridadeComPrioridadeFinalQuandoDefinida() {
        Chamado chamado = criarChamado();
        chamado.setPrioridadeSugerida(Prioridade.MEDIA);
        chamado.setPrioridadeFinal(Prioridade.ALTA);

        ChamadoResumoResponse resumo = ChamadoResumoResponse.de(chamado, Instant.now());

        assertEquals(Prioridade.ALTA, resumo.prioridade());
    }

    @Test
    void deveFazerFallbackParaPrioridadeSugeridaQuandoPrioridadeFinalEstiverNula() {
        Chamado chamado = criarChamado();
        chamado.setPrioridadeSugerida(Prioridade.BAIXA);
        chamado.setPrioridadeFinal(null);

        ChamadoResumoResponse resumo = ChamadoResumoResponse.de(chamado, Instant.now());

        assertEquals(Prioridade.BAIXA, resumo.prioridade());
    }

    @Test
    void deveRetornarPrioridadeNulaQuandoNenhumaPrioridadeEstiverDefinida() {
        Chamado chamado = criarChamado();
        chamado.setPrioridadeSugerida(null);
        chamado.setPrioridadeFinal(null);

        ChamadoResumoResponse resumo = ChamadoResumoResponse.de(chamado, Instant.now());

        assertNull(resumo.prioridade());
    }

    private static Chamado criarChamado() {
        Unidade unidade = new Unidade();
        unidade.setId(UUID.randomUUID());
        unidade.setNome("U.03 Boa Viagem");

        Chamado chamado = new Chamado();
        chamado.setId(UUID.randomUUID());
        chamado.setCodigo("LUM-2026-000100");
        chamado.setDescricao("Falha de rede na recepção");
        chamado.setUnidade(unidade);
        chamado.setCanalOrigem(CanalOrigem.LIGACAO);
        chamado.setAbertoEm(Instant.now());
        return chamado;
    }
}
