package com.avandesk.api.chamado.entity;

/**
 * Subtipos de Espera para o status AGUARDANDO (Seção 5 - SLA e Critérios de Resultado).
 *
 * Regras do Relógio de SLA:
 * - SOLICITANTE: PAUSA o relógio de SLA (aguardando retorno/ação do usuário).
 * - FORNECEDOR: NÃO PAUSA o relógio de SLA (RN07: time interno segue responsável perante o SLA global).
 *               O período é registrado como Tempo de Espera de Terceiros.
 * - PECA_EQUIPAMENTO: NÃO PAUSA o relógio de SLA (dependência de hardware/logística).
 */
public enum MotivoEspera {
    FORNECEDOR(false),
    SOLICITANTE(true),
    PECA_EQUIPAMENTO(false);

    private final boolean pausaRelogioSla;

    MotivoEspera(boolean pausaRelogioSla) {
        this.pausaRelogioSla = pausaRelogioSla;
    }

    public boolean isPausaRelogioSla() {
        return pausaRelogioSla;
    }
}
