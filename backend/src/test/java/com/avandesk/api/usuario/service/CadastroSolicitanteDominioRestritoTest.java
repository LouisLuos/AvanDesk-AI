package com.avandesk.api.usuario.service;

import com.avandesk.api.core.exception.RegraNegocioException;
import com.avandesk.api.notificacao.port.NotificacaoPort;
import com.avandesk.api.unidade.repository.UnidadeRepository;
import com.avandesk.api.usuario.dto.CadastroSolicitanteRequest;
import com.avandesk.api.usuario.entity.StatusConta;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {
    "nexo.solicitantes.restringir-dominio-email=true",
    "nexo.solicitantes.dominios-permitidos=@lumen.com.br"
})
class CadastroSolicitanteDominioRestritoTest {

    @MockitoBean
    private NotificacaoPort notificacaoPort;

    @Autowired
    private CadastroSolicitanteService cadastroSolicitanteService;

    @Autowired
    private UnidadeRepository unidadeRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    void deveRecusarProvedorPessoalComARestricaoLigada() {
        emTransacaoDesfeita(() -> assertThatThrownBy(() -> cadastroSolicitanteService.registrar(
            new CadastroSolicitanteRequest("Mariana Alves", "mariana." + UUID.randomUUID() + "@gmail.com",
                unidadeAtiva(), "Coleta#2026ok")))
            .isInstanceOf(RegraNegocioException.class)
            .hasMessage("Use o seu e-mail corporativo (@lumen.com.br) para criar a conta."));
    }

    @Test
    void deveAceitarODominioCorporativoComARestricaoLigada() {
        emTransacaoDesfeita(() -> assertThat(cadastroSolicitanteService.registrar(
            new CadastroSolicitanteRequest("Mariana Alves", "mariana." + UUID.randomUUID() + "@lumen.com.br",
                unidadeAtiva(), "Coleta#2026ok")).statusConta())
            .isEqualTo(StatusConta.PENDENTE_VERIFICACAO));
    }

    private UUID unidadeAtiva() {
        return unidadeRepository.findByAtivoOrderByNomeAsc(true).getFirst().getId();
    }

    private void emTransacaoDesfeita(Runnable bloco) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            status.setRollbackOnly();
            bloco.run();
        });
    }
}
