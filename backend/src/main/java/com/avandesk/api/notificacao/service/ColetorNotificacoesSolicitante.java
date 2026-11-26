package com.avandesk.api.notificacao.service;

import com.avandesk.api.chamado.entity.Chamado;
import com.avandesk.api.chamado.entity.OrigemDado;
import com.avandesk.api.chamado.entity.StatusChamado;
import com.avandesk.api.chamado.entity.TipoEvento;
import com.avandesk.api.chamado.service.EventoHistoricoRegistrado;
import com.avandesk.api.notificacao.config.NotificacaoEmailProperties;
import com.avandesk.api.notificacao.entity.NotificacaoEmail;
import com.avandesk.api.notificacao.repository.NotificacaoEmailRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ColetorNotificacoesSolicitante {

    public static final Set<StatusChamado> STATUS_NOTIFICADOS = EnumSet.of(
        StatusChamado.EM_TRIAGEM, StatusChamado.EM_ATENDIMENTO, StatusChamado.AGUARDANDO, StatusChamado.RESOLVIDO);
    public static final Duration CARENCIA_DO_ENVIO_IMEDIATO = Duration.ofMinutes(2);

    private final NotificacaoEmailRepository notificacaoEmailRepository;
    private final ApplicationEventPublisher publicadorDeEventos;
    private final NotificacaoEmailProperties propriedades;

    @EventListener
    public void aoRegistrarEvento(EventoHistoricoRegistrado evento) {
        boolean interessa = evento.tipo() == TipoEvento.CHAMADO_ABERTO
            || TipoEvento.MUDANCAS_DE_STATUS.contains(evento.tipo());
        if (!propriedades.habilitado() || !interessa || !TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        Coleta coleta = (Coleta) TransactionSynchronizationManager.getResource(this);
        if (coleta == null) {
            coleta = new Coleta();
            TransactionSynchronizationManager.bindResource(this, coleta);
            TransactionSynchronizationManager.registerSynchronization(coleta);
        }
        coleta.chamados.putIfAbsent(evento.chamado().getId(), evento.chamado());
    }

    private void enfileirar(Chamado chamado) {
        if (chamado.getOrigemDado() == OrigemDado.SIMULADO || chamado.getSolicitante().getEmail() == null) {
            return;
        }
        Instant agora = Instant.now();
        Instant reenvioSeNaoSair = agora.plus(CARENCIA_DO_ENVIO_IMEDIATO);
        StatusChamado anterior = chamado.getStatusAoCarregar();
        NotificacaoEmail notificacao;
        if (anterior == null) {
            notificacao = NotificacaoEmail.deAbertura(chamado, agora, reenvioSeNaoSair);
        } else if (anterior != chamado.getStatus() && STATUS_NOTIFICADOS.contains(chamado.getStatus())) {
            notificacao = NotificacaoEmail.deMudancaDeStatus(chamado, anterior, agora, reenvioSeNaoSair);
        } else {
            return;
        }
        notificacaoEmailRepository.save(notificacao);
        publicadorDeEventos.publishEvent(new NotificacaoEnfileirada(notificacao.getId()));
    }

    private final class Coleta implements TransactionSynchronization {

        private final Map<UUID, Chamado> chamados = new LinkedHashMap<>();

        @Override
        public void beforeCommit(boolean somenteLeitura) {
            chamados.values().forEach(ColetorNotificacoesSolicitante.this::enfileirar);
        }

        @Override
        public void afterCompletion(int situacao) {
            TransactionSynchronizationManager.unbindResourceIfPossible(ColetorNotificacoesSolicitante.this);
        }
    }
}
