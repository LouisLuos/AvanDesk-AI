package com.avandesk.api.chamado.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record EscalonamentoRequest(

    @NotBlank(message = "é obrigatório")
    @Size(max = 120, message = "deve ter no máximo 120 caracteres")
    String fornecedor,

    @JsonProperty("protocolo_externo")
    @JsonAlias({"protocoloExterno"})
    @Size(max = 100, message = "deve ter no máximo 100 caracteres")
    String protocoloExterno,

    @Size(max = 1000, message = "deve ter no máximo 1000 caracteres")
    String observacao,

    @JsonProperty("enviado_em")
    @JsonAlias({"enviadoEm"})
    Instant enviadoEm
) {
}
