package com.avandesk.api.usuario.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tokens_verificacao_email")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TokenVerificacaoEmail {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false, updatable = false)
    private Usuario usuario;

    @Column(name = "token_hash", nullable = false, updatable = false, length = 64)
    private String tokenHash;

    @Column(name = "expira_em", nullable = false, updatable = false)
    private Instant expiraEm;

    @Column(name = "utilizado_em")
    private Instant utilizadoEm;

    @Column(name = "revogado_em")
    private Instant revogadoEm;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    public TokenVerificacaoEmail(Usuario usuario, String tokenHash, Instant criadoEm, Instant expiraEm) {
        this.usuario = usuario;
        this.tokenHash = tokenHash;
        this.criadoEm = criadoEm;
        this.expiraEm = expiraEm;
    }

    public boolean utilizado() {
        return utilizadoEm != null;
    }

    public boolean revogado() {
        return revogadoEm != null;
    }

    public boolean expiradoEm(Instant agora) {
        return !agora.isBefore(expiraEm);
    }

    public void marcarUtilizado(Instant agora) {
        this.utilizadoEm = agora;
    }

    public void revogar(Instant agora) {
        this.revogadoEm = agora;
    }
}
