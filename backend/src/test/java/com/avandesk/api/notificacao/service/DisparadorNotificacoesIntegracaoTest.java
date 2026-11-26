package com.avandesk.api.notificacao.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

@SpringBootTest
class DisparadorNotificacoesIntegracaoTest {

    @MockitoBean
    private EntregaNotificacaoService entregaNotificacaoService;

    @Autowired
    private ApplicationEventPublisher publicadorDeEventos;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    void deveEntregarEmOutraThreadSoDepoisDoCommit() {
        UUID notificacaoId = UUID.randomUUID();
        AtomicReference<String> threadDaEntrega = new AtomicReference<>();
        doAnswer(invocacao -> {
            threadDaEntrega.set(Thread.currentThread().getName());
            return null;
        }).when(entregaNotificacaoService).enviarAgora(notificacaoId);

        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            publicadorDeEventos.publishEvent(new NotificacaoEnfileirada(notificacaoId));
            verify(entregaNotificacaoService, after(300).never()).enviarAgora(any());
        });

        verify(entregaNotificacaoService, timeout(3000)).enviarAgora(notificacaoId);
        assertThat(threadDaEntrega.get()).startsWith("notificacao-");
    }

    @Test
    void naoDeveEntregarQuandoATransacaoEDesfeita() {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            status.setRollbackOnly();
            publicadorDeEventos.publishEvent(new NotificacaoEnfileirada(UUID.randomUUID()));
        });

        verify(entregaNotificacaoService, after(500).never()).enviarAgora(any());
    }
}
