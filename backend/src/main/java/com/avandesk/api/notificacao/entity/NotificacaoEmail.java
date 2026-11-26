package com.avandesk.api.notificacao.entity;

import com.avandesk.api.chamado.entity.Chamado;
import com.avandesk.api.chamado.entity.MotivoEspera;
import com.avandesk.api.chamado.entity.OrigemDado;
import com.avandesk.api.chamado.entity.StatusChamado;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "notificacoes_email")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificacaoEmail {

    public static final int TAMANHO_MAXIMO_ERRO = 500;

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "chamado_id", nullable = false, updatable = false)
    private Chamado chamado;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private ModeloNotificacao modelo;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_anterior", updatable = false)
    private StatusChamado statusAnterior;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_novo", nullable = false, updatable = false)
    private StatusChamado statusNovo;

    @Enumerated(EnumType.STRING)
    @Column(name = "motivo_espera", updatable = false)
    private MotivoEspera motivoEspera;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SituacaoNotificacao situacao = SituacaoNotificacao.PENDENTE;

    @Column(nullable = false)
    private int tentativas;

    @Column(name = "ultimo_erro", length = TAMANHO_MAXIMO_ERRO)
    private String ultimoErro;

    @Column(name = "proxima_tentativa_em")
    private Instant proximaTentativaEm;

    @Column(name = "enviada_em")
    private Instant enviadaEm;

    @Column(name = "criada_em", nullable = false, updatable = false)
    private Instant criadaEm;

    @Enumerated(EnumType.STRING)
    @Column(name = "origem_dado", nullable = false, updatable = false)
    private OrigemDado origemDado;

    public static NotificacaoEmail deAbertura(Chamado chamado, Instant agora, Instant reenvioSeNaoSair) {
        return new NotificacaoEmail(chamado, ModeloNotificacao.ABERTURA, null, agora, reenvioSeNaoSair);
    }

    public static NotificacaoEmail deMudancaDeStatus(Chamado chamado, StatusChamado anterior, Instant agora,
                                                     Instant reenvioSeNaoSair) {
        return new NotificacaoEmail(chamado, ModeloNotificacao.MUDANCA_STATUS, anterior, agora, reenvioSeNaoSair);
    }

    private NotificacaoEmail(Chamado chamado, ModeloNotificacao modelo, StatusChamado anterior, Instant agora,
                             Instant reenvioSeNaoSair) {
        this.chamado = chamado;
        this.modelo = modelo;
        this.statusAnterior = anterior;
        this.statusNovo = chamado.getStatus();
        this.motivoEspera = chamado.getStatus() == StatusChamado.AGUARDANDO ? chamado.getMotivoEspera() : null;
        this.criadaEm = agora;
        this.proximaTentativaEm = reenvioSeNaoSair;
        this.origemDado = chamado.getOrigemDado();
    }

    public boolean aguardandoEnvio() {
        return situacao.aguardandoEnvio();
    }

    public boolean vencidaPara(Instant agora) {
        return aguardandoEnvio() && !proximaTentativaEm.isAfter(agora);
    }

    public void registrarEnvio(Instant agora) {
        tentativas++;
        situacao = SituacaoNotificacao.ENVIADA;
        enviadaEm = agora;
        proximaTentativaEm = null;
        ultimoErro = null;
    }

    public void registrarFalha(String erro, Instant agora, List<Duration> esperasEntreTentativas) {
        tentativas++;
        ultimoErro = erro == null || erro.length() <= TAMANHO_MAXIMO_ERRO ? erro : erro.substring(0, TAMANHO_MAXIMO_ERRO);
        if (tentativas > esperasEntreTentativas.size()) {
            situacao = SituacaoNotificacao.ESGOTADA;
            proximaTentativaEm = null;
        } else {
            situacao = SituacaoNotificacao.FALHOU;
            proximaTentativaEm = agora.plus(esperasEntreTentativas.get(tentativas - 1));
        }
    }
}
