package com.avandesk.api.usuario.service;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/**
 * Gera a senha temporária de um analista recém-cadastrado (HU10), no formato "Kp7m-Qx4t-Rz9w":
 * 12 caracteres aleatórios, sem os ambíguos (0/O, 1/l/I), para ditar ou copiar sem erro.
 */
@Component
public class GeradorSenhaTemporaria {

    private static final String ALFABETO = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";
    private static final int GRUPOS = 3;
    private static final int TAMANHO_GRUPO = 4;

    private final SecureRandom aleatorio = new SecureRandom();

    public String gerar() {
        StringBuilder senha = new StringBuilder();
        for (int grupo = 0; grupo < GRUPOS; grupo++) {
            if (grupo > 0) {
                senha.append('-');
            }
            for (int i = 0; i < TAMANHO_GRUPO; i++) {
                senha.append(ALFABETO.charAt(aleatorio.nextInt(ALFABETO.length())));
            }
        }
        return senha.toString();
    }
}
