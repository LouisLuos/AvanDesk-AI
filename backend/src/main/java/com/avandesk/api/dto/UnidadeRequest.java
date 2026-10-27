package com.avandesk.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UnidadeRequest(

    @NotBlank(message = "é obrigatório")
    @Size(max = 120, message = "deve ter no máximo 120 caracteres")
    String nome,

    @NotBlank(message = "é obrigatório")
    @Size(max = 20, message = "deve ter no máximo 20 caracteres")
    @Pattern(regexp = "^[A-Za-z0-9-]*$", message = "deve conter apenas letras, números e hífen")
    String identificacao,

    @Size(max = 255, message = "deve ter no máximo 255 caracteres")
    String localizacao,

    @JsonProperty("atendimento_24h")
    Boolean atendimento24h
) {
}
