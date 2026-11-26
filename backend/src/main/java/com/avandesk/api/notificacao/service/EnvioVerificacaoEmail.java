package com.avandesk.api.notificacao.service;

import com.avandesk.api.notificacao.config.NotificacaoConfig;
import com.avandesk.api.notificacao.port.MensagemEmail;
import com.avandesk.api.notificacao.port.NotificacaoPort;
import com.avandesk.api.usuario.config.SolicitanteProperties;
import com.avandesk.api.usuario.service.VerificacaoEmailSolicitada;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class EnvioVerificacaoEmail {

    private static final DateTimeFormatter DATA_E_HORA =
        DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm", MotorDeModelosEmail.PORTUGUES);
    private static final ZoneId FUSO = ZoneId.of("America/Recife");

    private final NotificacaoPort notificacaoPort;
    private final MotorDeModelosEmail motor;
    private final SolicitanteProperties propriedades;

    @Async(NotificacaoConfig.EXECUTOR_DE_NOTIFICACOES)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void aoSolicitarVerificacao(VerificacaoEmailSolicitada solicitacao) {
        try {
            notificacaoPort.enviar(montar(solicitacao));
        } catch (RuntimeException erro) {
            log.warn("Falha ao enviar o e-mail de verificação do usuário {}: {}: {}", solicitacao.usuarioId(),
                erro.getClass().getSimpleName(), erro.getMessage());
        }
    }

    MensagemEmail montar(VerificacaoEmailSolicitada solicitacao) {
        String link = UriComponentsBuilder.fromUriString(propriedades.urlVerificacao())
            .queryParam("token", solicitacao.token())
            .build()
            .toUriString();
        Map<String, Object> variaveis = new HashMap<>();
        variaveis.put("nome", solicitacao.nome());
        variaveis.put("link", link);
        variaveis.put("validoAte", DATA_E_HORA.format(solicitacao.expiraEm().atZone(FUSO)));
        return new MensagemEmail(solicitacao.email(), "[Nexo] Confirme o seu e-mail",
            motor.processar("email/verificacao.txt", variaveis), motor.processar("email/verificacao.html", variaveis));
    }
}
