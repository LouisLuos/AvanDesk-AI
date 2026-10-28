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
@Table(name = "registros_contato")
@Getter
@Setter
@NoArgsConstructor
public class RegistroContato {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "chamado_id", nullable = false)
    private Chamado chamado;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chamado_original_id")
    private Chamado chamadoOriginal;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "solicitante_id", nullable = false)
    private Solicitante solicitante;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CanalOrigem canal;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DirecaoContato direcao;

    @Column(name = "identificador_externo")
    private String identificadorExterno;

    private String conteudo;

    @Column(name = "ocorrido_em", nullable = false)
    private Instant ocorridoEm;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registrado_por_id")
    private Usuario registradoPor;

    @Enumerated(EnumType.STRING)
    @Column(name = "origem_dado", nullable = false, updatable = false)
    private OrigemDado origemDado;
}
