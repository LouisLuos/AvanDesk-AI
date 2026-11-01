package com.avandesk.api.chamado.service;

import com.avandesk.api.chamado.entity.MotivoEspera;
import com.avandesk.api.chamado.entity.Prioridade;
import com.avandesk.api.sla.domain.CalculadoraPrioridadeMatriz3D;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Testes de Domínio: Regras Oficiais de SLA e Prioridade (Sarah Cyrne)")
class RegrasOficiaisSlaTest {

    private CalculadoraPrioridadeMatriz3D calculadora;

    @BeforeEach
    void setUp() {
        calculadora = new CalculadoraPrioridadeMatriz3D();
    }

    @Nested
    @DisplayName("3.1 Matriz 3D Oficial (HU04 / Seção 3: Soma S + U + I)")
    class Matriz3DTest {

        @ParameterizedTest(name = "Soma {0}+{1}+{2} = Baixa (P3)")
        @CsvSource({
            "1, 1, 1", // Soma 3
            "1, 2, 1", // Soma 4
            "1, 1, 3", // Soma 5
            "2, 2, 1", // Soma 5 (limite superior P3)
            "3, 1, 1"  // Soma 5
        })
        void deveSugerirPrioridadeBaixaParaSomaAte5(int s, int u, int i) {
            assertEquals(Prioridade.BAIXA, calculadora.calcular(s, u, i));
        }

        @ParameterizedTest(name = "Soma {0}+{1}+{2} = Média (P2)")
        @CsvSource({
            "2, 2, 2", // Soma 6 (limite inferior P2)
            "3, 2, 1", // Soma 6
            "1, 3, 2", // Soma 6
            "3, 2, 2", // Soma 7 (limite superior P2)
            "2, 3, 2"  // Soma 7
        })
        void deveSugerirPrioridadeMediaParaSoma6e7(int s, int u, int i) {
            assertEquals(Prioridade.MEDIA, calculadora.calcular(s, u, i));
        }

        @ParameterizedTest(name = "Soma {0}+{1}+{2} = Alta (P1)")
        @CsvSource({
            "3, 3, 2", // Soma 8 (limite inferior P1)
            "2, 3, 3", // Soma 8
            "3, 3, 3"  // Soma 9 (máximo P1)
        })
        void deveSugerirPrioridadeAltaParaSoma8e9(int s, int u, int i) {
            assertEquals(Prioridade.ALTA, calculadora.calcular(s, u, i));
        }

        @Test
        @DisplayName("Deve rejeitar dimensões menores que 1 ou maiores que 3")
        void deveRejeitarDimensoesForaDoRange() {
            assertThrows(IllegalArgumentException.class, () -> calculadora.calcular(0, 2, 2));
            assertThrows(IllegalArgumentException.class, () -> calculadora.calcular(4, 2, 2));
            assertThrows(IllegalArgumentException.class, () -> calculadora.calcular(2, -1, 2));
            assertThrows(IllegalArgumentException.class, () -> calculadora.calcular(2, 2, 5));
        }
    }

    @Nested
    @DisplayName("3.2 Regras do Relógio de SLA (Seção 5)")
    class RelogioSlaTest {

        @Test
        @DisplayName("Apenas SOLICITANTE deve pausar o relógio de SLA")
        void devePausarApenasAguardandoSolicitante() {
            assertTrue(MotivoEspera.SOLICITANTE.isPausaRelogioSla(), "SOLICITANTE deve pausar o relógio de SLA");
            assertFalse(MotivoEspera.FORNECEDOR.isPausaRelogioSla(), "FORNECEDOR não pode pausar o relógio (RN07)");
            assertFalse(MotivoEspera.PECA_EQUIPAMENTO.isPausaRelogioSla(), "PECA_EQUIPAMENTO não pausa o relógio");
        }
    }

    @Nested
    @DisplayName("3.3 Regras de Mesclagem / Vínculo (RN17 / Seção 5)")
    class MesclagemPrioridadeTest {

        @Test
        @DisplayName("Chamado principal deve assumir a maior prioridade entre os dois (RN17)")
        void deveHerdarMaiorPrioridade() {
            assertEquals(Prioridade.ALTA, Prioridade.maiorPrioridade(Prioridade.BAIXA, Prioridade.ALTA));
            assertEquals(Prioridade.ALTA, Prioridade.maiorPrioridade(Prioridade.ALTA, Prioridade.MEDIA));
            assertEquals(Prioridade.MEDIA, Prioridade.maiorPrioridade(Prioridade.MEDIA, Prioridade.BAIXA));
            assertEquals(Prioridade.BAIXA, Prioridade.maiorPrioridade(Prioridade.BAIXA, Prioridade.BAIXA));
            assertEquals(Prioridade.MEDIA, Prioridade.maiorPrioridade(null, Prioridade.MEDIA));
            assertEquals(Prioridade.ALTA, Prioridade.maiorPrioridade(Prioridade.ALTA, null));
        }
    }
}
