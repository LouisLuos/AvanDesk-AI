package com.avandesk.api.chamado.dto;

import com.avandesk.api.chamado.entity.CanalOrigem;
import com.avandesk.api.chamado.entity.DirecaoContato;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record RegistroContatoRequest(

    @NotNull(message = "é obrigatório")
    CanalOrigem canal,

    DirecaoContato direcao,

    @NotBlank(message = "é obrigatório")
    @Size(max = 2000, message = "deve ter no máximo 2000 caracteres")
    String conteudo,

    @JsonProperty("ocorrido_em")
    @JsonAlias({"ocorridoEm"})
    Instant ocorridoEm,

    @Valid
    SolicitanteRequest solicitante,

    @JsonProperty("identificador_externo")
    @JsonAlias({"identificadorExterno"})
    @Size(max = 255, message = "deve ter no máximo 255 caracteres")
    String identificadorExterno
) {
}
