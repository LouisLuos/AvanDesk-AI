package com.avandesk.api.usuario.service;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class GeradorSenhaTemporariaTest {

    private final GeradorSenhaTemporaria gerador = new GeradorSenhaTemporaria();

    @Test
    void deveGerarTresGruposDeQuatroCaracteresSemOsAmbiguos() {
        for (int i = 0; i < 200; i++) {
            assertThat(gerador.gerar()).matches("[A-HJ-NP-Za-km-z2-9]{4}-[A-HJ-NP-Za-km-z2-9]{4}-[A-HJ-NP-Za-km-z2-9]{4}");
        }
    }

    @Test
    void deveGerarSenhasDiferentesACadaChamada() {
        Set<String> senhas = new HashSet<>();
        for (int i = 0; i < 200; i++) {
            senhas.add(gerador.gerar());
        }

        assertThat(senhas).hasSize(200);
    }
}
