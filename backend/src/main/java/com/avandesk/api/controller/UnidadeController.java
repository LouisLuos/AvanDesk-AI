package com.avandesk.api.controller;

import com.avandesk.api.dto.ApiResponse;
import com.avandesk.api.dto.UnidadeRequest;
import com.avandesk.api.dto.UnidadeResponse;
import com.avandesk.api.service.UnidadeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/unidades")
@RequiredArgsConstructor
public class UnidadeController {

    private final UnidadeService unidadeService;

    @PostMapping
    public ResponseEntity<ApiResponse<UnidadeResponse>> cadastrar(@Valid @RequestBody UnidadeRequest request) {
        UnidadeResponse unidade = unidadeService.cadastrar(request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.sucesso(HttpStatus.CREATED, "Unidade cadastrada com sucesso", unidade));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<UnidadeResponse>>> listar(@RequestParam(required = false) Boolean ativo) {
        List<UnidadeResponse> unidades = unidadeService.listar(ativo);
        return ResponseEntity.ok(ApiResponse.sucesso(HttpStatus.OK, "Unidades recuperadas com sucesso", unidades));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UnidadeResponse>> buscarPorId(@PathVariable UUID id) {
        UnidadeResponse unidade = unidadeService.buscarPorId(id);
        return ResponseEntity.ok(ApiResponse.sucesso(HttpStatus.OK, "Unidade recuperada com sucesso", unidade));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UnidadeResponse>> editar(@PathVariable UUID id,
                                                               @Valid @RequestBody UnidadeRequest request) {
        UnidadeResponse unidade = unidadeService.editar(id, request);
        return ResponseEntity.ok(ApiResponse.sucesso(HttpStatus.OK, "Unidade atualizada com sucesso", unidade));
    }

    @PatchMapping("/{id}/desativar")
    public ResponseEntity<ApiResponse<UnidadeResponse>> desativar(@PathVariable UUID id) {
        UnidadeResponse unidade = unidadeService.desativar(id);
        return ResponseEntity.ok(ApiResponse.sucesso(HttpStatus.OK, "Unidade desativada com sucesso", unidade));
    }
}
