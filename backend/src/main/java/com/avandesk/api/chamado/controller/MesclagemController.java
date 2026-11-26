package com.avandesk.api.chamado.controller;

import com.avandesk.api.chamado.dto.ChamadoDetalheResponse;
import com.avandesk.api.chamado.dto.MesclagemRequest;
import com.avandesk.api.chamado.service.MesclagemService;
import com.avandesk.api.core.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/chamados")
@RequiredArgsConstructor
@Tag(name = "Mesclagem", description = "Mesclagem de chamados duplicados no chamado principal (HU03)")
public class MesclagemController {

    private final MesclagemService mesclagemService;

    @PostMapping("/{id}/mesclar")
    @Operation(summary = "Mescla um chamado duplicado no chamado principal (HU03)",
        description = "O chamado do caminho é o duplicado (secundário) e vai para MESCLADO, apontando o principal. "
            + "Os registros de contato dele passam para o principal com a origem em chamado_original, e o "
            + "secundário fica com o responsável do principal. O principal mantém responsável e prioridade. Grava "
            + "CHAMADO_MESCLADO no secundário e um RELATO_VINCULADO por contato no principal. Para achar o principal "
            + "pelo protocolo, use GET /chamados?codigo=. Responde 200 com o detalhe do secundário; 400 sem "
            + "chamado_principal_id; 404 para chamado ou principal inexistente; 422 para o mesmo chamado, secundário "
            + "resolvido ou encerrado, principal fechado, cancelado ou mesclado, ou secundário aguardando fornecedor.")
    public ResponseEntity<ApiResponse<ChamadoDetalheResponse>> mesclar(
        @PathVariable UUID id, @Valid @RequestBody MesclagemRequest request) {
        ChamadoDetalheResponse chamado = mesclagemService.mesclar(id, request.chamadoPrincipalId());
        return ResponseEntity.ok(ApiResponse.sucesso(HttpStatus.OK, "Chamado mesclado com sucesso", chamado));
    }
}
