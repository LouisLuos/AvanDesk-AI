package com.avandesk.api.chamado.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
}
