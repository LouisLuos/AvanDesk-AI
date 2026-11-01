package com.avandesk.api.sla.domain;

import com.avandesk.api.chamado.entity.Prioridade;
import org.springframework.stereotype.Service;

/**
 * Service de Aplicação/Infra que expõe o Domain Service Puro para o container do Spring.
 */
@Service
public class SlaDomainService {

    private final CalculadoraPrioridadeMatriz3D calculadoraMatriz3D = new CalculadoraPrioridadeMatriz3D();

    public Prioridade calcularPrioridadeSugerida(int severidade, int urgencia, int impacto) {
        return calculadoraMatriz3D.calcular(severidade, urgencia, impacto);
    }

    public AvaliacaoMatriz3D avaliarMatriz3D(int severidade, int urgencia, int impacto) {
        return AvaliacaoMatriz3D.de(severidade, urgencia, impacto, calculadoraMatriz3D);
    }
}

