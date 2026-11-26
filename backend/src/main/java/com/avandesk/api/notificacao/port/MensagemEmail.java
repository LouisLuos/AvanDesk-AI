package com.avandesk.api.notificacao.port;

public record MensagemEmail(String destinatario, String assunto, String texto, String html) {
}
