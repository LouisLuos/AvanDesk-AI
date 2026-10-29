package com.avandesk.api.chamado.controller;

import com.avandesk.api.chamado.dto.ChamadoDetalheResponse;
import com.avandesk.api.chamado.dto.ChamadoRequest;
import com.avandesk.api.chamado.dto.ChamadoResponse;
import com.avandesk.api.chamado.dto.FilaChamadosResponse;
import com.avandesk.api.chamado.dto.FiltroChamados;
import com.avandesk.api.chamado.dto.ResponsavelRequest;
import com.avandesk.api.chamado.entity.CanalOrigem;
import com.avandesk.api.chamado.entity.Prioridade;
import com.avandesk.api.chamado.entity.StatusChamado;
import com.avandesk.api.chamado.service.ChamadoService;
import com.avandesk.api.chamado.service.FilaChamadosService;
import com.avandesk.api.core.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/chamados")
@RequiredArgsConstructor
public class ChamadoController {

    private final ChamadoService chamadoService;
    private final FilaChamadosService filaChamadosService;

    @PostMapping
    public ResponseEntity<ApiResponse<ChamadoResponse>> registrar(@Valid @RequestBody ChamadoRequest request) {
        ChamadoResponse chamado = chamadoService.registrar(request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.sucesso(HttpStatus.CREATED, "Chamado registrado com sucesso", chamado));
    }

    /**
     * Fila de chamados (HU14): paginada, do mais recente para o mais antigo. Todos os filtros são
     * opcionais e se somam; {@code status} pode vir repetido. Valor inválido em um filtro responde 400.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<FilaChamadosResponse>> listar(
        @RequestParam(name = "page", defaultValue = "0") int pagina,
        @RequestParam(name = "size", defaultValue = "" + FilaChamadosService.TAMANHO_PADRAO) int tamanho,
        @RequestParam(name = "status", required = false) List<StatusChamado> status,
        @RequestParam(name = "unidade_id", required = false) UUID unidadeId,
        @RequestParam(name = "prioridade", required = false) Prioridade prioridade,
        @RequestParam(name = "canal_origem", required = false) CanalOrigem canalOrigem,
        @RequestParam(name = "responsavel_id", required = false) UUID responsavelId,
        @RequestParam(name = "sem_responsavel", defaultValue = "false") boolean semResponsavel,
        @RequestParam(name = "sem_prioridade", defaultValue = "false") boolean semPrioridade,
        @RequestParam(name = "meus", defaultValue = "false") boolean meus,
        @RequestParam(name = "codigo", required = false) String codigo
    ) {
        FiltroChamados filtro = new FiltroChamados(
            status, unidadeId, prioridade, canalOrigem, responsavelId, semResponsavel, semPrioridade, codigo);
        FilaChamadosResponse fila = filaChamadosService.listar(filtro, meus, pagina, tamanho);
        return ResponseEntity.ok(ApiResponse.sucesso(HttpStatus.OK, "Chamados recuperados com sucesso", fila));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ChamadoDetalheResponse>> detalhar(@PathVariable UUID id) {
        ChamadoDetalheResponse chamado = chamadoService.detalhar(id);
        return ResponseEntity.ok(ApiResponse.sucesso(HttpStatus.OK, "Detalhes do chamado recuperados", chamado));
    }

    @PatchMapping("/{id}/responsavel")
    public ResponseEntity<ApiResponse<ChamadoDetalheResponse>> atribuirResponsavel(
        @PathVariable UUID id, @Valid @RequestBody ResponsavelRequest request) {
        ChamadoDetalheResponse chamado = chamadoService.atribuirResponsavel(id, request.responsavelId());
        return ResponseEntity.ok(ApiResponse.sucesso(HttpStatus.OK, "Responsável atribuído com sucesso", chamado));
    }
}
