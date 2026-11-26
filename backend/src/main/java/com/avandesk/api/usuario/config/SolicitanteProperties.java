package com.avandesk.api.usuario.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.util.List;
import java.util.Locale;

@Validated
@ConfigurationProperties(prefix = "nexo.solicitantes")
public record SolicitanteProperties(

    boolean restringirDominioEmail,

    List<String> dominiosPermitidos,

    @NotNull
    Duration validadeTokenVerificacao,

    @NotNull
    Duration intervaloReenvio,

    @NotBlank
    String urlVerificacao
) {

    public SolicitanteProperties {
        dominiosPermitidos = dominiosPermitidos == null ? List.of() : dominiosPermitidos.stream()
            .map(dominio -> dominio.trim().toLowerCase(Locale.ROOT))
            .map(dominio -> dominio.startsWith("@") ? dominio.substring(1) : dominio)
            .filter(dominio -> !dominio.isEmpty())
            .toList();
    }

    public boolean aceitaDominioDe(String email) {
        if (!restringirDominioEmail) {
            return true;
        }
        String dominio = email.substring(email.lastIndexOf('@') + 1).toLowerCase(Locale.ROOT);
        return dominiosPermitidos.contains(dominio);
    }
}
