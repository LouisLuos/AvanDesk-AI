package com.avandesk.api.usuario.config;

import jakarta.validation.constraints.NotEmpty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.List;

/**
 * Propriedades do bloco "nexo.usuarios" do application.yml. Se faltar alguma, a API não sobe.
 *
 * @param dominiosEmail domínios aceitos no e-mail corporativo dos analistas (HU10), sem o "@"
 */
@Validated
@ConfigurationProperties(prefix = "nexo.usuarios")
public record UsuarioProperties(

    @NotEmpty
    List<String> dominiosEmail
) {
}
