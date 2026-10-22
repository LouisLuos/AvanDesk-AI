package com.avandesk.api.controller;

import com.avandesk.api.dto.ApiResponse;
import com.avandesk.api.dto.CategoriaResponse;
import com.avandesk.api.service.CategoriaService;
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
