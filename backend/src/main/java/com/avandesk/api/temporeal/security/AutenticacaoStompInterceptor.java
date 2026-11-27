package com.avandesk.api.temporeal.security;

import com.avandesk.api.usuario.entity.Usuario;
import com.avandesk.api.usuario.repository.UsuarioRepository;
import com.avandesk.api.usuario.service.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class AutenticacaoStompInterceptor implements ChannelInterceptor {

    public static final String TOPICO_CHAMADOS = "/topic/chamados";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final UsuarioRepository usuarioRepository;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }
        switch (accessor.getCommand()) {
            case CONNECT -> accessor.setUser(autenticar(accessor));
            case SUBSCRIBE -> validarAssinatura(accessor);
            case SEND -> throw new MessageDeliveryException("O tempo real só envia do servidor para o cliente.");
            default -> {
            }
        }
        return message;
    }

    private UsernamePasswordAuthenticationToken autenticar(StompHeaderAccessor accessor) {
        String ticket = extrairTicket(accessor)
            .orElseThrow(() -> new MessageDeliveryException("Informe o ticket de tempo real no header Authorization."));
        if (!jwtService.isTokenValido(ticket) || !jwtService.ehTicketTempoReal(ticket)) {
            throw new MessageDeliveryException("Ticket de tempo real inválido ou expirado.");
        }
        Usuario usuario = usuarioRepository.findById(jwtService.obterUsuarioId(ticket))
            .filter(Usuario::isAtivo)
            .orElseThrow(() -> new MessageDeliveryException("Usuário do ticket não está ativo."));
        return new UsernamePasswordAuthenticationToken(usuario.getEmail(), null,
            List.of(new SimpleGrantedAuthority("ROLE_" + usuario.getPerfil().name())));
    }

    private static void validarAssinatura(StompHeaderAccessor accessor) {
        if (accessor.getUser() == null) {
            throw new MessageDeliveryException("Conecte com um ticket antes de assinar.");
        }
        if (!TOPICO_CHAMADOS.equals(accessor.getDestination())) {
            throw new MessageDeliveryException("Destino não permitido: " + accessor.getDestination());
        }
    }

    private static Optional<String> extrairTicket(StompHeaderAccessor accessor) {
        String header = accessor.getFirstNativeHeader("Authorization");
        if (!StringUtils.hasText(header) || !header.startsWith(BEARER_PREFIX)) {
            return Optional.empty();
        }
        return Optional.of(header.substring(BEARER_PREFIX.length()).trim()).filter(StringUtils::hasText);
    }
}
