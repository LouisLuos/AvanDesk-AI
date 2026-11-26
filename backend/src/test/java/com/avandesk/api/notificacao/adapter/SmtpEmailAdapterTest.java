package com.avandesk.api.notificacao.adapter;

import com.avandesk.api.notificacao.config.NotificacaoEmailProperties;
import com.avandesk.api.notificacao.port.MensagemEmail;
import jakarta.mail.Multipart;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SmtpEmailAdapterTest {

    private final JavaMailSender mailSender = mock(JavaMailSender.class);
    private final SmtpEmailAdapter adapter = new SmtpEmailAdapter(mailSender,
        new NotificacaoEmailProperties(true, "nao-responda@lumen.local"));

    @Test
    void deveEnviarComRemetenteDestinatarioAssuntoTextoEHtml() throws Exception {
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage(Session.getInstance(new Properties())));

        adapter.enviar(new MensagemEmail("recepcao.bvg@lumen.local", "[Nexo] Chamado LUM-2026-000123 registrado",
            "Texto do e-mail", "<p>HTML do e-mail</p>"));

        ArgumentCaptor<MimeMessage> enviada = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(enviada.capture());
        MimeMessage mensagem = enviada.getValue();
        assertThat(((InternetAddress) mensagem.getFrom()[0]).getAddress()).isEqualTo("nao-responda@lumen.local");
        assertThat(((InternetAddress) mensagem.getAllRecipients()[0]).getAddress()).isEqualTo("recepcao.bvg@lumen.local");
        assertThat(mensagem.getSubject()).isEqualTo("[Nexo] Chamado LUM-2026-000123 registrado");
        assertThat(mensagem.getContent()).isInstanceOf(Multipart.class);
    }

    @Test
    void deveRepassarAFalhaDoServidorDeEmail() {
        when(mailSender.createMimeMessage()).thenReturn(new MimeMessage(Session.getInstance(new Properties())));
        doThrow(new MailSendException("SMTP indisponível")).when(mailSender).send(any(MimeMessage.class));

        assertThatThrownBy(() -> adapter.enviar(new MensagemEmail("recepcao.bvg@lumen.local", "Assunto", "Texto", null)))
            .isInstanceOf(MailSendException.class);
    }
}
