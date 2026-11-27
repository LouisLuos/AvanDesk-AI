package com.avandesk.api.temporeal.port;

import com.avandesk.api.temporeal.dto.EventoTempoRealMessage;

public interface EventoTempoRealPort {

    void publicar(EventoTempoRealMessage mensagem);
}
