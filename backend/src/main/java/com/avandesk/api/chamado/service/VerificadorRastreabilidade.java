package com.avandesk.api.chamado.service;

import com.avandesk.api.chamado.dto.ItemVerificacaoResponse;
import com.avandesk.api.chamado.dto.VerificacaoEncerramentoResponse;
import com.avandesk.api.chamado.entity.Chamado;

import java.util.List;

/** Checklist de rastreabilidade do encerramento (HU15 / M01): os seis itens que o histórico precisa ter. */
public class VerificadorRastreabilidade {

    public static final int TAMANHO_MINIMO_TEXTO = 10;
    public static final int TAMANHO_MINIMO_JUSTIFICATIVA = 15;

    public VerificacaoEncerramentoResponse verificar(Chamado chamado, boolean temContato,
                                                     String justificativaSemCausaRaiz) {
        List<ItemVerificacaoResponse> itens = List.of(
            problemaECategoria(chamado),
            item("UNIDADE", "Unidade associada", chamado.getUnidade() != null,
                "Associe o chamado a uma unidade."),
            item("RESPONSAVEL", "Responsável identificado", chamado.getResponsavel() != null,
                "Atribua um responsável ao chamado."),
            item("CONTATO", "Ao menos um contato registrado", temContato,
                "Registre ao menos um contato do solicitante."),
            causaRaizOuJustificativa(chamado, justificativaSemCausaRaiz),
            item("RESOLUCAO", "Resolução detalhada", temTexto(chamado.getResolucao(), TAMANHO_MINIMO_TEXTO),
                "Descreva a resolução com pelo menos %d caracteres.".formatted(TAMANHO_MINIMO_TEXTO))
        );
        return VerificacaoEncerramentoResponse.de(itens);
    }

    private static ItemVerificacaoResponse problemaECategoria(Chamado chamado) {
        boolean temProblema = temTexto(chamado.getDescricao(), TAMANHO_MINIMO_TEXTO);
        boolean temCategoria = chamado.getCategoria() != null;
        String pendencia = !temProblema && !temCategoria ? "Descreva o problema e informe a categoria."
            : !temProblema ? "Descreva o problema com pelo menos %d caracteres.".formatted(TAMANHO_MINIMO_TEXTO)
            : "Informe a categoria do chamado.";
        return item("PROBLEMA_E_CATEGORIA", "Problema e categoria descritos", temProblema && temCategoria, pendencia);
    }

    private static ItemVerificacaoResponse causaRaizOuJustificativa(Chamado chamado, String justificativa) {
        boolean ok = temTexto(chamado.getCausaRaiz(), 1) || temTexto(justificativa, TAMANHO_MINIMO_JUSTIFICATIVA);
        return item("CAUSA_RAIZ_OU_JUSTIFICATIVA", "Causa raiz ou justificativa preenchida", ok,
            "Informe a causa raiz na resolução ou justifique, com pelo menos %d caracteres, por que não há causa raiz."
                .formatted(TAMANHO_MINIMO_JUSTIFICATIVA));
    }

    private static ItemVerificacaoResponse item(String codigo, String descricao, boolean ok, String pendencia) {
        return new ItemVerificacaoResponse(codigo, descricao, ok, ok ? null : pendencia);
    }

    private static boolean temTexto(String texto, int tamanhoMinimo) {
        return texto != null && texto.trim().length() >= tamanhoMinimo;
    }
}
