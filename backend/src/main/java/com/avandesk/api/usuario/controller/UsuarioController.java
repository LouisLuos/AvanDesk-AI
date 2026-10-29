package com.avandesk.api.usuario.controller;

import com.avandesk.api.core.dto.ApiResponse;
import com.avandesk.api.usuario.dto.UsuarioAutenticadoResponse;
import com.avandesk.api.usuario.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping({"/api/v1/usuarios", "/usuarios"})
@RequiredArgsConstructor
@Tag(name = "Usuários", description = "Endpoints de consulta e gerenciamento de usuários")
public class UsuarioController {

    private final UsuarioService usuarioService;

    @GetMapping("/analistas")
    @Operation(summary = "Lista todos os analistas de suporte ativos para atribuição de chamados (HU12)")
    public ResponseEntity<ApiResponse<List<UsuarioAutenticadoResponse>>> listarAnalistas() {
        List<UsuarioAutenticadoResponse> analistas = usuarioService.listarAnalistasAtivos();
        return ResponseEntity.ok(ApiResponse.sucesso(HttpStatus.OK, "Analistas recuperados com sucesso", analistas));
    }
}
