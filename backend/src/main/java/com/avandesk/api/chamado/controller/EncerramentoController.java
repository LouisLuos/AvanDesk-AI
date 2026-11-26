package com.avandesk.api.chamado.controller;

import com.avandesk.api.chamado.dto.ChamadoDetalheResponse;
import com.avandesk.api.chamado.dto.FechamentoRequest;
import com.avandesk.api.chamado.dto.VerificacaoEncerramentoResponse;
import com.avandesk.api.chamado.service.EncerramentoService;
import com.avandesk.api.core.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@RequestMapping("/api/v1/chamados/{id}")
@RequiredArgsConstructor
@Tag(name = "Encerramento", description = "Verificação de rastreabilidade e encerramento do chamado (HU15)")
public class EncerramentoController {

    private final EncerramentoService encerramentoService;

    @GetMapping("/verificacao-encerramento")
    @Operation(summary = "Confere os seis itens de rastreabilidade antes de encerrar (HU15)",
        description = "Só para chamado RESOLVIDO. Problema e categoria, unidade, responsável, ao menos um contato, "
            + "causa raiz (ou justificativa no encerramento) e resolução detalhada. Cada item vem com ok e, quando "
            + "falta, a pendência. Responde 200; 404 para chamado inexistente; 422 para status diferente de RESOLVIDO.")
    public ResponseEntity<ApiResponse<VerificacaoEncerramentoResponse>> verificar(@PathVariable UUID id) {
        VerificacaoEncerramentoResponse verificacao = encerramentoService.verificar(id);
        return ResponseEntity.ok(ApiResponse.sucesso(HttpStatus.OK, "Verificação de encerramento concluída", verificacao));
    }

    @PostMapping("/fechar")
    @Operation(summary = "Encerra o chamado resolvido depois da verificação de rastreabilidade (HU15)",
        description = "Só a partir de RESOLVIDO e após o prazo de 7 dias para reabertura (RN20). Roda os seis itens da "
            + "verificação; sem causa raiz, aceita justificativa_sem_causa_raiz com pelo menos 15 caracteres. Aprovado, "
            + "o chamado vai para FECHADO, grava fechado_em e o evento CHAMADO_FECHADO com o resultado da verificação "
            + "(M01). Responde 200 com o detalhe; 400 para dados inválidos; 404 para chamado inexistente; 422 para "
            + "status diferente de RESOLVIDO, prazo de 7 dias ainda em aberto ou pendências, listadas em errors.")
    public ResponseEntity<ApiResponse<ChamadoDetalheResponse>> fechar(
        @PathVariable UUID id, @Valid @RequestBody(required = false) FechamentoRequest request) {
        ChamadoDetalheResponse chamado = encerramentoService.fechar(id, request);
        return ResponseEntity.ok(ApiResponse.sucesso(HttpStatus.OK, "Chamado encerrado", chamado));
    }
}
