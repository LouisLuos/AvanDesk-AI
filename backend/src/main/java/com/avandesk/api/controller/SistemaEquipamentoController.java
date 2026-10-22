package com.avandesk.api.controller;

import com.avandesk.api.dto.ApiResponse;
import com.avandesk.api.dto.SistemaEquipamentoResponse;
import com.avandesk.api.service.SistemaEquipamentoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/sistemas-equipamentos")
@RequiredArgsConstructor
public class SistemaEquipamentoController {

    private final SistemaEquipamentoService sistemaEquipamentoService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<SistemaEquipamentoResponse>>> listarAtivos() {
        List<SistemaEquipamentoResponse> sistemas = sistemaEquipamentoService.listarAtivos();
        return ResponseEntity.ok(
            ApiResponse.sucesso(HttpStatus.OK, "Sistemas e equipamentos recuperados com sucesso", sistemas));
    }
}
