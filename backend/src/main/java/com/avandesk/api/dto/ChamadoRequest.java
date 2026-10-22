package com.avandesk.api.dto;

import com.avandesk.api.entity.CanalOrigem;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.UUID;

public record ChamadoRequest(

    @NotNull(message = "é obrigatório")
    UUID unidadeId,

    @NotNull(message = "é obrigatório")
    CanalOrigem canalOrigem,

    @NotNull(message = "é obrigatório")
    @Valid
    SolicitanteRequest solicitante,

    @NotBlank(message = "é obrigatório")
    @Size(min = 10, max = 2000, message = "deve ter entre 10 e 2000 caracteres")
    String descricao,

    @NotNull(message = "é obrigatório")
    UUID categoriaId,

    UUID sistemaEquipamentoId,

    Instant registroIniciadoEm
) {
}
