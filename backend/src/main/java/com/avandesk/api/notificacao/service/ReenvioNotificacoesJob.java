package com.avandesk.api.notificacao.service;

import com.avandesk.api.notificacao.config.NotificacaoEmailProperties;
import com.avandesk.api.notificacao.entity.SituacaoNotificacao;
import com.avandesk.api.notificacao.repository.NotificacaoEmailRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.EnumSet;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReenvioNotificacoesJob {

    public static final int TAMANHO_DO_LOTE = 50;

    private final NotificacaoEmailRepository notificacaoEmailRepository;
    private final EntregaNotificacaoService entregaNotificacaoService;
    private final NotificacaoEmailProperties propriedades;

    @Scheduled(initialDelayString = "${nexo.notificacoes.email.intervalo-reenvio:PT1M}",
        fixedDelayString = "${nexo.notificacoes.email.intervalo-reenvio:PT1M}")
    public void reenviarVencidas() {
        if (!propriedades.habilitado()) {
            return;
        }
        for (UUID id : notificacaoEmailRepository.vencidas(
            EnumSet.of(SituacaoNotificacao.PENDENTE, SituacaoNotificacao.FALHOU), Instant.now(),
            PageRequest.of(0, TAMANHO_DO_LOTE))) {
            try {
                entregaNotificacaoService.reenviarSeVencida(id);
            } catch (RuntimeException erro) {
                log.error("Erro ao reenviar a notificação {}", id, erro);
            }
        }
    }
}
