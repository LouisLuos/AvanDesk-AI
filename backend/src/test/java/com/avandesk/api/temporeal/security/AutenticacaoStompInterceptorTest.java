package com.avandesk.api.temporeal.security;

import com.avandesk.api.usuario.entity.PerfilUsuario;
import com.avandesk.api.usuario.entity.Usuario;
import com.avandesk.api.usuario.repository.UsuarioRepository;
import com.avandesk.api.usuario.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;

import java.security.Principal;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AutenticacaoStompInterceptorTest {

    private final JwtService jwtService = new JwtService(
        "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970", 3_600_000);
    private final MessageChannel canal = mock(MessageChannel.class);

    @Mock
    private UsuarioRepository usuarioRepository;

    private AutenticacaoStompInterceptor interceptor;
    private Usuario analista;

    @BeforeEach
    void configurar() {
        interceptor = new AutenticacaoStompInterceptor(jwtService, usuarioRepository);
        analista = new Usuario();
        analista.setId(UUID.randomUUID());
        analista.setNome("Analista Dev Um");
        analista.setEmail("analista1@lumen.local");
        analista.setPerfil(PerfilUsuario.ANALISTA_SUPORTE);
        analista.setAtivo(true);
    }

    @Test
    void deveConectarComTicketValidoEIdentificarOUsuario() {
        when(usuarioRepository.findById(analista.getId())).thenReturn(Optional.of(analista));
        String ticket = jwtService.gerarTicketTempoReal(analista, Duration.ofSeconds(60));

        Message<?> resultado = interceptor.preSend(connect("Bearer " + ticket), canal);

        Principal usuario = StompHeaderAccessor.wrap(resultado).getUser();
        assertThat(usuario).isNotNull();
        assertThat(usuario.getName()).isEqualTo("analista1@lumen.local");
    }

    @Test
    void deveRecusarConexaoSemTicket() {
        assertThatThrownBy(() -> interceptor.preSend(connect(null), canal))
            .isInstanceOf(MessageDeliveryException.class)
            .hasMessageContaining("ticket");
    }

    @Test
    void deveRecusarOTokenDeSessaoNoLugarDoTicket() {
        String tokenDeSessao = jwtService.gerarToken(analista);

        assertThatThrownBy(() -> interceptor.preSend(connect("Bearer " + tokenDeSessao), canal))
            .isInstanceOf(MessageDeliveryException.class)
            .hasMessageContaining("inválido ou expirado");
    }

    @Test
    void deveRecusarTicketExpiradoOuAdulterado() {
        String expirado = jwtService.gerarTicketTempoReal(analista, Duration.ofSeconds(-5));
        String adulterado = jwtService.gerarTicketTempoReal(analista, Duration.ofSeconds(60)) + "x";

        assertThatThrownBy(() -> interceptor.preSend(connect("Bearer " + expirado), canal))
            .isInstanceOf(MessageDeliveryException.class);
        assertThatThrownBy(() -> interceptor.preSend(connect("Bearer " + adulterado), canal))
            .isInstanceOf(MessageDeliveryException.class);
    }

    @Test
    void deveRecusarTicketDeUsuarioInativo() {
        analista.setAtivo(false);
        when(usuarioRepository.findById(analista.getId())).thenReturn(Optional.of(analista));
        String ticket = jwtService.gerarTicketTempoReal(analista, Duration.ofSeconds(60));

        assertThatThrownBy(() -> interceptor.preSend(connect("Bearer " + ticket), canal))
            .isInstanceOf(MessageDeliveryException.class)
            .hasMessageContaining("ativo");
    }

    @Test
    void deveDeixarAssinarSoOTopicoDeChamadosDepoisDeConectar() {
        Principal usuario = () -> "analista1@lumen.local";

        assertThat(interceptor.preSend(subscribe("/topic/chamados", usuario), canal)).isNotNull();
        assertThatThrownBy(() -> interceptor.preSend(subscribe("/topic/outro", usuario), canal))
            .isInstanceOf(MessageDeliveryException.class);
        assertThatThrownBy(() -> interceptor.preSend(subscribe("/topic/chamados", null), canal))
            .isInstanceOf(MessageDeliveryException.class);
    }

    @Test
    void deveRecusarEnvioDoClienteParaOServidor() {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SEND);
        accessor.setDestination("/app/qualquer");
        accessor.setLeaveMutable(true);

        assertThatThrownBy(() -> interceptor.preSend(mensagem(accessor), canal))
            .isInstanceOf(MessageDeliveryException.class);
    }

    private static Message<byte[]> connect(String authorization) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        if (authorization != null) {
            accessor.addNativeHeader("Authorization", authorization);
        }
        accessor.setLeaveMutable(true);
        return mensagem(accessor);
    }

    private static Message<byte[]> subscribe(String destino, Principal usuario) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination(destino);
        accessor.setUser(usuario);
        accessor.setLeaveMutable(true);
        return mensagem(accessor);
    }

    private static Message<byte[]> mensagem(StompHeaderAccessor accessor) {
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }
}
