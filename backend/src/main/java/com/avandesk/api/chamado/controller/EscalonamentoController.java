package com.avandesk.api.chamado.controller;

import com.avandesk.api.chamado.dto.ChamadoDetalheResponse;
import com.avandesk.api.chamado.dto.EscalonamentoRequest;
import com.avandesk.api.chamado.dto.RetornoFornecedorRequest;
import com.avandesk.api.chamado.service.EscalonamentoService;
import com.avandesk.api.core.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/chamados/{id}/escalonamentos")
@RequiredArgsConstructor
@Tag(name = "Escalonamentos", description = "Envio do chamado a um fornecedor externo e registro do retorno (HU07)")
public class EscalonamentoController {

    private final EscalonamentoService escalonamentoService;

    @PostMapping
    @Operation(summary = "Escala o chamado para um fornecedor externo (HU07)",
        description = "Só a partir de EM_ATENDIMENTO. O chamado vai para AGUARDANDO com motivo FORNECEDOR e o "
            + "responsável interno continua o mesmo. Fornecedor obrigatório (até 120 caracteres); protocolo_externo, "
            + "observacao e enviado_em são opcionais (sem enviado_em, vale agora). Grava CHAMADO_ESCALADO. "
            + "Responde 201 com o detalhe; 400 para dados inválidos, enviado_em no futuro, antes da abertura ou antes "
            + "do retorno do escalonamento anterior; 404 para chamado inexistente; 422 para chamado fora de "
            + "EM_ATENDIMENTO, sem responsável ou com escalonamento sem retorno.")
    public ResponseEntity<ApiResponse<ChamadoDetalheResponse>> escalar(
        @PathVariable UUID id, @Valid @RequestBody EscalonamentoRequest request) {
        ChamadoDetalheResponse chamado = escalonamentoService.escalar(id, request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.sucesso(HttpStatus.CREATED, "Chamado escalado para o fornecedor", chamado));
    }

    @PatchMapping("/{escalonamentoId}/retorno")
    @Operation(summary = "Registra o retorno do fornecedor e encerra o escalonamento (HU07)",
        description = "O chamado volta de AGUARDANDO (motivo FORNECEDOR) para EM_ATENDIMENTO. Retorno obrigatório "
            + "(até 2000 caracteres); sem retorno_em, vale agora. Grava RETORNO_RECEBIDO. Responde 200 com o detalhe; "
            + "400 para dados inválidos, retorno_em no futuro ou antes do envio; 404 para chamado inexistente ou "
            + "escalonamento que não é deste chamado; 422 para escalonamento já com retorno ou chamado que não está "
            + "aguardando fornecedor.")
    public ResponseEntity<ApiResponse<ChamadoDetalheResponse>> registrarRetorno(
        @PathVariable UUID id, @PathVariable UUID escalonamentoId, @Valid @RequestBody RetornoFornecedorRequest request) {
        ChamadoDetalheResponse chamado = escalonamentoService.registrarRetorno(id, escalonamentoId, request);
        return ResponseEntity.ok(ApiResponse.sucesso(HttpStatus.OK, "Retorno do fornecedor registrado", chamado));
    }
}
