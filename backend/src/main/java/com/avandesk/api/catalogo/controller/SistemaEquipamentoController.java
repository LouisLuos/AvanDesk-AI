package com.avandesk.api.catalogo.controller;

import com.avandesk.api.catalogo.dto.SistemaEquipamentoResponse;
import com.avandesk.api.catalogo.service.SistemaEquipamentoService;
import com.avandesk.api.core.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Catálogo", description = "Listas do formulário de registro de chamado (HU01)")
public class SistemaEquipamentoController {

    private final SistemaEquipamentoService sistemaEquipamentoService;

    @GetMapping
    @Operation(summary = "Lista os sistemas e equipamentos ativos (HU01)", description = "Em ordem alfabética. Responde 200.")
    public ResponseEntity<ApiResponse<List<SistemaEquipamentoResponse>>> listarAtivos() {
        List<SistemaEquipamentoResponse> sistemas = sistemaEquipamentoService.listarAtivos();
        return ResponseEntity.ok(ApiResponse.sucesso(HttpStatus.OK, "Sistemas/equipamentos recuperados com sucesso", sistemas));
    }
}
