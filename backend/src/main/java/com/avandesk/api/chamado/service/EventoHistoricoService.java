package com.avandesk.api.chamado.service;

import com.avandesk.api.chamado.entity.Chamado;
import com.avandesk.api.chamado.entity.EventoHistorico;
import com.avandesk.api.chamado.entity.TipoEvento;
import com.avandesk.api.chamado.repository.EventoHistoricoRepository;
import com.avandesk.api.usuario.entity.Usuario;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EventoHistoricoService {

    /**
     * Ordem da linha do tempo: pela data e, no empate, pela ordem do {@link TipoEvento}.
     * Eventos gravados juntos (ex.: CHAMADO_ABERTO e REGISTRO_DE_CONTATO_RECEBIDO no registro)
     * podem cair no mesmo microssegundo, e o banco não guarda nada menor que isso.
     */
    private static final Comparator<EventoHistorico> ORDEM_DA_LINHA_DO_TEMPO =
        Comparator.comparing(EventoHistorico::getOcorridoEm).thenComparing(EventoHistorico::getTipo);

    private final EventoHistoricoRepository eventoHistoricoRepository;

    @Transactional(propagation = Propagation.MANDATORY)
    public EventoHistorico registrar(Chamado chamado, TipoEvento tipo, Usuario autor, Map<String, Object> dados) {
        EventoHistorico evento = new EventoHistorico(
            chamado, tipo, Instant.now(), autor, dados, chamado.getOrigemDado());
        return eventoHistoricoRepository.save(evento);
    }

    /** Linha do tempo do chamado em ordem cronológica (do mais antigo para o mais novo). */
    @Transactional(readOnly = true)
    public List<EventoHistorico> listarDoChamado(UUID chamadoId) {
        return eventoHistoricoRepository.findByChamadoIdOrderByOcorridoEmAsc(chamadoId).stream()
            .sorted(ORDEM_DA_LINHA_DO_TEMPO)
            .toList();
    }
}
