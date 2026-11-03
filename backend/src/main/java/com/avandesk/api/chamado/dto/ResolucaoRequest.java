package com.avandesk.api.chamado.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResolucaoRequest(

    @NotBlank(message = "é obrigatório")
    @Size(min = 10, max = 2000, message = "deve ter entre 10 e 2000 caracteres")
    String resolucao,

    @JsonProperty("causa_raiz")
    @JsonAlias({"causaRaiz"})
    @Size(max = 1000, message = "deve ter no máximo 1000 caracteres")
    String causaRaiz,

    @JsonProperty("possivel_recorrencia")
    @JsonAlias({"possivelRecorrencia"})
    Boolean possivelRecorrencia
) {
}
