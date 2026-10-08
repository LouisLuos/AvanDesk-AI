package com.avandesk.api.service;

import com.avandesk.api.dto.UnidadeRequest;
import com.avandesk.api.dto.UnidadeResponse;
import com.avandesk.api.entity.Unidade;
import com.avandesk.api.exception.RecursoNaoEncontradoException;
import com.avandesk.api.exception.RegraNegocioException;
import com.avandesk.api.repository.UnidadeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UnidadeService {

    private final UnidadeRepository unidadeRepository;

    @Transactional
    public UnidadeResponse cadastrar(UnidadeRequest request) {
        String identificacao = normalizarIdentificacao(request.identificacao());
        if (unidadeRepository.existsByIdentificacao(identificacao)) {
            throw identificacaoDuplicada(identificacao);
        }

        Unidade unidade = new Unidade();
        preencher(unidade, request, identificacao);
        return UnidadeResponse.de(unidadeRepository.save(unidade));
    }

    @Transactional(readOnly = true)
    public List<UnidadeResponse> listar(Boolean ativo) {
        List<Unidade> unidades = ativo == null
            ? unidadeRepository.findAllByOrderByNomeAsc()
            : unidadeRepository.findByAtivoOrderByNomeAsc(ativo);
        return unidades.stream().map(UnidadeResponse::de).toList();
    }

    @Transactional(readOnly = true)
    public UnidadeResponse buscarPorId(UUID id) {
        return UnidadeResponse.de(buscarEntidade(id));
    }

    @Transactional
    public UnidadeResponse editar(UUID id, UnidadeRequest request) {
        Unidade unidade = buscarEntidade(id);
        String identificacao = normalizarIdentificacao(request.identificacao());
        if (unidadeRepository.existsByIdentificacaoAndIdNot(identificacao, id)) {
            throw identificacaoDuplicada(identificacao);
        }

        preencher(unidade, request, identificacao);
        return UnidadeResponse.de(unidade);
    }

    @Transactional
    public UnidadeResponse desativar(UUID id) {
        Unidade unidade = buscarEntidade(id);
        unidade.setAtivo(false);
        return UnidadeResponse.de(unidade);
    }

    private Unidade buscarEntidade(UUID id) {
        return unidadeRepository.findById(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException("Unidade não encontrada."));
    }

    private void preencher(Unidade unidade, UnidadeRequest request, String identificacao) {
        unidade.setNome(request.nome().trim());
        unidade.setIdentificacao(identificacao);
        unidade.setLocalizacao(normalizarLocalizacao(request.localizacao()));
        unidade.setAtendimento24h(Boolean.TRUE.equals(request.atendimento24h()));
    }

    private static String normalizarIdentificacao(String identificacao) {
        return identificacao.trim().toUpperCase(Locale.ROOT);
    }

    private static String normalizarLocalizacao(String localizacao) {
        return localizacao == null || localizacao.isBlank() ? null : localizacao.trim();
    }

    private static RegraNegocioException identificacaoDuplicada(String identificacao) {
        return new RegraNegocioException("Já existe uma unidade com a identificação '%s'.".formatted(identificacao));
    }
}
