package com.avandesk.api.usuario.service;

import java.util.ArrayList;
import java.util.List;

public final class PoliticaSenhaForte {

    public static final int TAMANHO_MINIMO = 10;
    public static final int TAMANHO_MAXIMO = 72;

    private PoliticaSenhaForte() {
    }

    public static List<String> pendencias(String senha) {
        String texto = senha == null ? "" : senha;
        List<String> pendencias = new ArrayList<>();
        if (texto.length() < TAMANHO_MINIMO) {
            pendencias.add("ter pelo menos %d caracteres".formatted(TAMANHO_MINIMO));
        }
        if (texto.length() > TAMANHO_MAXIMO) {
            pendencias.add("ter no máximo %d caracteres".formatted(TAMANHO_MAXIMO));
        }
        if (texto.chars().noneMatch(Character::isUpperCase)) {
            pendencias.add("ter uma letra maiúscula");
        }
        if (texto.chars().noneMatch(Character::isLowerCase)) {
            pendencias.add("ter uma letra minúscula");
        }
        if (texto.chars().noneMatch(Character::isDigit)) {
            pendencias.add("ter um número");
        }
        if (texto.chars().allMatch(Character::isLetterOrDigit)) {
            pendencias.add("ter um símbolo (ex.: ! @ # $ %)");
        }
        if (!texto.isEmpty() && texto.isBlank()) {
            pendencias.add("não ser só espaços");
        }
        return pendencias;
    }
}
