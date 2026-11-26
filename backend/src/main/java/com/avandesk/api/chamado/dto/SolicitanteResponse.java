package com.avandesk.api.chamado.dto;

import com.avandesk.api.chamado.entity.Solicitante;

public record SolicitanteResponse(String nome, String email, String telefone, String contato) {

    public static SolicitanteResponse de(Solicitante solicitante) {
        String contato = solicitante.getEmail() != null ? solicitante.getEmail() : solicitante.getTelefone();
        return new SolicitanteResponse(solicitante.getNome(), solicitante.getEmail(), solicitante.getTelefone(), contato);
    }
}
