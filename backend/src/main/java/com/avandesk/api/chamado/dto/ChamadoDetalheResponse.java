package com.avandesk.api.chamado.dto;

import com.avandesk.api.catalogo.dto.CategoriaResponse;
import com.avandesk.api.catalogo.dto.SistemaEquipamentoResponse;
import com.avandesk.api.chamado.entity.CanalOrigem;
import com.avandesk.api.chamado.entity.Chamado;
import com.avandesk.api.chamado.entity.EventoHistorico;
import com.avandesk.api.chamado.entity.MotivoEspera;
import com.avandesk.api.chamado.entity.OrigemDado;
import com.avandesk.api.chamado.entity.Prioridade;
import com.avandesk.api.chamado.entity.RegistroContato;
import com.avandesk.api.chamado.entity.StatusChamado;
import com.avandesk.api.unidade.dto.UnidadeResumoResponse;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Detalhe do chamado (HU02 + HU05): dados, status, responsável, contatos e linha do tempo. */
public record ChamadoDetalheResponse(

    UUID id,
    String codigo,
    StatusChamado status,
    MotivoEspera motivoEspera,
    String descricao,
    CanalOrigem canalOrigem,
    UnidadeResumoResponse unidade,
    CategoriaResponse categoria,
    SistemaEquipamentoResponse sistemaEquipamento,
    SolicitanteResponse solicitante,
    ResponsavelResponse responsavel,
    Prioridade prioridade,
    Instant abertoEm,
    Instant primeiraRespostaEm,
    OrigemDado origemDado,
    List<RegistroContatoResponse> registrosContato,
    List<EventoHistoricoResponse> historico
) {

    /** Os contatos e os eventos já chegam em ordem de ocorrido_em. */
    public static ChamadoDetalheResponse de(Chamado chamado, List<RegistroContato> contatos,
                                            List<EventoHistorico> eventos) {
        Prioridade prioridade = chamado.getPrioridadeFinal() != null
            ? chamado.getPrioridadeFinal()
            : chamado.getPrioridadeSugerida();

        return new ChamadoDetalheResponse(
            chamado.getId(),
            chamado.getCodigo(),
            chamado.getStatus(),
            chamado.getMotivoEspera(),
            chamado.getDescricao(),
            chamado.getCanalOrigem(),
            UnidadeResumoResponse.de(chamado.getUnidade()),
            chamado.getCategoria() == null ? null : CategoriaResponse.de(chamado.getCategoria()),
            chamado.getSistemaEquipamento() == null
                ? null
                : SistemaEquipamentoResponse.de(chamado.getSistemaEquipamento()),
            SolicitanteResponse.de(chamado.getSolicitante()),
            ResponsavelResponse.de(chamado.getResponsavel()),
            prioridade,
            chamado.getAbertoEm(),
            chamado.getPrimeiraRespostaEm(),
            chamado.getOrigemDado(),
            contatos.stream().map(RegistroContatoResponse::de).toList(),
            eventos.stream().map(EventoHistoricoResponse::de).toList()
        );
    }
}
