package com.avandesk.api.service;

import com.avandesk.api.entity.Chamado;
import com.avandesk.api.entity.EventoHistorico;
import com.avandesk.api.entity.OrigemDado;
import com.avandesk.api.entity.TipoEvento;
import com.avandesk.api.entity.Usuario;
import com.avandesk.api.repository.EventoHistoricoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Sobe um contexto Spring mínimo, com gerenciador de transação de verdade, para provar que o
 * registro de evento exige uma transação já aberta (propagation = MANDATORY). Não usa banco.
 */
@SpringJUnitConfig
class EventoHistoricoServiceTest {

    @Configuration
    @EnableTransactionManagement
    static class Config {

        @Bean
        DataSource dataSource() throws SQLException {
            DataSource dataSource = mock(DataSource.class);
            when(dataSource.getConnection()).thenReturn(mock(Connection.class));
            return dataSource;
        }

        @Bean
        PlatformTransactionManager transactionManager(DataSource dataSource) {
            return new DataSourceTransactionManager(dataSource);
        }

        @Bean
        EventoHistoricoRepository eventoHistoricoRepository() {
            EventoHistoricoRepository repository = mock(EventoHistoricoRepository.class);
            when(repository.save(any(EventoHistorico.class))).thenAnswer(invocacao -> invocacao.getArgument(0));
            return repository;
        }

        @Bean
        EventoHistoricoService eventoHistoricoService(EventoHistoricoRepository repository) {
            return new EventoHistoricoService(repository);
        }
    }

    @Autowired
    private EventoHistoricoService eventoHistoricoService;

    @Autowired
    private EventoHistoricoRepository eventoHistoricoRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private final Chamado chamado = new Chamado();
    private final Usuario autor = new Usuario();

    @BeforeEach
    void configurar() {
        clearInvocations(eventoHistoricoRepository);
        chamado.setOrigemDado(OrigemDado.SIMULADO);
    }

    @Test
    void deveRegistrarEventoHerdandoAOrigemDoChamado() {
        Instant antes = Instant.now();

        EventoHistorico evento = new TransactionTemplate(transactionManager).execute(status ->
            eventoHistoricoService.registrar(chamado, TipoEvento.CHAMADO_ABERTO, autor, Map.of("canal", "LIGACAO")));

        assertSame(chamado, evento.getChamado());
        assertEquals(TipoEvento.CHAMADO_ABERTO, evento.getTipo());
        assertSame(autor, evento.getAutor());
        assertEquals(OrigemDado.SIMULADO, evento.getOrigemDado());
        assertEquals(Map.of("canal", "LIGACAO"), evento.getDados());
        assertFalse(evento.getOcorridoEm().isBefore(antes));
        assertTrue(evento.getOcorridoEm().toString().endsWith("Z"));
        verify(eventoHistoricoRepository).save(evento);
    }

    @Test
    void deveRecusarRegistroForaDeUmaTransacao() {
        assertThrows(IllegalTransactionStateException.class,
            () -> eventoHistoricoService.registrar(chamado, TipoEvento.CHAMADO_ABERTO, autor, Map.of()));

        verify(eventoHistoricoRepository, never()).save(any());
    }
}
