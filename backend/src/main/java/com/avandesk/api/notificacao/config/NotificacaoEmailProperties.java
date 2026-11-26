package com.avandesk.api.notificacao.config;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "nexo.notificacoes.email")
public record NotificacaoEmailProperties(

    boolean habilitado,

    @NotBlank
    @Email
    String remetente
) {
}
