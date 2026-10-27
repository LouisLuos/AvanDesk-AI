package com.avandesk.api.service;

import com.avandesk.api.entity.Solicitante;
import com.avandesk.api.exception.RegraNegocioException;
import com.avandesk.api.repository.SolicitanteRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SolicitanteServiceTest {

    @Mock
    private SolicitanteRepository solicitanteRepository;

    @InjectMocks
    private SolicitanteService solicitanteService;

    // ---------- e-mail ----------

    @Test
    void deveReaproveitarSolicitanteExistentePeloEmailEmMinusculas() {
        Solicitante existente = new Solicitante();
        when(solicitanteRepository.findByEmail("recepcao.bvg@lumen.local")).thenReturn(Optional.of(existente));

        Solicitante encontrado = solicitanteService.identificarOuCriar("Recepção", "  Recepcao.BVG@Lumen.local ");

        assertSame(existente, encontrado);
        verify(solicitanteRepository, never()).save(any());
    }

    @Test
    void deveCriarSolicitantePeloEmailQuandoNaoExiste() {
        when(solicitanteRepository.findByEmail("recepcao.bvg@lumen.local")).thenReturn(Optional.empty());
        when(solicitanteRepository.save(any(Solicitante.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

        Solicitante criado = solicitanteService.identificarOuCriar("  Recepção Boa Viagem ", "recepcao.bvg@lumen.local");

        assertEquals("Recepção Boa Viagem", criado.getNome());
        assertEquals("recepcao.bvg@lumen.local", criado.getEmail());
        assertNull(criado.getTelefone());
    }

    @Test
    void deveRecusarEmailInvalido() {
        RegraNegocioException erro = assertThrows(RegraNegocioException.class,
            () -> solicitanteService.identificarOuCriar(null, "recepcao@lumen"));

        assertEquals("Contato deve ser um telefone no formato E.164 (ex.: +5581999999999) ou um e-mail.",
            erro.getMessage());
        verify(solicitanteRepository, never()).save(any());
    }

    // ---------- telefone ----------

    @Test
    void deveAcrescentar55QuandoTelefoneVemSemDdi() {
        when(solicitanteRepository.findFirstByTelefoneAndNomeIsNull("+5581999999999")).thenReturn(Optional.empty());
        when(solicitanteRepository.save(any(Solicitante.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

        Solicitante criado = solicitanteService.identificarOuCriar("  ", "(81) 99999-9999");

        assertEquals("+5581999999999", criado.getTelefone());
        assertNull(criado.getNome());
        assertNull(criado.getEmail());
    }

    @Test
    void deveManterDdiQuandoTelefoneJaVemCompleto() {
        when(solicitanteRepository.findFirstByTelefoneAndNomeIsNull("+5581999999999")).thenReturn(Optional.empty());
        when(solicitanteRepository.save(any(Solicitante.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

        Solicitante criado = solicitanteService.identificarOuCriar(null, "+55 81 99999-9999");

        assertEquals("+5581999999999", criado.getTelefone());
    }

    @Test
    void deveReaproveitarSolicitantePeloTelefoneENome() {
        // Um telefone pode ser de mais de um solicitante (aparelho compartilhado, RN13): o nome desempata.
        Solicitante existente = new Solicitante();
        when(solicitanteRepository.findFirstByTelefoneAndNomeIgnoreCase("+5581999999999", "Recepção"))
            .thenReturn(Optional.of(existente));

        Solicitante encontrado = solicitanteService.identificarOuCriar("Recepção", "81999999999");

        assertSame(existente, encontrado);
        verify(solicitanteRepository, never()).save(any());
    }

    @Test
    void deveCriarNovoSolicitanteQuandoTelefoneJaExisteComOutroNome() {
        when(solicitanteRepository.findFirstByTelefoneAndNomeIgnoreCase("+5581999999999", "Coleta"))
            .thenReturn(Optional.empty());
        when(solicitanteRepository.save(any(Solicitante.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

        solicitanteService.identificarOuCriar("Coleta", "81999999999");

        ArgumentCaptor<Solicitante> salvo = ArgumentCaptor.forClass(Solicitante.class);
        verify(solicitanteRepository).save(salvo.capture());
        assertEquals("Coleta", salvo.getValue().getNome());
        assertEquals("+5581999999999", salvo.getValue().getTelefone());
    }

    @Test
    void deveRecusarTelefoneComPoucosDigitos() {
        assertThrows(RegraNegocioException.class, () -> solicitanteService.identificarOuCriar(null, "9999-9999"));

        verify(solicitanteRepository, never()).save(any());
    }

    @Test
    void deveRecusarContatoQueNaoEEmailNemTelefone() {
        assertThrows(RegraNegocioException.class, () -> solicitanteService.identificarOuCriar(null, "abc"));
    }
}
