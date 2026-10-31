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
import com.avandesk.api.catalogo.entity.Categoria;
import com.avandesk.api.catalogo.entity.SistemaEquipamento;
import com.avandesk.api.unidade.entity.Unidade;
import com.avandesk.api.usuario.entity.Usuario;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "chamados")
@Getter
@Setter
@NoArgsConstructor
public class Chamado {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, updatable = false)
    private String codigo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "unidade_id", nullable = false)
    private Unidade unidade;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "solicitante_id", nullable = false)
    private Solicitante solicitante;

    @Enumerated(EnumType.STRING)
    @Column(name = "canal_origem", nullable = false)
    private CanalOrigem canalOrigem;

    @Column(nullable = false)
    private String descricao;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categoria_id")
    private Categoria categoria;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sistema_equipamento_id")
    private SistemaEquipamento sistemaEquipamento;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusChamado status = StatusChamado.ABERTO;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "criado_por_id", nullable = false, updatable = false)
    private Usuario criadoPor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "responsavel_id")
    private Usuario responsavel;

    private Short severidade;

    private Short urgencia;

    @Column(name = "impacto_operacional")
    private Short impactoOperacional;

    @Enumerated(EnumType.STRING)
    @Column(name = "prioridade_sugerida")
    private Prioridade prioridadeSugerida;

    @Enumerated(EnumType.STRING)
    @Column(name = "prioridade_final")
    private Prioridade prioridadeFinal;

    @Column(name = "justificativa_prioridade")
    private String justificativaPrioridade;

    @Enumerated(EnumType.STRING)
    @Column(name = "motivo_espera")
    private MotivoEspera motivoEspera;

    @Column(name = "espera_iniciada_em")
    private Instant esperaIniciadaEm;

    @Column(name = "aberto_em", nullable = false, updatable = false)
    private Instant abertoEm = Instant.now();

    @Column(name = "registro_iniciado_em")
    private Instant registroIniciadoEm;

    @Column(name = "primeira_resposta_em")
    private Instant primeiraRespostaEm;

    @Column(name = "resolvido_em")
    private Instant resolvidoEm;

    @Column(name = "fechado_em")
    private Instant fechadoEm;

    private String resolucao;

    @Column(name = "causa_raiz")
    private String causaRaiz;

    @Column(name = "possivel_recorrencia", nullable = false)
    private boolean possivelRecorrencia = false;

    @Column(name = "motivo_cancelamento")
    private String motivoCancelamento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chamado_principal_id")
    private Chamado chamadoPrincipal;

    @Enumerated(EnumType.STRING)
    @Column(name = "origem_dado", nullable = false, updatable = false)
    private OrigemDado origemDado;
}
