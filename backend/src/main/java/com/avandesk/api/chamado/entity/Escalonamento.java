package com.avandesk.api.chamado.entity;

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
import com.avandesk.api.usuario.entity.Usuario;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "escalonamentos")
@Getter
@Setter
@NoArgsConstructor
public class Escalonamento {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "chamado_id", nullable = false, updatable = false)
    private Chamado chamado;

    @Column(nullable = false)
    private String fornecedor;

    @Column(name = "protocolo_externo")
    private String protocoloExterno;

    private String observacao;

    /** Responsável interno pelo chamado no momento do envio (RN07: continua responsável perante o SLA). */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "responsavel_id", nullable = false)
    private Usuario responsavel;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "enviado_por_id", nullable = false)
    private Usuario enviadoPor;

    @Column(name = "enviado_em", nullable = false)
    private Instant enviadoEm;

    private String retorno;

    @Column(name = "retorno_em")
    private Instant retornoEm;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "retorno_registrado_por_id")
    private Usuario retornoRegistradoPor;

    @Enumerated(EnumType.STRING)
    @Column(name = "origem_dado", nullable = false, updatable = false)
    private OrigemDado origemDado;

    public boolean aberto() {
        return retornoEm == null;
    }
}
