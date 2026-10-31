package com.avandesk.api.chamado.dto;

import com.avandesk.api.chamado.entity.Solicitante;

/** Quem relatou o problema, no mesmo formato do {@link SolicitanteRequest}. */
public record SolicitanteResponse(String nome, String contato) {

    /** Cada solicitante tem um contato só: o e-mail ou o telefone em E.164. */
    public static SolicitanteResponse de(Solicitante solicitante) {
        String contato = solicitante.getEmail() != null ? solicitante.getEmail() : solicitante.getTelefone();
        return new SolicitanteResponse(solicitante.getNome(), contato);
    }
}
