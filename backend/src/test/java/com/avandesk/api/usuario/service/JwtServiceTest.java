package com.avandesk.api.usuario.service;

import com.avandesk.api.usuario.entity.PerfilUsuario;
import com.avandesk.api.usuario.entity.Usuario;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Testes unitários de segurança para o JwtService (QA2-01, RNF-005, RNF-007).
 */
class JwtServiceTest {

    private static final String SECRET_TESTE = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    private static final long EXPIRACAO_1_HORA = 3600000L;

    private JwtService jwtService;
    private Usuario usuario;
    private UUID usuarioId;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET_TESTE, EXPIRACAO_1_HORA);

        usuarioId = UUID.randomUUID();
        usuario = new Usuario();
        usuario.setId(usuarioId);
        usuario.setNome("Ana Silva Suporte");
        usuario.setEmail("ana.silva@lumendiagnosticos.com.br");
        usuario.setPerfil(PerfilUsuario.ANALISTA_SUPORTE);
        usuario.setAtivo(true);
    }

    @Test
    @DisplayName("QA2-01: Deve emitir token JWT válido com claims id, email, role, sub e validade de 1 hora")
    void deveEmitirTokenJwtComClaimsCorretasEAssinaturaValida() {
        String token = jwtService.gerarToken(usuario);

        assertThat(token).isNotBlank();
        assertThat(jwtService.isTokenValido(token)).isTrue();

        Claims claims = jwtService.extrairClaims(token);
        assertThat(claims.getSubject()).isEqualTo(usuarioId.toString());
        assertThat(claims.get("id", String.class)).isEqualTo(usuarioId.toString());
        assertThat(claims.get("email", String.class)).isEqualTo("ana.silva@lumendiagnosticos.com.br");
        assertThat(claims.get("role", String.class)).isEqualTo("ANALISTA_SUPORTE");
        assertThat(claims.get("nome", String.class)).isEqualTo("Ana Silva Suporte");

        long diferencaExpiracaoMs = claims.getExpiration().getTime() - claims.getIssuedAt().getTime();
        assertThat(diferencaExpiracaoMs).isEqualTo(EXPIRACAO_1_HORA);
    }

    @Test
    @DisplayName("QA2-01: Deve rejeitar token com assinatura violada/adulterada")
    void deveRejeitarTokenComAssinaturaAdulterada() {
        String tokenValido = jwtService.gerarToken(usuario);
        String tokenAdulterado = tokenValido.substring(0, tokenValido.length() - 5) + "abcde";

        assertThat(jwtService.isTokenValido(tokenAdulterado)).isFalse();
    }

    @Test
    @DisplayName("QA2-01: Deve rejeitar token expirado")
    void deveRejeitarTokenExpirado() {
        // JwtService com expiração negativa (já nasce expirado)
        JwtService jwtServiceExpirado = new JwtService(SECRET_TESTE, -1000L);
        String tokenExpirado = jwtServiceExpirado.gerarToken(usuario);

        assertThat(jwtService.isTokenValido(tokenExpirado)).isFalse();
    }

    @Test
    @DisplayName("QA2-01: Deve extrair email, role e usuarioId corretamente")
    void deveExtrairInformacoesUtilitariasDoToken() {
        String token = jwtService.gerarToken(usuario);

        assertThat(jwtService.obterEmail(token)).isEqualTo("ana.silva@lumendiagnosticos.com.br");
        assertThat(jwtService.obterRole(token)).isEqualTo("ANALISTA_SUPORTE");
        assertThat(jwtService.obterUsuarioId(token)).isEqualTo(usuarioId);
        assertThat(jwtService.obterSubject(token)).isEqualTo(usuarioId.toString());
    }
}
