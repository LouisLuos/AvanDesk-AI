package com.avandesk.api.notificacao.service;

import com.avandesk.api.catalogo.entity.Categoria;
import com.avandesk.api.chamado.entity.Chamado;
import com.avandesk.api.chamado.entity.MotivoEspera;
import com.avandesk.api.chamado.entity.OrigemDado;
import com.avandesk.api.chamado.entity.Solicitante;
import com.avandesk.api.chamado.entity.StatusChamado;
import com.avandesk.api.notificacao.entity.NotificacaoEmail;
import com.avandesk.api.notificacao.port.MensagemEmail;
import com.avandesk.api.unidade.entity.Unidade;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class ModelosDeEmailTest {

    private static final Instant ABERTO_EM = Instant.parse("2026-11-26T13:05:00Z");
    private static final Instant ATUALIZADO_EM = Instant.parse("2026-11-26T14:30:00Z");
    private static final String DESCRICAO_SENSIVEL = "Prontuário do paciente José, CPF 123.456.789-00, não abre.";

    private final ModelosDeEmail modelos = new ModelosDeEmail();

    @Test
    void deveMontarAAberturaComCodigoUnidadeCategoriaEData() {
        Chamado chamado = chamado(StatusChamado.ABERTO, null);

        MensagemEmail mensagem = modelos.montar(NotificacaoEmail.deAbertura(chamado, ATUALIZADO_EM, ATUALIZADO_EM));

        assertThat(mensagem.destinatario()).isEqualTo("recepcao.bvg@lumen.local");
        assertThat(mensagem.assunto()).isEqualTo("[Nexo] Chamado LUM-2026-000123 registrado");
        assertThat(mensagem.texto())
            .contains("Olá, Recepção Boa Viagem.", "Código: LUM-2026-000123", "Unidade: U.03 Boa Viagem",
                "Categoria: Equipamento de apoio", "Aberto em: 26/11/2026 às 10:05", "Status: Aberto",
                "Guarde o código LUM-2026-000123");
        assertThat(mensagem.html())
            .contains("LUM-2026-000123", "U.03 Boa Viagem", "Equipamento de apoio", "26/11/2026 às 10:05", "Aberto");
    }

    @Test
    void deveMontarAMudancaComStatusAnteriorNovoEDataFormatada() {
        Chamado chamado = chamado(StatusChamado.EM_ATENDIMENTO, null);

        MensagemEmail mensagem = modelos.montar(
            NotificacaoEmail.deMudancaDeStatus(chamado, StatusChamado.EM_TRIAGEM, ATUALIZADO_EM, ATUALIZADO_EM));

        assertThat(mensagem.assunto()).isEqualTo("[Nexo] Chamado LUM-2026-000123: Em Atendimento");
        assertThat(mensagem.texto()).contains("Status anterior: Em Triagem", "Novo status: Em Atendimento",
            "Atualizado em: 26/11/2026 às 11:30", "Unidade: U.03 Boa Viagem");
        assertThat(mensagem.html()).contains("Em Triagem", "Em Atendimento", "26/11/2026 às 11:30");
    }

    @Test
    void naoDeveExporADescricaoNemPermitirHtmlVindoDosDados() {
        Chamado chamado = chamado(StatusChamado.ABERTO, null);
        chamado.getUnidade().setNome("<script>alert(1)</script> Boa Viagem");

        MensagemEmail mensagem = modelos.montar(NotificacaoEmail.deAbertura(chamado, ATUALIZADO_EM, ATUALIZADO_EM));

        assertThat(mensagem.html()).doesNotContain("<script>").contains("&lt;script&gt;");
        assertThat(mensagem.texto()).doesNotContain(DESCRICAO_SENSIVEL);
        assertThat(mensagem.html()).doesNotContain("Prontuário", "CPF");
    }

    @Test
    void deveCumprimentarSemNomeQuandoOSolicitanteNaoInformou() {
        Chamado chamado = chamado(StatusChamado.ABERTO, null);
        chamado.getSolicitante().setNome(null);

        MensagemEmail mensagem = modelos.montar(NotificacaoEmail.deAbertura(chamado, ATUALIZADO_EM, ATUALIZADO_EM));

        assertThat(mensagem.texto()).startsWith("Olá.").doesNotContain("null");
        assertThat(mensagem.html()).contains("Olá.").doesNotContain("null");
    }

    @ParameterizedTest
    @CsvSource({
        "SOLICITANTE, O prazo do atendimento fica pausado até o seu retorno.",
        "FORNECEDOR, depende do retorno de um fornecedor externo",
        "PECA_EQUIPAMENTO, aguarda a chegada de uma peça ou equipamento"
    })
    void deveOrientarConformeOMotivoDaEspera(MotivoEspera motivo, String trecho) {
        Chamado chamado = chamado(StatusChamado.AGUARDANDO, motivo);

        MensagemEmail mensagem = modelos.montar(
            NotificacaoEmail.deMudancaDeStatus(chamado, StatusChamado.EM_ATENDIMENTO, ATUALIZADO_EM, ATUALIZADO_EM));

        assertThat(mensagem.texto()).contains(trecho);
    }

    @Test
    void deveOrientarAReaberturaEmAteSeteDiasQuandoResolvido() {
        Chamado chamado = chamado(StatusChamado.RESOLVIDO, null);

        MensagemEmail mensagem = modelos.montar(
            NotificacaoEmail.deMudancaDeStatus(chamado, StatusChamado.EM_ATENDIMENTO, ATUALIZADO_EM, ATUALIZADO_EM));

        assertThat(mensagem.texto()).contains("em até 7 dias", "LUM-2026-000123");
    }

    private static Chamado chamado(StatusChamado status, MotivoEspera motivo) {
        Unidade unidade = new Unidade();
        unidade.setNome("U.03 Boa Viagem");
        Categoria categoria = new Categoria();
        categoria.setNome("Equipamento de apoio");
        Solicitante solicitante = new Solicitante();
        solicitante.setNome("Recepção Boa Viagem");
        solicitante.setEmail("recepcao.bvg@lumen.local");

        Chamado chamado = new Chamado();
        chamado.setCodigo("LUM-2026-000123");
        chamado.setUnidade(unidade);
        chamado.setCategoria(categoria);
        chamado.setSolicitante(solicitante);
        chamado.setDescricao(DESCRICAO_SENSIVEL);
        chamado.setAbertoEm(ABERTO_EM);
        chamado.setStatus(status);
        chamado.setMotivoEspera(motivo);
        chamado.setOrigemDado(OrigemDado.TESTE);
        return chamado;
    }
}
