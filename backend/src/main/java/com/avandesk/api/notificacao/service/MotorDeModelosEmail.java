package com.avandesk.api.notificacao.service;

import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Component
public class MotorDeModelosEmail {

    public static final Locale PORTUGUES = Locale.forLanguageTag("pt-BR");

    private final TemplateEngine motor = criarMotor();

    public String processar(String modelo, Map<String, Object> variaveis) {
        Context contexto = new Context(PORTUGUES);
        contexto.setVariables(variaveis);
        return motor.process(modelo, contexto);
    }

    private static TemplateEngine criarMotor() {
        TemplateEngine motor = new TemplateEngine();
        motor.addTemplateResolver(resolvedor(TemplateMode.HTML, "*.html", 1));
        motor.addTemplateResolver(resolvedor(TemplateMode.TEXT, "*.txt", 2));
        return motor;
    }

    private static ClassLoaderTemplateResolver resolvedor(TemplateMode modo, String padrao, int ordem) {
        ClassLoaderTemplateResolver resolvedor = new ClassLoaderTemplateResolver();
        resolvedor.setPrefix("templates/");
        resolvedor.setTemplateMode(modo);
        resolvedor.setResolvablePatterns(Set.of(padrao));
        resolvedor.setCharacterEncoding(StandardCharsets.UTF_8.name());
        resolvedor.setCheckExistence(true);
        resolvedor.setCacheable(true);
        resolvedor.setOrder(ordem);
        return resolvedor;
    }
}
