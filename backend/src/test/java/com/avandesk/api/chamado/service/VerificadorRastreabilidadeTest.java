package com.avandesk.api.chamado.service;

import com.avandesk.api.catalogo.entity.Categoria;
import com.avandesk.api.chamado.dto.ItemVerificacaoResponse;
import com.avandesk.api.chamado.dto.VerificacaoEncerramentoResponse;
import com.avandesk.api.chamado.entity.Chamado;
import com.avandesk.api.chamado.entity.StatusChamado;
import com.avandesk.api.unidade.entity.Unidade;
import com.avandesk.api.usuario.entity.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VerificadorRastreabilidadeTest {

    private final VerificadorRastreabilidade verificador = new VerificadorRastreabilidade();
    private Chamado chamado;

    @BeforeEach
    void chamadoCompleto() {
        chamado = new Chamado();
        chamado.setStatus(StatusChamado.RESOLVIDO);
        chamado.setDescricao("Impressora de etiquetas da coleta parou de imprimir.");
        chamado.setCategoria(new Categoria());
        chamado.setUnidade(new Unidade());
        chamado.setResponsavel(new Usuario());
        chamado.setCausaRaiz("Cabo de energia danificado");
        chamado.setResolucao("Cabo trocado e impressora testada com etiqueta de amostra.");
    }

    @Test
    void deveAprovarChamadoComOsSeisItens() {
        VerificacaoEncerramentoResponse resultado = verificador.verificar(chamado, true, null);

        assertTrue(resultado.aprovado());
        assertEquals(6, resultado.itensConformes());
        assertEquals(6, resultado.totalItens());
        assertEquals(List.of("PROBLEMA_E_CATEGORIA", "UNIDADE", "RESPONSAVEL", "CONTATO",
            "CAUSA_RAIZ_OU_JUSTIFICATIVA", "RESOLUCAO"), resultado.itens().stream().map(ItemVerificacaoResponse::codigo).toList());
        assertTrue(resultado.itens().stream().allMatch(item -> item.pendencia() == null));
        assertTrue(resultado.pendencias().isEmpty());
    }

    @Test
    void deveApontarCategoriaAusente() {
        chamado.setCategoria(null);

        ItemVerificacaoResponse item = item(verificador.verificar(chamado, true, null), "PROBLEMA_E_CATEGORIA");

        assertFalse(item.ok());
        assertEquals("Informe a categoria do chamado.", item.pendencia());
    }

    @Test
    void deveApontarProblemaSemDescricaoSuficiente() {
        chamado.setDescricao("Quebrou");

        ItemVerificacaoResponse item = item(verificador.verificar(chamado, true, null), "PROBLEMA_E_CATEGORIA");

        assertFalse(item.ok());
        assertEquals("Descreva o problema com pelo menos 10 caracteres.", item.pendencia());
    }

    @Test
    void deveApontarUnidadeResponsavelEContatoAusentes() {
        chamado.setUnidade(null);
        chamado.setResponsavel(null);

        VerificacaoEncerramentoResponse resultado = verificador.verificar(chamado, false, null);

        assertFalse(resultado.aprovado());
        assertEquals(3, resultado.itensConformes());
        assertEquals(List.of("Associe o chamado a uma unidade.", "Atribua um responsável ao chamado.",
            "Registre ao menos um contato do solicitante."), resultado.pendencias());
    }

    @Test
    void deveExigirCausaRaizOuJustificativaComQuinzeCaracteres() {
        chamado.setCausaRaiz("  ");

        assertFalse(item(verificador.verificar(chamado, true, null), "CAUSA_RAIZ_OU_JUSTIFICATIVA").ok());
        assertFalse(item(verificador.verificar(chamado, true, "Sem causa"), "CAUSA_RAIZ_OU_JUSTIFICATIVA").ok());

        ItemVerificacaoResponse comJustificativa = item(
            verificador.verificar(chamado, true, "Falha intermitente sem causa identificada."), "CAUSA_RAIZ_OU_JUSTIFICATIVA");
        assertTrue(comJustificativa.ok());
        assertNull(comJustificativa.pendencia());
    }

    @Test
    void deveApontarResolucaoCurtaOuAusente() {
        chamado.setResolucao(null);
        assertFalse(item(verificador.verificar(chamado, true, null), "RESOLUCAO").ok());

        chamado.setResolucao("Resolvido");
        ItemVerificacaoResponse item = item(verificador.verificar(chamado, true, null), "RESOLUCAO");
        assertFalse(item.ok());
        assertEquals("Descreva a resolução com pelo menos 10 caracteres.", item.pendencia());
    }

    private static ItemVerificacaoResponse item(VerificacaoEncerramentoResponse resultado, String codigo) {
        return resultado.itens().stream().filter(i -> i.codigo().equals(codigo)).findFirst().orElseThrow();
    }
}
