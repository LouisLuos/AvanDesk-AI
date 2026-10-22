package com.avandesk.api.controller;

import com.avandesk.api.dto.ApiResponse;
import com.avandesk.api.dto.ChamadoRequest;
import com.avandesk.api.dto.ChamadoResponse;
import com.avandesk.api.service.ChamadoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/chamados")
@RequiredArgsConstructor
public class ChamadoController {

    private final ChamadoService chamadoService;

    @PostMapping
    public ResponseEntity<ApiResponse<ChamadoResponse>> registrar(@Valid @RequestBody ChamadoRequest request) {
        ChamadoResponse chamado = chamadoService.registrar(request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.sucesso(HttpStatus.CREATED, "Chamado registrado com sucesso", chamado));
    }
}
