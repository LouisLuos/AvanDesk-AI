package com.avandesk.api.notificacao.service;

import com.avandesk.api.notificacao.port.MensagemEmail;
import com.avandesk.api.notificacao.port.NotificacaoPort;
import com.avandesk.api.usuario.config.SolicitanteProperties;
import com.avandesk.api.usuario.service.VerificacaoEmailSolicitada;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.MailSendException;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class EnvioVerificacaoEmailTest {

    private static final String TOKEN = "Abc_123-xyz";

    private final NotificacaoPort notificacaoPort = mock(NotificacaoPort.class);
    private final EnvioVerificacaoEmail envio = new EnvioVerificacaoEmail(notificacaoPort, new MotorDeModelosEmail(),
        new SolicitanteProperties(false, List.of(), Duration.ofHours(24), Duration.ofSeconds(60),
            "http://localhost:4200/verificar-email"));
    private final VerificacaoEmailSolicitada pedido = new VerificacaoEmailSolicitada(UUID.randomUUID(),
        "Mariana <Alves>", "mariana.alves@gmail.com", TOKEN, Instant.parse("2026-11-27T18:00:00Z"));

    @Test
    void deveEnviarOLinkComOTokenEAValidade() {
        envio.aoSolicitarVerificacao(pedido);

        ArgumentCaptor<MensagemEmail> mensagem = ArgumentCaptor.forClass(MensagemEmail.class);
        verify(notificacaoPort).enviar(mensagem.capture());
        String link = "http://localhost:4200/verificar-email?token=" + TOKEN;
        assertThat(mensagem.getValue().destinatario()).isEqualTo("mariana.alves@gmail.com");
        assertThat(mensagem.getValue().assunto()).isEqualTo("[Nexo] Confirme o seu e-mail");
        assertThat(mensagem.getValue().texto()).contains(link, "27/11/2026 às 15:00", "só pode ser usado uma vez");
        assertThat(mensagem.getValue().html()).contains("href=\"" + link + "\"", "Mariana &lt;Alves&gt;")
            .doesNotContain("<Alves>");
    }

    @Test
    void naoDevePropagarFalhaDoServidorDeEmail() {
        doThrow(new MailSendException("SMTP indisponível")).when(notificacaoPort).enviar(any());

        assertThatCode(() -> envio.aoSolicitarVerificacao(pedido)).doesNotThrowAnyException();
    }
}
