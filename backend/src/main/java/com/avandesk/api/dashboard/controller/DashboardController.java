package com.avandesk.api.dashboard.controller;

import com.avandesk.api.core.dto.ApiResponse;
import com.avandesk.api.dashboard.dto.DashboardMetricasResponse;
import com.avandesk.api.dashboard.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "Painel da Administração e indicadores executivos de SLA (HU08)")
public class DashboardController {

    private static final String SO_LIDERANCA = "hasRole('LIDERANCA_TI')";

    private final DashboardService dashboardService;

    @GetMapping("/metricas")
    @PreAuthorize(SO_LIDERANCA)
    @Operation(
        summary = "Consulta KPIs, SLA, problemas recorrentes, tempo em terceiros e alertas da Seção 9.7 (HU08)",
        description = "Restrito ao perfil LIDERANCA_TI. Sem inicio/fim informados, considera os últimos 30 dias."
    )
    public ResponseEntity<ApiResponse<DashboardMetricasResponse>> obterMetricas(
        @RequestParam(required = false) Instant inicio,
        @RequestParam(required = false) Instant fim,
        @RequestParam(name = "unidade_id", required = false) UUID unidadeId
    ) {
        DashboardMetricasResponse metricas = dashboardService.consultarMetricas(inicio, fim, unidadeId);
        return ResponseEntity.ok(
            ApiResponse.sucesso(HttpStatus.OK, "Métricas do dashboard recuperadas com sucesso.", metricas)
        );
    }
}
