package com.avandesk.api.dto;

import com.avandesk.api.entity.Unidade;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

public record UnidadeResponse(

    UUID id,
    String nome,
    String identificacao,
    String localizacao,
    @JsonProperty("atendimento_24h") boolean atendimento24h,
    boolean ativo
) {

    public static UnidadeResponse de(Unidade unidade) {
        return new UnidadeResponse(
            unidade.getId(),
            unidade.getNome(),
            unidade.getIdentificacao(),
            unidade.getLocalizacao(),
            unidade.isAtendimento24h(),
            unidade.isAtivo()
        );
    }
}
