package com.avandesk.api.catalogo.controller;

import com.avandesk.api.catalogo.dto.CategoriaResponse;
import com.avandesk.api.catalogo.service.CategoriaService;
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
@RequestMapping("/api/v1/categorias")
@RequiredArgsConstructor
@Tag(name = "Catálogo", description = "Listas do formulário de registro de chamado (HU01)")
public class CategoriaController {

    private final CategoriaService categoriaService;

    @GetMapping
    @Operation(summary = "Lista as categorias ativas (HU01)", description = "Em ordem alfabética. Responde 200.")
    public ResponseEntity<ApiResponse<List<CategoriaResponse>>> listarAtivas() {
        List<CategoriaResponse> categorias = categoriaService.listarAtivas();
        return ResponseEntity.ok(ApiResponse.sucesso(HttpStatus.OK, "Categorias recuperadas com sucesso", categorias));
    }
}
