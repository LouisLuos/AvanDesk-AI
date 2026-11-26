package com.avandesk.api.temporeal.service;

import com.avandesk.api.chamado.dto.ChamadoResumoResponse;
import com.avandesk.api.chamado.dto.ResponsavelResponse;
import com.avandesk.api.chamado.service.EventoHistoricoRegistrado;
import com.avandesk.api.temporeal.dto.EventoTempoRealMessage;
import com.avandesk.api.temporeal.dto.TipoEventoTempoReal;
import com.avandesk.api.temporeal.port.EventoTempoRealPort;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class NotificadorTempoReal {

    private final EventoTempoRealPort eventoTempoRealPort;

    @EventListener
    public void aoRegistrarEvento(EventoHistoricoRegistrado evento) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        PendentesDaTransacao pendentes = pendentesDaTransacao();
        pendentes.registrar(evento);
    }

    private PendentesDaTransacao pendentesDaTransacao() {
        Object existente = TransactionSynchronizationManager.getResource(this);
        if (existente instanceof PendentesDaTransacao pendentes) {
            return pendentes;
        }
        PendentesDaTransacao pendentes = new PendentesDaTransacao();
        TransactionSynchronizationManager.bindResource(this, pendentes);
        TransactionSynchronizationManager.registerSynchronization(pendentes);
        return pendentes;
    }

    private final class PendentesDaTransacao implements TransactionSynchronization {

        private final Map<UUID, Pendente> porChamado = new LinkedHashMap<>();
        private final List<EventoTempoRealMessage> prontas = new ArrayList<>();

        void registrar(EventoHistoricoRegistrado evento) {
            TipoEventoTempoReal tipo = TipoEventoTempoReal.de(evento.tipo());
            porChamado.merge(evento.chamado().getId(), new Pendente(evento, tipo),
                (atual, novo) -> new Pendente(novo.evento(), atual.tipo().maisRelevante(novo.tipo())));
        }

        @Override
        public void beforeCommit(boolean readOnly) {
            porChamado.values().forEach(pendente -> {
                EventoHistoricoRegistrado evento = pendente.evento();
                Instant ocorridoEm = evento.ocorridoEm();
                prontas.add(new EventoTempoRealMessage(
                    pendente.tipo(),
                    ChamadoResumoResponse.de(evento.chamado(), ocorridoEm),
                    ResponsavelResponse.de(evento.autor()),
                    ocorridoEm));
            });
        }

        @Override
        public void afterCommit() {
            prontas.forEach(eventoTempoRealPort::publicar);
        }

        @Override
        public void afterCompletion(int status) {
            TransactionSynchronizationManager.unbindResourceIfPossible(NotificadorTempoReal.this);
        }
    }

    private record Pendente(EventoHistoricoRegistrado evento, TipoEventoTempoReal tipo) {
    }
}
