package com.avandesk.api.usuario.controller;

import com.avandesk.api.core.dto.ApiResponse;
import com.avandesk.api.usuario.dto.AnalistaCadastradoResponse;
import com.avandesk.api.usuario.dto.AnalistaRequest;
import com.avandesk.api.usuario.dto.AnalistaResponse;
import com.avandesk.api.usuario.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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

/**
 * Gestão da equipe de analistas de suporte (HU10). A listagem serve qualquer usuário autenticado
 * (alimenta a atribuição de chamados); as alterações são só da Liderança de TI.
 */
@RestController
@RequestMapping("/api/v1/analistas")
@RequiredArgsConstructor
@Tag(name = "Analistas", description = "Gestão dos analistas de suporte (HU10)")
public class UsuarioController {

    private static final String SO_LIDERANCA = "hasRole('LIDERANCA_TI')";

    private final UsuarioService usuarioService;

    @GetMapping
    @Operation(summary = "Lista os analistas de suporte (HU10)",
        description = "Em ordem alfabética de nome. O parâmetro ativo filtra só os ativos (true) ou só os inativos "
            + "(false); sem ele, vêm todos. Responde 200.")
    public ResponseEntity<ApiResponse<List<AnalistaResponse>>> listar(@RequestParam(required = false) Boolean ativo) {
        List<AnalistaResponse> analistas = usuarioService.listarAnalistas(ativo);
        return ResponseEntity.ok(ApiResponse.sucesso(HttpStatus.OK, "Analistas recuperados com sucesso", analistas));
    }

    @PostMapping
    @PreAuthorize(SO_LIDERANCA)
    @Operation(summary = "Cadastra um analista com senha temporária (HU10)",
        description = "Nome e e-mail corporativo são obrigatórios; o perfil é sempre ANALISTA_SUPORTE. A senha "
            + "temporária vem só nesta resposta. Responde 201; 400 para dados inválidos ou domínio não corporativo; "
            + "403 fora da Liderança de TI; 409 para e-mail repetido.")
    public ResponseEntity<ApiResponse<AnalistaCadastradoResponse>> cadastrar(@Valid @RequestBody AnalistaRequest request) {
        AnalistaCadastradoResponse cadastrado = usuarioService.cadastrarAnalista(request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.sucesso(HttpStatus.CREATED, "Analista cadastrado com sucesso", cadastrado));
    }

    @PutMapping("/{id}")
    @PreAuthorize(SO_LIDERANCA)
    @Operation(summary = "Edita um analista (HU10)",
        description = "Mesmas regras do cadastro. Responde 200; 400 para dados inválidos; 403 fora da Liderança "
            + "de TI; 404 quando o analista não existe; 409 para e-mail repetido.")
    public ResponseEntity<ApiResponse<AnalistaResponse>> editar(@PathVariable UUID id,
                                                                @Valid @RequestBody AnalistaRequest request) {
        AnalistaResponse analista = usuarioService.editarAnalista(id, request);
        return ResponseEntity.ok(ApiResponse.sucesso(HttpStatus.OK, "Analista atualizado com sucesso", analista));
    }

    @PatchMapping("/{id}/desativar")
    @PreAuthorize(SO_LIDERANCA)
    @Operation(summary = "Desativa um analista (HU10)",
        description = "Exclusão lógica: o analista perde o acesso na próxima requisição e não recebe novos "
            + "chamados, mas continua como autor no histórico. Responde 200; 403 fora da Liderança de TI; "
            + "404 quando o analista não existe.")
    public ResponseEntity<ApiResponse<AnalistaResponse>> desativar(@PathVariable UUID id) {
        AnalistaResponse analista = usuarioService.desativarAnalista(id);
        return ResponseEntity.ok(ApiResponse.sucesso(HttpStatus.OK, "Analista desativado com sucesso", analista));
    }

    @PatchMapping("/{id}/reativar")
    @PreAuthorize(SO_LIDERANCA)
    @Operation(summary = "Reativa um analista (HU10)",
        description = "Devolve o acesso e a possibilidade de receber chamados. Responde 200; 403 fora da "
            + "Liderança de TI; 404 quando o analista não existe.")
    public ResponseEntity<ApiResponse<AnalistaResponse>> reativar(@PathVariable UUID id) {
        AnalistaResponse analista = usuarioService.reativarAnalista(id);
        return ResponseEntity.ok(ApiResponse.sucesso(HttpStatus.OK, "Analista reativado com sucesso", analista));
    }
}
