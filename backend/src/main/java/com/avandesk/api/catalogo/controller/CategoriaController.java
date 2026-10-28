package com.avandesk.api.catalogo.controller;

import com.avandesk.api.catalogo.dto.CategoriaResponse;
import com.avandesk.api.catalogo.service.CategoriaService;
import com.avandesk.api.core.dto.ApiResponse;
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
public class CategoriaController {

    private final CategoriaService categoriaService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<CategoriaResponse>>> listarAtivas() {
        List<CategoriaResponse> categorias = categoriaService.listarAtivas();
        return ResponseEntity.ok(ApiResponse.sucesso(HttpStatus.OK, "Categorias recuperadas com sucesso", categorias));
    }
}
