package com.avandesk.api.usuario.controller;

import com.avandesk.api.core.dto.ApiResponse;
import com.avandesk.api.unidade.dto.UnidadeResumoResponse;
import com.avandesk.api.usuario.dto.CadastroSolicitanteRequest;
import com.avandesk.api.usuario.dto.CadastroSolicitanteResponse;
import com.avandesk.api.usuario.dto.ReenvioVerificacaoRequest;
import com.avandesk.api.usuario.dto.VerificacaoEmailResponse;
import com.avandesk.api.usuario.dto.VerificarEmailRequest;
import com.avandesk.api.usuario.service.CadastroSolicitanteService;
import com.avandesk.api.usuario.service.VerificacaoEmailService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Conta do solicitante", description = "Auto-cadastro e verificação de e-mail do solicitante (HU17)")
public class CadastroSolicitanteController {

    private final CadastroSolicitanteService cadastroSolicitanteService;
    private final VerificacaoEmailService verificacaoEmailService;

    @PostMapping("/registrar")
    @Operation(summary = "Cria a conta do solicitante e envia o link de verificação (HU17)",
        description = "Público. Exige nome, e-mail, unidade padrão ativa e senha forte (10+ caracteres com maiúscula, "
            + "minúscula, número e símbolo). Com nexo.solicitantes.restringir-dominio-email=true, só aceita os "
            + "domínios de nexo.solicitantes.dominios-permitidos. A conta nasce PENDENTE_VERIFICACAO e recebe por "
            + "e-mail um link válido por 24 h. Responde 201; 400 para dados inválidos, senha fraca ou domínio "
            + "recusado; 404 para unidade inexistente; 409 para e-mail já cadastrado.")
    public ResponseEntity<ApiResponse<CadastroSolicitanteResponse>> registrar(
        @Valid @RequestBody CadastroSolicitanteRequest request
    ) {
        CadastroSolicitanteResponse conta = cadastroSolicitanteService.registrar(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.sucesso(HttpStatus.CREATED,
            "Conta criada. Enviamos um link de confirmação para o seu e-mail.", conta));
    }

    @PostMapping("/verificar-email")
    @Operation(summary = "Confirma o e-mail do solicitante pelo token do link (HU17)",
        description = "Público. Ativa a conta e vincula o solicitante ao usuário: chamados abertos antes pela equipe "
            + "com o mesmo e-mail passam a ser dele. Responde 200 com chamados_vinculados; 422 com errors[0] = "
            + "TOKEN_INVALIDO, TOKEN_UTILIZADO, TOKEN_SUBSTITUIDO ou TOKEN_EXPIRADO.")
    public ResponseEntity<ApiResponse<VerificacaoEmailResponse>> verificarEmail(
        @Valid @RequestBody VerificarEmailRequest request
    ) {
        VerificacaoEmailResponse verificacao = verificacaoEmailService.verificar(request.token());
        return ResponseEntity.ok(ApiResponse.sucesso(HttpStatus.OK, "E-mail confirmado. A sua conta está ativa.",
            verificacao));
    }

    @PostMapping("/reenviar-verificacao")
    @Operation(summary = "Envia um novo link de verificação (HU17)",
        description = "Público. Sempre responde 202 com a mesma mensagem, exista ou não a conta, para não revelar "
            + "e-mails cadastrados. Só envia para conta de solicitante pendente e respeita o intervalo de "
            + "nexo.solicitantes.intervalo-reenvio (60 s) desde o último link; os links anteriores deixam de valer.")
    public ResponseEntity<ApiResponse<Void>> reenviarVerificacao(@Valid @RequestBody ReenvioVerificacaoRequest request) {
        verificacaoEmailService.reenviar(request.email());
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(ApiResponse.sucesso(HttpStatus.ACCEPTED,
            "Se houver uma conta pendente com este e-mail, enviamos um novo link de confirmação.", null));
    }

    @GetMapping("/unidades")
    @Operation(summary = "Lista as unidades ativas para a tela de cadastro (HU17)",
        description = "Público. Traz só id e nome das unidades ativas, em ordem alfabética.")
    public ResponseEntity<ApiResponse<List<UnidadeResumoResponse>>> unidades() {
        return ResponseEntity.ok(ApiResponse.sucesso(HttpStatus.OK, "Unidades disponíveis para cadastro",
            cadastroSolicitanteService.unidadesParaCadastro()));
    }
}
