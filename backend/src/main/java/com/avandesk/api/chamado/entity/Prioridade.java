package com.avandesk.api.chamado.entity;

/**
 * Níveis de Prioridade do Chamado e respectiva severidade para herança em mesclagem (RN17).
 */
public enum Prioridade {
    BAIXA(1),
    MEDIA(2),
    ALTA(3);

    private final int peso;

    Prioridade(int peso) {
        this.peso = peso;
    }

    public int getPeso() {
        return peso;
    }

    /**
     * Regra RN17 (Seção 5): Em caso de mesclagem ou vínculo, determina a maior prioridade.
     */
    public static Prioridade maiorPrioridade(Prioridade p1, Prioridade p2) {
        if (p1 == null) return p2;
        if (p2 == null) return p1;
        return p1.peso >= p2.peso ? p1 : p2;
    }
}
