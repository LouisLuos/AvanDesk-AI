package com.avandesk.api.entity;

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
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "eventos_historico")
@Immutable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EventoHistorico {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "chamado_id", nullable = false)
    private Chamado chamado;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoEvento tipo;

    @Column(name = "ocorrido_em", nullable = false)
    private Instant ocorridoEm;

    /** Nulo quando o evento é gerado pelo sistema. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "autor_id")
    private Usuario autor;

    /** Detalhes do evento (coluna JSONB), por exemplo {"canal": "LIGACAO"}. */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private Map<String, Object> dados = new LinkedHashMap<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "origem_dado", nullable = false)
    private OrigemDado origemDado;

    public EventoHistorico(Chamado chamado, TipoEvento tipo, Instant ocorridoEm, Usuario autor,
                           Map<String, Object> dados, OrigemDado origemDado) {
        this.chamado = chamado;
        this.tipo = tipo;
        this.ocorridoEm = ocorridoEm;
        this.autor = autor;
        this.dados = dados == null ? new LinkedHashMap<>() : new LinkedHashMap<>(dados);
        this.origemDado = origemDado;
    }

    /** Devolve uma visão só de leitura, para ninguém alterar o evento depois de criado. */
    public Map<String, Object> getDados() {
        return Collections.unmodifiableMap(dados);
    }
}
