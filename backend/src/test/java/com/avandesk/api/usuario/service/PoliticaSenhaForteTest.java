package com.avandesk.api.usuario.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class PoliticaSenhaForteTest {

    @Test
    void deveAceitarSenhaComTodosOsRequisitos() {
        assertThat(PoliticaSenhaForte.pendencias("Coleta#2026ok")).isEmpty();
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
        "Cur#2026      | ter pelo menos 10 caracteres",
        "coleta#2026ok | ter uma letra maiúscula",
        "COLETA#2026OK | ter uma letra minúscula",
        "Coleta#Lumen! | ter um número",
        "Coleta2026ok  | ter um símbolo (ex.: ! @ # $ %)"
    })
    void deveApontarCadaRequisitoQueFalta(String senha, String pendencia) {
        assertThat(PoliticaSenhaForte.pendencias(senha.trim())).containsExactly(pendencia);
    }

    @Test
    void deveRecusarSenhaVaziaComTodasAsPendencias() {
        assertThat(PoliticaSenhaForte.pendencias(null)).hasSize(5);
    }

    @Test
    void deveRecusarSenhaAcimaDoLimiteDoBcrypt() {
        assertThat(PoliticaSenhaForte.pendencias("Aa1#" + "x".repeat(70))).containsExactly("ter no máximo 72 caracteres");
    }
}
