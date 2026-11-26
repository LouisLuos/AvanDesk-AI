package com.avandesk.api.chamado.service;

import com.avandesk.api.chamado.entity.Solicitante;
import com.avandesk.api.chamado.repository.SolicitanteRepository;
import com.avandesk.api.core.exception.RegraNegocioException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
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
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class SolicitanteServiceTest {

    @Mock
    private SolicitanteRepository solicitanteRepository;

    @InjectMocks
    private SolicitanteService solicitanteService;

    @Test
    void deveCriarSolicitanteComEmailEmMinusculasETelefoneEmE164() {
        when(solicitanteRepository.findByEmail("recepcao.bvg@lumen.local")).thenReturn(Optional.empty());
        when(solicitanteRepository.save(any(Solicitante.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

        Solicitante criado = solicitanteService.identificarOuCriar(
            "  Recepção Boa Viagem ", "  Recepcao.BVG@Lumen.local ", "(81) 99999-9999");

        assertEquals("Recepção Boa Viagem", criado.getNome());
        assertEquals("recepcao.bvg@lumen.local", criado.getEmail());
        assertEquals("+5581999999999", criado.getTelefone());
    }

    @Test
    void deveCriarSolicitanteSemTelefoneQuandoNaoInformado() {
        when(solicitanteRepository.findByEmail("coleta@lumen.local")).thenReturn(Optional.empty());
        when(solicitanteRepository.save(any(Solicitante.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

        Solicitante criado = solicitanteService.identificarOuCriar(null, "coleta@lumen.local", "   ");

        assertNull(criado.getNome());
        assertNull(criado.getTelefone());
    }

    @Test
    void deveManterDdiQuandoTelefoneJaVemCompleto() {
        when(solicitanteRepository.findByEmail("coleta@lumen.local")).thenReturn(Optional.empty());
        when(solicitanteRepository.save(any(Solicitante.class))).thenAnswer(invocacao -> invocacao.getArgument(0));

        Solicitante criado = solicitanteService.identificarOuCriar(null, "coleta@lumen.local", "+55 81 99999-9999");

        assertEquals("+5581999999999", criado.getTelefone());
    }

    @Test
    void deveReaproveitarSolicitantePeloEmailECompletarOQueFalta() {
        Solicitante existente = new Solicitante();
        existente.setEmail("recepcao.bvg@lumen.local");
        when(solicitanteRepository.findByEmail("recepcao.bvg@lumen.local")).thenReturn(Optional.of(existente));

        Solicitante encontrado = solicitanteService.identificarOuCriar(
            "Recepção", "Recepcao.BVG@lumen.local", "81999999999");

        assertSame(existente, encontrado);
        assertEquals("Recepção", encontrado.getNome());
        assertEquals("+5581999999999", encontrado.getTelefone());
        verify(solicitanteRepository, never()).save(any());
    }

    @Test
    void naoDeveSobrescreverNomeNemTelefoneDoSolicitanteExistente() {
        Solicitante existente = new Solicitante();
        existente.setNome("Recepção Boa Viagem");
        existente.setTelefone("+5581988887777");
        existente.setEmail("recepcao.bvg@lumen.local");
        when(solicitanteRepository.findByEmail("recepcao.bvg@lumen.local")).thenReturn(Optional.of(existente));

        solicitanteService.identificarOuCriar("Outro Nome", "recepcao.bvg@lumen.local", "81999999999");

        assertEquals("Recepção Boa Viagem", existente.getNome());
        assertEquals("+5581988887777", existente.getTelefone());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "recepcao@lumen", "recepcao.lumen.local", "recep cao@lumen.local", "@lumen.local"})
    void deveRecusarEmailAusenteOuInvalido(String email) {
        RegraNegocioException erro = assertThrows(RegraNegocioException.class,
            () -> solicitanteService.identificarOuCriar("Recepção", email, null));

        assertEquals("Informe um e-mail válido para o solicitante (ex.: recepcao@lumen.com.br).", erro.getMessage());
        verifyNoInteractions(solicitanteRepository);
    }

    @ParameterizedTest
    @ValueSource(strings = {"9999-9999", "abc", "+55 81 99999-9999 12345"})
    void deveRecusarTelefoneInvalido(String telefone) {
        assertThrows(RegraNegocioException.class,
            () -> solicitanteService.identificarOuCriar(null, "coleta@lumen.local", telefone));

        verifyNoInteractions(solicitanteRepository);
    }
}
