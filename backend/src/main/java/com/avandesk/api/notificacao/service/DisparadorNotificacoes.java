package com.avandesk.api.notificacao.service;

import com.avandesk.api.notificacao.config.NotificacaoConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class DisparadorNotificacoes {

    private final EntregaNotificacaoService entregaNotificacaoService;

    @Async(NotificacaoConfig.EXECUTOR_DE_NOTIFICACOES)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void aoEnfileirar(NotificacaoEnfileirada evento) {
        entregaNotificacaoService.enviarAgora(evento.notificacaoId());
    }
}
