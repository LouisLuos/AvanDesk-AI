package com.avandesk.api.sla.domain;

import com.avandesk.api.chamado.entity.Prioridade;

/**
 * Value Object imutável que representa uma avaliação na Matriz 3D.
 */
public record AvaliacaoMatriz3D(
    int severidade,
    int urgencia,
    int impacto,
    Prioridade prioridadeSugerida
) {
    public static AvaliacaoMatriz3D de(int severidade, int urgencia, int impacto, CalculadoraPrioridadeMatriz3D calculadora) {
        Prioridade sugerida = calculadora.calcular(severidade, urgencia, impacto);
        return new AvaliacaoMatriz3D(severidade, urgencia, impacto, sugerida);
    }
}

