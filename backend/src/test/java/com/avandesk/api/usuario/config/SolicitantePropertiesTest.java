package com.avandesk.api.usuario.config;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SolicitantePropertiesTest {

    @Test
    void deveAceitarQualquerProvedorComARestricaoDesligada() {
        SolicitanteProperties politica = politica(false, List.of("lumen.com.br"));

        assertThat(politica.aceitaDominioDe("recepcao@gmail.com")).isTrue();
        assertThat(politica.aceitaDominioDe("coleta@outlook.com")).isTrue();
    }

    @Test
    void deveAceitarSoOsDominiosPermitidosComARestricaoLigada() {
        SolicitanteProperties politica = politica(true, List.of("@Lumen.com.br", " lumen.local "));

        assertThat(politica.dominiosPermitidos()).containsExactly("lumen.com.br", "lumen.local");
        assertThat(politica.aceitaDominioDe("recepcao@lumen.com.br")).isTrue();
        assertThat(politica.aceitaDominioDe("recepcao@LUMEN.LOCAL")).isTrue();
        assertThat(politica.aceitaDominioDe("recepcao@gmail.com")).isFalse();
        assertThat(politica.aceitaDominioDe("recepcao@lumen.com.br.golpe.com")).isFalse();
        assertThat(politica.aceitaDominioDe("recepcao@sub.lumen.com.br")).isFalse();
    }

    private static SolicitanteProperties politica(boolean restringir, List<String> dominios) {
        return new SolicitanteProperties(restringir, dominios, Duration.ofHours(24), Duration.ofSeconds(60),
            "http://localhost:4200/verificar-email");
    }
}
