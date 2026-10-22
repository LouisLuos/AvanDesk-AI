package com.avandesk.api.service;

import com.avandesk.api.entity.Chamado;
import com.avandesk.api.entity.EventoHistorico;
import com.avandesk.api.entity.TipoEvento;
import com.avandesk.api.entity.Usuario;
import com.avandesk.api.repository.EventoHistoricoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class EventoHistoricoService {

    private final EventoHistoricoRepository eventoHistoricoRepository;

    @Transactional(propagation = Propagation.MANDATORY)
    public EventoHistorico registrar(Chamado chamado, TipoEvento tipo, Usuario autor, Map<String, Object> dados) {
        EventoHistorico evento = new EventoHistorico(
            chamado, tipo, Instant.now(), autor, dados, chamado.getOrigemDado());
        return eventoHistoricoRepository.save(evento);
    }
}
