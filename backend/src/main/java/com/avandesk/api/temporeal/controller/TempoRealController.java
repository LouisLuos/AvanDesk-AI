package com.avandesk.api.temporeal.controller;

import com.avandesk.api.core.dto.ApiResponse;
import com.avandesk.api.temporeal.dto.TicketTempoRealResponse;
import com.avandesk.api.temporeal.service.TicketTempoRealService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tempo-real")
@RequiredArgsConstructor
@Tag(name = "Tempo real", description = "Conexão WebSocket/STOMP da fila e do detalhe (HU25)")
public class TempoRealController {

    private final TicketTempoRealService ticketTempoRealService;

    @GetMapping("/ticket")
    @Operation(summary = "Emite o ticket para conectar no WebSocket de tempo real (HU25)",
        description = "JWT curto (60 s) do usuário autenticado, que só vale no CONNECT do STOMP em /api/v1/ws "
            + "(header Authorization: Bearer <ticket>) e é recusado nas rotas REST. Peça um novo a cada conexão. "
            + "Responde 200; 401 sem login.")
    public ResponseEntity<ApiResponse<TicketTempoRealResponse>> ticket() {
        return ResponseEntity.ok(ApiResponse.sucesso(HttpStatus.OK, "Ticket de tempo real emitido",
            ticketTempoRealService.emitir()));
    }
}
