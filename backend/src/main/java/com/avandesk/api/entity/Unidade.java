package com.avandesk.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "unidades")
@Getter
@Setter
@NoArgsConstructor
public class Unidade {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false)
    private String identificacao;

    private String localizacao;

    @Column(name = "atendimento_24h", nullable = false)
    private boolean atendimento24h;

    @Column(nullable = false)
    private boolean ativo = true;
}
