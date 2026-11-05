package com.avandesk.api.chamado.controller;

import com.avandesk.api.chamado.dto.ChamadoDetalheResponse;
import com.avandesk.api.chamado.dto.ChamadoRequest;
import com.avandesk.api.chamado.dto.ChamadoResponse;
import com.avandesk.api.chamado.dto.FilaChamadosResponse;
import com.avandesk.api.chamado.dto.FiltroChamados;
import com.avandesk.api.chamado.dto.PrioridadeRequest;
import com.avandesk.api.chamado.dto.RegistroContatoRequest;
import com.avandesk.api.chamado.dto.ResolucaoRequest;
import com.avandesk.api.chamado.dto.ResponsavelRequest;
import com.avandesk.api.chamado.dto.StatusRequest;
import com.avandesk.api.chamado.entity.CanalOrigem;
import com.avandesk.api.chamado.entity.Prioridade;
import com.avandesk.api.chamado.entity.StatusChamado;
import com.avandesk.api.chamado.service.ChamadoService;
import com.avandesk.api.chamado.service.FilaChamadosService;
import com.avandesk.api.core.dto.ApiResponse;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/chamados")
@RequiredArgsConstructor
@Tag(name = "Chamados", description = "Registro, fila, detalhe e ciclo de atendimento dos chamados")
public class ChamadoController {

    private final ChamadoService chamadoService;
    private final FilaChamadosService filaChamadosService;

    @PostMapping
    @Operation(summary = "Registra um chamado (HU01)",
        description = "Grava o chamado, o contato inicial e os eventos CHAMADO_ABERTO e REGISTRO_DE_CONTATO_RECEBIDO. "
            + "Com atribuir_a_mim, quem registra assume o chamado, que vai para EM_ATENDIMENTO. "
            + "Responde 201; 400 para dados inválidos; 404 para unidade, categoria ou sistema inexistente; "
            + "422 quando quem marca atribuir_a_mim não é analista ativo.")
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
    @Operation(summary = "Lista e busca chamados na fila (HU14)")
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
    @Operation(summary = "Consulta o detalhe, os contatos e o histórico do chamado (HU02, HU05)",
        description = "Responde 200; 404 quando o chamado não existe.")
    public ResponseEntity<ApiResponse<ChamadoDetalheResponse>> detalhar(@PathVariable UUID id) {
        ChamadoDetalheResponse chamado = chamadoService.detalhar(id);
        return ResponseEntity.ok(ApiResponse.sucesso(HttpStatus.OK, "Detalhes do chamado recuperados", chamado));
    }

    @PatchMapping("/{id}/responsavel")
    @Operation(summary = "Assume ou reatribui o chamado (HU12)",
        description = "Para assumir, mande o próprio id. Só analista de suporte ativo pode ser responsável. "
            + "Grava CHAMADO_ATRIBUIDO. Responde 200 com o detalhe; 404 para chamado ou usuário inexistente; "
            + "422 para usuário de outro perfil ou inativo, mesmo responsável ou chamado encerrado.")
    public ResponseEntity<ApiResponse<ChamadoDetalheResponse>> atribuirResponsavel(
        @PathVariable UUID id, @Valid @RequestBody ResponsavelRequest request) {
        ChamadoDetalheResponse chamado = chamadoService.atribuirResponsavel(id, request.responsavelId());
        return ResponseEntity.ok(ApiResponse.sucesso(HttpStatus.OK, "Responsável atribuído com sucesso", chamado));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Altera o status do chamado (HU12)",
        description = "Segue o ciclo de vida: ABERTO para EM_TRIAGEM, EM_TRIAGEM para EM_ATENDIMENTO (exige responsável), "
            + "EM_ATENDIMENTO e AGUARDANDO entre si (AGUARDANDO exige motivo_espera), ABERTO ou EM_TRIAGEM para "
            + "CANCELADO (exige motivo_cancelamento), RESOLVIDO para REABERTO (exige motivo_reabertura, até 7 dias "
            + "depois da resolução) e REABERTO para EM_ATENDIMENTO. Responde 200 com o detalhe; 404 para chamado "
            + "inexistente; 422 para transição fora do ciclo, falta de motivo, prazo vencido ou chamado encerrado.")
    public ResponseEntity<ApiResponse<ChamadoDetalheResponse>> alterarStatus(
        @PathVariable UUID id, @Valid @RequestBody StatusRequest request) {
        ChamadoDetalheResponse chamado = chamadoService.alterarStatus(id, request);
        return ResponseEntity.ok(ApiResponse.sucesso(HttpStatus.OK, "Status do chamado atualizado com sucesso", chamado));
    }

    @PatchMapping("/{id}/resolucao")
    @Operation(summary = "Registra a resolução do chamado (HU06)",
        description = "Só a partir de EM_ATENDIMENTO ou AGUARDANDO. Resolução obrigatória (10 a 2000 caracteres); "
            + "causa_raiz e possivel_recorrencia são opcionais. Grava CHAMADO_RESOLVIDO com o texto da resolução. "
            + "Responde 200 com o detalhe; 400 para dados inválidos; 404 para chamado inexistente; "
            + "422 para status que não pode ir para RESOLVIDO.")
    public ResponseEntity<ApiResponse<ChamadoDetalheResponse>> resolver(
        @PathVariable UUID id, @Valid @RequestBody ResolucaoRequest request) {
        ChamadoDetalheResponse chamado = chamadoService.resolver(id, request);
        return ResponseEntity.ok(ApiResponse.sucesso(HttpStatus.OK, "Chamado resolvido com sucesso", chamado));
    }

    @PostMapping("/{id}/contatos")
    @Operation(summary = "Registra um novo contato no chamado (HU13)",
        description = "Canal e conteúdo são obrigatórios. Sem direção, vale RECEBIDO; sem solicitante, o do chamado; "
            + "sem ocorrido_em, agora. Grava REGISTRO_DE_CONTATO_RECEBIDO. Responde 201 com o detalhe; 400 para data "
            + "no futuro ou antes da abertura e identificador externo repetido no canal; 404 para chamado inexistente; "
            + "422 para chamado fechado, cancelado ou mesclado.")
    public ResponseEntity<ApiResponse<ChamadoDetalheResponse>> registrarContato(
        @PathVariable UUID id, @Valid @RequestBody RegistroContatoRequest request) {
        ChamadoDetalheResponse chamado = chamadoService.registrarContato(id, request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.sucesso(HttpStatus.CREATED, "Contato registrado com sucesso", chamado));
    }

    @PatchMapping("/{id}/prioridade")
    @Operation(summary = "Define a prioridade do chamado pela Matriz 3D (HU04)",
        description = "Calcula a prioridade sugerida com base em severidade, urgência e impacto (1 a 3). "
            + "Se a prioridade final for diferente da sugerida, a justificativa é obrigatória. "
            + "Grava o evento PRIORIDADE_DEFINIDA. Responde 200 com o detalhe atualizado; 400 para notas fora de 1 a 3; "
            + "404 para chamado inexistente; 422 para divergência sem justificativa ou chamado encerrado.")
    public ResponseEntity<ApiResponse<ChamadoDetalheResponse>> definirPrioridade(
        @PathVariable UUID id, @Valid @RequestBody PrioridadeRequest request) {
        ChamadoDetalheResponse chamado = chamadoService.definirPrioridade(id, request);
        return ResponseEntity.ok(ApiResponse.sucesso(HttpStatus.OK, "Prioridade do chamado definida com sucesso", chamado));
    }
}
