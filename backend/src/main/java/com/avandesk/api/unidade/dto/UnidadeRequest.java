package com.avandesk.api.unidade.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UnidadeRequest(

    @NotBlank(message = "O nome é obrigatório.")
    @Size(max = 120, message = "O nome não pode exceder 120 caracteres.")
    String nome,

    @NotBlank(message = "A identificação é obrigatória.")
    @Size(max = 20, message = "A identificação não pode exceder 20 caracteres.")
    @Pattern(regexp = "^[A-Za-z0-9-]*$", message = "A identificação deve conter apenas letras, números e traço (-).")
    String identificacao,

    @Size(max = 255, message = "A localização não pode exceder 255 caracteres.")
    String localizacao,

    Boolean atendimento24h
) {
}
