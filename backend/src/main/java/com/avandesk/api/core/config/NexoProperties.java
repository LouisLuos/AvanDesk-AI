package com.avandesk.api.core.config;

import com.avandesk.api.chamado.entity.OrigemDado;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Propriedades do bloco "nexo" do application.yml. Se faltar alguma, a API não sobe.
 */
@Validated
@ConfigurationProperties(prefix = "nexo")
public record NexoProperties(

    @NotNull
    OrigemDado origemDado
) {
}
