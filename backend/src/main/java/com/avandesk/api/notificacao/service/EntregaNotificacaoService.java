package com.avandesk.api.notificacao.service;

import com.avandesk.api.chamado.entity.Chamado;
import com.avandesk.api.chamado.entity.TipoEvento;
import com.avandesk.api.chamado.service.EventoHistoricoService;
import com.avandesk.api.notificacao.entity.NotificacaoEmail;
import com.avandesk.api.notificacao.entity.SituacaoNotificacao;
import com.avandesk.api.notificacao.port.NotificacaoPort;
import com.avandesk.api.notificacao.repository.NotificacaoEmailRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class EntregaNotificacaoService {

    public static final List<Duration> ESPERAS_ENTRE_TENTATIVAS = List.of(
        Duration.ofMinutes(1), Duration.ofMinutes(5), Duration.ofMinutes(30));

    private final NotificacaoEmailRepository notificacaoEmailRepository;
    private final NotificacaoPort notificacaoPort;
    private final ModelosDeEmail modelosDeEmail;
    private final EventoHistoricoService eventoHistoricoService;

    @Transactional
    public void enviarAgora(UUID notificacaoId) {
        tentar(notificacaoId, false);
    }

    @Transactional
    public void reenviarSeVencida(UUID notificacaoId) {
        tentar(notificacaoId, true);
    }

    private void tentar(UUID notificacaoId, boolean respeitarAgenda) {
        NotificacaoEmail notificacao = notificacaoEmailRepository.travarParaEnvio(notificacaoId).orElse(null);
        Instant agora = Instant.now();
        if (notificacao == null || !notificacao.aguardandoEnvio()
            || (respeitarAgenda && !notificacao.vencidaPara(agora))) {
            return;
        }
        Chamado chamado = notificacao.getChamado();
        try {
            notificacaoPort.enviar(modelosDeEmail.montar(notificacao));
        } catch (RuntimeException erro) {
            registrarFalha(notificacao, chamado, erro, agora);
            return;
        }
        notificacao.registrarEnvio(agora);
        eventoHistoricoService.registrar(chamado, TipoEvento.NOTIFICACAO_ENVIADA, null, dadosDoEnvio(notificacao));
    }

    private static void registrarFalha(NotificacaoEmail notificacao, Chamado chamado, RuntimeException erro,
                                       Instant agora) {
        String motivo = erro.getClass().getSimpleName() + ": " + erro.getMessage();
        notificacao.registrarFalha(motivo, agora, ESPERAS_ENTRE_TENTATIVAS);
        if (notificacao.getSituacao() == SituacaoNotificacao.ESGOTADA) {
            log.error("Notificação {} do chamado {} desistida após {} tentativas: {}",
                notificacao.getId(), chamado.getCodigo(), notificacao.getTentativas(), motivo);
        } else {
            log.warn("Falha ao enviar a notificação {} do chamado {} (tentativa {}); nova tentativa em {}: {}",
                notificacao.getId(), chamado.getCodigo(), notificacao.getTentativas(),
                notificacao.getProximaTentativaEm(), motivo);
        }
    }

    private static Map<String, Object> dadosDoEnvio(NotificacaoEmail notificacao) {
        Map<String, Object> dados = new LinkedHashMap<>();
        dados.put("notificacao_id", notificacao.getId().toString());
        dados.put("canal", "EMAIL");
        dados.put("modelo", notificacao.getModelo().name());
        if (notificacao.getStatusAnterior() != null) {
            dados.put("status_anterior", notificacao.getStatusAnterior().name());
        }
        dados.put("status_novo", notificacao.getStatusNovo().name());
        return dados;
    }
}
