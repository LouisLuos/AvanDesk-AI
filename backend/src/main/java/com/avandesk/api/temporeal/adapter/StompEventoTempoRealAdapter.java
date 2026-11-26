package com.avandesk.api.temporeal.adapter;

import com.avandesk.api.temporeal.dto.EventoTempoRealMessage;
import com.avandesk.api.temporeal.port.EventoTempoRealPort;
import com.avandesk.api.temporeal.security.AutenticacaoStompInterceptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class StompEventoTempoRealAdapter implements EventoTempoRealPort {

    private final SimpMessagingTemplate mensageiro;

    @Override
    public void publicar(EventoTempoRealMessage mensagem) {
        try {
            mensageiro.convertAndSend(AutenticacaoStompInterceptor.TOPICO_CHAMADOS, mensagem);
        } catch (MessagingException erro) {
            log.warn("Não foi possível publicar {} do chamado {} em tempo real",
                mensagem.tipo(), mensagem.chamado().codigo(), erro);
        }
    }
}
