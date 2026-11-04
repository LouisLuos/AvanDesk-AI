package com.avandesk.api.usuario.dto;

/**
 * Resposta do cadastro (HU10). A senha temporária só aparece aqui: o banco guarda apenas o hash.
 */
public record AnalistaCadastradoResponse(
    AnalistaResponse analista,
    String senhaTemporaria
) {
}
