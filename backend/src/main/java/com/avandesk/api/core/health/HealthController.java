package com.avandesk.api.core.health;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@Tag(name = "Saúde", description = "Verificação de que a API está no ar")
public class HealthController {

    @GetMapping("/health")
    @Operation(summary = "Verifica se a API está no ar", description = "Rota pública. Responde 200 com status UP.")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "timestamp", LocalDateTime.now().toString(),
            "service", "avandesk-api"
        ));
    }
}
