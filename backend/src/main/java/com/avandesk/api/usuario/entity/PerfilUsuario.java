package com.avandesk.api.usuario.entity;

public enum PerfilUsuario {
    ANALISTA_SUPORTE,
    LIDERANCA_TI,
    ADMIN,
    SOLICITANTE;

    public static final String[] PERFIS_DA_EQUIPE = {ANALISTA_SUPORTE.name(), LIDERANCA_TI.name(), ADMIN.name()};
}
