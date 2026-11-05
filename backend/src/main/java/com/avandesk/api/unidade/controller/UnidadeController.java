package com.avandesk.api.unidade.controller;

import com.avandesk.api.core.dto.ApiResponse;
import com.avandesk.api.unidade.dto.UnidadeRequest;
import com.avandesk.api.unidade.dto.UnidadeResponse;
import com.avandesk.api.unidade.service.UnidadeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Unidades", description = "Cadastro das unidades da Rede Lúmen (HU11)")
public class UnidadeController {

    private final UnidadeService unidadeService;

    @PostMapping
    @Operation(summary = "Cadastra uma unidade (HU11)",
        description = "Nome e identificação são obrigatórios; a identificação aceita letras, números e hífen "
            + "(até 20 caracteres) e não pode repetir. Responde 201; 400 para dados inválidos ou identificação repetida.")
    public ResponseEntity<ApiResponse<UnidadeResponse>> cadastrar(@Valid @RequestBody UnidadeRequest request) {
        UnidadeResponse unidade = unidadeService.cadastrar(request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.sucesso(HttpStatus.CREATED, "Unidade cadastrada com sucesso", unidade));
    }

    @GetMapping
    @Operation(summary = "Lista as unidades (HU11)",
        description = "Em ordem alfabética de nome. O parâmetro ativo filtra só as ativas (true) ou só as inativas "
            + "(false); sem ele, vêm todas. Responde 200.")
    public ResponseEntity<ApiResponse<List<UnidadeResponse>>> listar(@RequestParam(required = false) Boolean ativo) {
        List<UnidadeResponse> unidades = unidadeService.listar(ativo);
        return ResponseEntity.ok(ApiResponse.sucesso(HttpStatus.OK, "Unidades recuperadas com sucesso", unidades));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca uma unidade pelo id (HU11)", description = "Responde 200; 404 quando a unidade não existe.")
    public ResponseEntity<ApiResponse<UnidadeResponse>> buscarPorId(@PathVariable UUID id) {
        UnidadeResponse unidade = unidadeService.buscarPorId(id);
        return ResponseEntity.ok(ApiResponse.sucesso(HttpStatus.OK, "Unidade encontrada", unidade));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Edita uma unidade (HU11)",
        description = "Mesmas regras do cadastro. Responde 200; 400 para dados inválidos ou identificação repetida; "
            + "404 quando a unidade não existe.")
    public ResponseEntity<ApiResponse<UnidadeResponse>> editar(@PathVariable UUID id,
                                                               @Valid @RequestBody UnidadeRequest request) {
        UnidadeResponse unidade = unidadeService.editar(id, request);
        return ResponseEntity.ok(ApiResponse.sucesso(HttpStatus.OK, "Unidade atualizada com sucesso", unidade));
    }

    @PatchMapping("/{id}/desativar")
    @Operation(summary = "Desativa uma unidade (HU11)",
        description = "Exclusão lógica: a unidade não recebe novos chamados, mas continua nos chamados antigos. "
            + "Responde 200; 404 quando a unidade não existe.")
    public ResponseEntity<ApiResponse<UnidadeResponse>> desativar(@PathVariable UUID id) {
        UnidadeResponse unidade = unidadeService.desativar(id);
        return ResponseEntity.ok(ApiResponse.sucesso(HttpStatus.OK, "Unidade desativada com sucesso", unidade));
    }
}
