package com.avandesk.api.temporeal;

import com.avandesk.api.chamado.dto.ChamadoResumoResponse;
import com.avandesk.api.chamado.dto.ResponsavelResponse;
import com.avandesk.api.chamado.entity.CanalOrigem;
import com.avandesk.api.chamado.entity.Prioridade;
import com.avandesk.api.chamado.entity.StatusChamado;
import com.avandesk.api.temporeal.dto.EventoTempoRealMessage;
import com.avandesk.api.temporeal.dto.TipoEventoTempoReal;
import com.avandesk.api.temporeal.port.EventoTempoRealPort;
import com.avandesk.api.unidade.dto.UnidadeResumoResponse;
import com.avandesk.api.usuario.entity.Usuario;
import com.avandesk.api.usuario.repository.UsuarioRepository;
import com.avandesk.api.usuario.service.JwtService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.messaging.converter.StringMessageConverter;
import org.springframework.util.MimeTypeUtils;

import java.lang.reflect.Type;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TempoRealStompE2ETest {

    @LocalServerPort
    private int porta;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private EventoTempoRealPort eventoTempoRealPort;

    private final WebSocketStompClient cliente = new WebSocketStompClient(new StandardWebSocketClient());

    @AfterEach
    void parar() {
        cliente.stop();
    }

    private static final class ConversorDeJsonParaTexto extends StringMessageConverter {

        ConversorDeJsonParaTexto() {
            addSupportedMimeTypes(MimeTypeUtils.APPLICATION_JSON);
        }
    }

    @Test
    void deveEntregarAMensagemPublicadaAoClienteConectadoComTicketEmMenosDe500ms() throws Exception {
        cliente.setMessageConverter(new ConversorDeJsonParaTexto());
        Usuario analista = usuarioRepository.findByEmail("analista1@lumen.local").orElseThrow();
        StompSession sessao = conectar("Bearer " + jwtService.gerarTicketTempoReal(analista, Duration.ofSeconds(60)));
        BlockingQueue<String> recebidas = new LinkedBlockingQueue<>();
        sessao.subscribe("/topic/chamados", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return String.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                recebidas.add((String) payload);
            }
        });
        Thread.sleep(500);

        long inicio = System.nanoTime();
        eventoTempoRealPort.publicar(mensagem("LUM-2026-009999"));
        String recebida = recebidas.poll(2, TimeUnit.SECONDS);
        long milissegundos = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - inicio);

        assertThat(recebida).contains("\"tipo\":\"CHAMADO_CRIADO\"").contains("\"codigo\":\"LUM-2026-009999\"");
        assertThat(milissegundos).isLessThan(500);
    }

    @Test
    void deveRecusarConexaoSemTicket() {
        assertThatThrownBy(() -> conectar(null)).isInstanceOf(ExecutionException.class);
    }

    @Test
    void deveRecusarOTokenDeSessaoNoLugarDoTicket() {
        Usuario analista = usuarioRepository.findByEmail("analista1@lumen.local").orElseThrow();

        assertThatThrownBy(() -> conectar("Bearer " + jwtService.gerarToken(analista)))
            .isInstanceOf(ExecutionException.class);
    }

    private StompSession conectar(String authorization) throws Exception {
        StompHeaders connect = new StompHeaders();
        if (authorization != null) {
            connect.add("Authorization", authorization);
        }
        WebSocketHttpHeaders handshake = new WebSocketHttpHeaders();
        handshake.setOrigin("http://localhost:4200");
        CompletableFuture<StompSession> futuro = cliente.connectAsync(
            "ws://localhost:" + porta + "/api/v1/ws", handshake, connect, new StompSessionHandlerAdapter() {
            });
        return futuro.get(5, TimeUnit.SECONDS);
    }

    private static EventoTempoRealMessage mensagem(String codigo) {
        ChamadoResumoResponse resumo = new ChamadoResumoResponse(UUID.randomUUID(), codigo, "Impressora parada",
            new UnidadeResumoResponse(UUID.randomUUID(), "U.03 Boa Viagem"), null, CanalOrigem.LIGACAO,
            StatusChamado.ABERTO, null, Prioridade.ALTA, null, Instant.now(), null, Instant.now());
        return new EventoTempoRealMessage(TipoEventoTempoReal.CHAMADO_CRIADO, resumo,
            new ResponsavelResponse(UUID.randomUUID(), "Analista Dev Dois"), Instant.now());
    }
}
