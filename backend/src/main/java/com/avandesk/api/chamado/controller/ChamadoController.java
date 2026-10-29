package com.avandesk.api.chamado.controller;

import com.avandesk.api.chamado.dto.ChamadoDetalheResponse;
import com.avandesk.api.chamado.dto.ChamadoRequest;
import com.avandesk.api.chamado.dto.ChamadoResponse;
import com.avandesk.api.chamado.service.ChamadoService;
import com.avandesk.api.core.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

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

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ChamadoDetalheResponse>> detalhar(@PathVariable UUID id) {
        ChamadoDetalheResponse chamado = chamadoService.detalhar(id);
        return ResponseEntity.ok(ApiResponse.sucesso(HttpStatus.OK, "Detalhes do chamado recuperados", chamado));
    }
}
