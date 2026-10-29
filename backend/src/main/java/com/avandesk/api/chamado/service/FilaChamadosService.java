package com.avandesk.api.chamado.service;

import com.avandesk.api.chamado.dto.ChamadoResumoResponse;
import com.avandesk.api.chamado.dto.FilaChamadosResponse;
import com.avandesk.api.chamado.dto.FiltroChamados;
import com.avandesk.api.chamado.entity.Chamado;
import com.avandesk.api.chamado.entity.StatusChamado;
import com.avandesk.api.chamado.repository.ChamadoRepository;
import com.avandesk.api.chamado.repository.ChamadoSpecification;
import com.avandesk.api.chamado.repository.EventoHistoricoRepository;
import com.avandesk.api.chamado.repository.EventoHistoricoRepository.UltimaAtualizacao;
import com.avandesk.api.core.exception.RegraNegocioException;
import com.avandesk.api.usuario.service.UsuarioLogadoService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Fila de chamados (HU14): lista paginada, do mais recente para o mais antigo, com filtros que se somam.
 */
@Service
@RequiredArgsConstructor
public class FilaChamadosService {

    public static final int TAMANHO_PADRAO = 20;
    public static final int TAMANHO_MAXIMO = 100;

    /** Ordem fixa: o cliente não escolhe o campo, para não ordenar por qualquer coluna. */
    private static final Sort MAIS_RECENTES_PRIMEIRO =
        Sort.by(Sort.Order.desc("abertoEm"), Sort.Order.desc("id"));

    private final ChamadoRepository chamadoRepository;
    private final EventoHistoricoRepository eventoHistoricoRepository;
    private final UsuarioLogadoService usuarioLogadoService;

    /**
     * @param meus quando true, só os chamados do usuário autenticado (descoberto pelo token, não pelo
     *             cliente). Se vier junto com outro {@code responsavelId}, o resultado é vazio, porque
     *             os filtros se somam.
     */
    @Transactional(readOnly = true)
    public FilaChamadosResponse listar(FiltroChamados filtro, boolean meus, int pagina, int tamanho) {
        validarPaginacao(pagina, tamanho);
        FiltroChamados filtroFinal = meus ? comResponsavelLogado(filtro) : filtro;
        if (filtroFinal == null) {
            return paginaVazia(pagina, tamanho);
        }

        Page<Chamado> resultado = chamadoRepository.findAll(
            ChamadoSpecification.de(filtroFinal), PageRequest.of(pagina, tamanho, MAIS_RECENTES_PRIMEIRO));
        Map<UUID, Instant> atualizacoes = ultimasAtualizacoes(resultado.getContent());
        List<ChamadoResumoResponse> itens = resultado.getContent().stream()
            .map(chamado -> ChamadoResumoResponse.de(chamado, atualizacoes.get(chamado.getId())))
            .toList();

        return new FilaChamadosResponse(itens, pagina, tamanho, resultado.getTotalElements(),
            resultado.getTotalPages(), totaisPorStatus(filtroFinal));
    }

    private static void validarPaginacao(int pagina, int tamanho) {
        if (pagina < 0) {
            throw new RegraNegocioException("O parâmetro 'page' deve ser 0 ou maior.");
        }
        if (tamanho < 1 || tamanho > TAMANHO_MAXIMO) {
            throw new RegraNegocioException(
                "O parâmetro 'size' deve estar entre 1 e %d.".formatted(TAMANHO_MAXIMO));
        }
    }

    /** Devolve null quando o filtro pede outro responsável junto com "meus": não há resultado possível. */
    private FiltroChamados comResponsavelLogado(FiltroChamados filtro) {
        UUID eu = usuarioLogadoService.obterUsuarioLogado().getId();
        if (filtro.responsavelId() != null && !filtro.responsavelId().equals(eu)) {
            return null;
        }
        return new FiltroChamados(filtro.status(), filtro.unidadeId(), filtro.prioridade(),
            filtro.canalOrigem(), eu, filtro.semResponsavel(), filtro.codigo());
    }

    private Map<UUID, Instant> ultimasAtualizacoes(List<Chamado> chamados) {
        if (chamados.isEmpty()) {
            return Map.of();
        }
        List<UUID> ids = chamados.stream().map(Chamado::getId).toList();
        return eventoHistoricoRepository.buscarUltimaAtualizacao(ids).stream()
            .filter(linha -> linha.getUltimaAtualizacao() != null)
            .collect(Collectors.toMap(UltimaAtualizacao::getChamadoId, UltimaAtualizacao::getUltimaAtualizacao));
    }

    /** Conta com os mesmos filtros, menos o de status, para os cartões continuarem mostrando todos os status. */
    private Map<StatusChamado, Long> totaisPorStatus(FiltroChamados filtro) {
        Map<StatusChamado, Long> contagem = chamadoRepository.contarPorStatus(
            ChamadoSpecification.de(filtro.semStatus()));
        return comTodosOsStatus(contagem);
    }

    private static Map<StatusChamado, Long> comTodosOsStatus(Map<StatusChamado, Long> contagem) {
        Map<StatusChamado, Long> totais = new EnumMap<>(StatusChamado.class);
        for (StatusChamado status : StatusChamado.values()) {
            totais.put(status, Objects.requireNonNullElse(contagem.get(status), 0L));
        }
        return totais;
    }

    private static FilaChamadosResponse paginaVazia(int pagina, int tamanho) {
        return new FilaChamadosResponse(List.of(), pagina, tamanho, 0, 0, comTodosOsStatus(Map.of()));
    }
}
