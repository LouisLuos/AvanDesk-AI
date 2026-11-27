package com.avandesk.api.temporeal.service;

import com.avandesk.api.chamado.entity.CanalOrigem;
import com.avandesk.api.chamado.entity.Chamado;
import com.avandesk.api.chamado.entity.Prioridade;
import com.avandesk.api.chamado.entity.StatusChamado;
import com.avandesk.api.chamado.entity.TipoEvento;
import com.avandesk.api.chamado.service.EventoHistoricoRegistrado;
import com.avandesk.api.temporeal.dto.EventoTempoRealMessage;
import com.avandesk.api.temporeal.dto.TipoEventoTempoReal;
import com.avandesk.api.temporeal.port.EventoTempoRealPort;
import com.avandesk.api.unidade.entity.Unidade;
import com.avandesk.api.usuario.entity.Usuario;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class NotificadorTempoRealTest {

    private final EventoTempoRealPort port = mock(EventoTempoRealPort.class);
    private final NotificadorTempoReal notificador = new NotificadorTempoReal(port);
    private final TransactionTemplate transacao = new TransactionTemplate(new TransacaoDeTeste());

    private final Usuario analistaUm = usuario("Analista Dev Um");
    private final Usuario analistaDois = usuario("Analista Dev Dois");

    @Test
    void devePublicarUmaMensagemPorChamadoSoDepoisDoCommitComOTipoMaisRelevante() {
        Chamado novo = chamado("LUM-2026-000301", StatusChamado.EM_ATENDIMENTO);
        Chamado outro = chamado("LUM-2026-000302", StatusChamado.EM_TRIAGEM);

        transacao.executeWithoutResult(status -> {
            notificador.aoRegistrarEvento(evento(novo, TipoEvento.CHAMADO_ABERTO, analistaUm, "2026-11-26T13:00:00Z"));
            notificador.aoRegistrarEvento(evento(novo, TipoEvento.REGISTRO_DE_CONTATO_RECEBIDO, analistaUm, "2026-11-26T13:00:00Z"));
            notificador.aoRegistrarEvento(evento(novo, TipoEvento.CHAMADO_ATRIBUIDO, analistaUm, "2026-11-26T13:00:01Z"));
            notificador.aoRegistrarEvento(evento(outro, TipoEvento.PRIORIDADE_DEFINIDA, analistaDois, "2026-11-26T13:00:02Z"));
            verifyNoInteractions(port);
        });

        ArgumentCaptor<EventoTempoRealMessage> mensagens = ArgumentCaptor.forClass(EventoTempoRealMessage.class);
        verify(port, times(2)).publicar(mensagens.capture());
        List<EventoTempoRealMessage> publicadas = mensagens.getAllValues();

        assertThat(publicadas.get(0).tipo()).isEqualTo(TipoEventoTempoReal.CHAMADO_CRIADO);
        assertThat(publicadas.get(0).chamado().codigo()).isEqualTo("LUM-2026-000301");
        assertThat(publicadas.get(0).chamado().status()).isEqualTo(StatusChamado.EM_ATENDIMENTO);
        assertThat(publicadas.get(0).chamado().prioridade()).isEqualTo(Prioridade.ALTA);
        assertThat(publicadas.get(0).chamado().atualizadoEm()).isEqualTo(Instant.parse("2026-11-26T13:00:01Z"));
        assertThat(publicadas.get(0).autor().nome()).isEqualTo("Analista Dev Um");

        assertThat(publicadas.get(1).tipo()).isEqualTo(TipoEventoTempoReal.CHAMADO_ATUALIZADO);
        assertThat(publicadas.get(1).chamado().codigo()).isEqualTo("LUM-2026-000302");
        assertThat(publicadas.get(1).autor().nome()).isEqualTo("Analista Dev Dois");
    }

    @Test
    void naoDevePublicarQuandoATransacaoEDesfeita() {
        Chamado chamado = chamado("LUM-2026-000303", StatusChamado.RESOLVIDO);

        transacao.executeWithoutResult(status -> {
            notificador.aoRegistrarEvento(evento(chamado, TipoEvento.CHAMADO_RESOLVIDO, analistaUm, "2026-11-26T13:00:00Z"));
            status.setRollbackOnly();
        });

        verifyNoInteractions(port);
    }

    @Test
    void naoDeveVazarPendenciasDeUmaTransacaoParaAProxima() {
        Chamado chamado = chamado("LUM-2026-000304", StatusChamado.AGUARDANDO);

        transacao.executeWithoutResult(status -> {
            notificador.aoRegistrarEvento(evento(chamado, TipoEvento.CHAMADO_AGUARDANDO, analistaUm, "2026-11-26T13:00:00Z"));
            status.setRollbackOnly();
        });
        transacao.executeWithoutResult(status ->
            notificador.aoRegistrarEvento(evento(chamado, TipoEvento.RETORNO_RECEBIDO, analistaUm, "2026-11-26T13:05:00Z")));

        ArgumentCaptor<EventoTempoRealMessage> mensagem = ArgumentCaptor.forClass(EventoTempoRealMessage.class);
        verify(port).publicar(mensagem.capture());
        assertThat(mensagem.getValue().tipo()).isEqualTo(TipoEventoTempoReal.STATUS_ALTERADO);
    }

    @Test
    void naoDevePublicarForaDeTransacao() {
        notificador.aoRegistrarEvento(evento(chamado("LUM-2026-000305", StatusChamado.ABERTO),
            TipoEvento.CHAMADO_ABERTO, analistaUm, "2026-11-26T13:00:00Z"));

        verifyNoInteractions(port);
    }

    @Test
    void deveClassificarOsTiposDeEventoDoHistorico() {
        assertThat(TipoEventoTempoReal.de(TipoEvento.CHAMADO_ABERTO)).isEqualTo(TipoEventoTempoReal.CHAMADO_CRIADO);
        assertThat(TipoEventoTempoReal.de(TipoEvento.CHAMADO_ATRIBUIDO)).isEqualTo(TipoEventoTempoReal.CHAMADO_ATRIBUIDO);
        assertThat(TipoEventoTempoReal.de(TipoEvento.CHAMADO_FECHADO)).isEqualTo(TipoEventoTempoReal.STATUS_ALTERADO);
        assertThat(TipoEventoTempoReal.de(TipoEvento.CHAMADO_MESCLADO)).isEqualTo(TipoEventoTempoReal.STATUS_ALTERADO);
        assertThat(TipoEventoTempoReal.de(TipoEvento.REGISTRO_DE_CONTATO_RECEBIDO))
            .isEqualTo(TipoEventoTempoReal.CHAMADO_ATUALIZADO);
    }

    private static EventoHistoricoRegistrado evento(Chamado chamado, TipoEvento tipo, Usuario autor, String quando) {
        return new EventoHistoricoRegistrado(chamado, tipo, autor, Instant.parse(quando));
    }

    private static Chamado chamado(String codigo, StatusChamado status) {
        Unidade unidade = new Unidade();
        unidade.setId(UUID.randomUUID());
        unidade.setNome("U.03 Boa Viagem");
        Chamado chamado = new Chamado();
        chamado.setId(UUID.randomUUID());
        chamado.setCodigo(codigo);
        chamado.setUnidade(unidade);
        chamado.setDescricao("Impressora de etiquetas da coleta parada.");
        chamado.setCanalOrigem(CanalOrigem.LIGACAO);
        chamado.setStatus(status);
        chamado.setPrioridadeSugerida(Prioridade.ALTA);
        chamado.setAbertoEm(Instant.parse("2026-11-26T12:59:00Z"));
        return chamado;
    }

    private static Usuario usuario(String nome) {
        Usuario usuario = new Usuario();
        usuario.setId(UUID.randomUUID());
        usuario.setNome(nome);
        return usuario;
    }

    private static final class TransacaoDeTeste extends AbstractPlatformTransactionManager {

        @Override
        protected Object doGetTransaction() {
            return new Object();
        }

        @Override
        protected void doBegin(Object transaction, TransactionDefinition definition) {
        }

        @Override
        protected void doCommit(DefaultTransactionStatus status) {
        }

        @Override
        protected void doRollback(DefaultTransactionStatus status) {
        }
    }
}
