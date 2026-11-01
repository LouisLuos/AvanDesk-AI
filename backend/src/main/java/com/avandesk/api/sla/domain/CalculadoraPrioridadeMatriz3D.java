package com.avandesk.api.sla.domain;

import com.avandesk.api.chamado.entity.Prioridade;

/**
 * Calculadora da Matriz 3D de Prioridade (Domain Service Puro).
 * Alinhado com a Seção 3 do Documento Oficial de SLA (Sarah Cyrne / HU04).
 *
 * Fórmula:
 *   Soma = Severidade + Urgência + Impacto
 *   Onde cada dimensão varia entre 1 e 3:
 *     - 1: Baixo
 *     - 2: Médio
 *     - 3: Alto
 *
 * Faixas de Prioridade Sugerida:
 *   - Soma 3 a 5: BAIXA (P3)
 *   - Soma 6 a 7: MEDIA (P2)
 *   - Soma 8 a 9: ALTA (P1)
 *
 * Regra de Ouro (Clean Architecture): Zero dependência de Spring, JPA ou HTTP.
 */
public class CalculadoraPrioridadeMatriz3D {

    public static final int VALOR_MINIMO = 1;
    public static final int VALOR_MAXIMO = 3;

    public Prioridade calcular(int severidade, int urgencia, int impacto) {
        validarDimensao("Severidade", severidade);
        validarDimensao("Urgência", urgencia);
        validarDimensao("Impacto", impacto);

        int soma = severidade + urgencia + impacto;

        if (soma >= 3 && soma <= 5) {
            return Prioridade.BAIXA;
        } else if (soma >= 6 && soma <= 7) {
            return Prioridade.MEDIA;
        } else if (soma >= 8 && soma <= 9) {
            return Prioridade.ALTA;
        }

        throw new IllegalStateException("Soma calculada fora dos limites permitidos: " + soma);
    }

    private void validarDimensao(String nomeDimensao, int valor) {
        if (valor < VALOR_MINIMO || valor > VALOR_MAXIMO) {
            throw new IllegalArgumentException(
                nomeDimensao + " deve estar entre " + VALOR_MINIMO + " e " + VALOR_MAXIMO + " (recebido: " + valor + ")"
            );
        }
    }
}

