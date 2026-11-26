package com.avandesk.api.notificacao.adapter;

import com.avandesk.api.notificacao.config.NotificacaoEmailProperties;
import com.avandesk.api.notificacao.port.MensagemEmail;
import com.avandesk.api.notificacao.port.NotificacaoPort;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.MailPreparationException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class SmtpEmailAdapter implements NotificacaoPort {

    private final JavaMailSender mailSender;
    private final NotificacaoEmailProperties propriedades;

    @Override
    public void enviar(MensagemEmail mensagem) {
        MimeMessage mime = mailSender.createMimeMessage();
        try {
            MimeMessageHelper montador = new MimeMessageHelper(mime, mensagem.html() != null, StandardCharsets.UTF_8.name());
            montador.setFrom(propriedades.remetente());
            montador.setTo(mensagem.destinatario());
            montador.setSubject(mensagem.assunto());
            if (mensagem.html() == null) {
                montador.setText(mensagem.texto());
            } else {
                montador.setText(mensagem.texto(), mensagem.html());
            }
        } catch (MessagingException erro) {
            throw new MailPreparationException("Não foi possível montar o e-mail de notificação.", erro);
        }
        mailSender.send(mime);
    }
}
