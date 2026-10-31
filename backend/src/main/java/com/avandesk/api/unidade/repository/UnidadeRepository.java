package com.avandesk.api.unidade.repository;

import com.avandesk.api.unidade.entity.Unidade;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UnidadeRepository extends JpaRepository<Unidade, UUID> {

    Optional<Unidade> findByIdentificacao(String identificacao);

    boolean existsByIdentificacao(String identificacao);

    boolean existsByIdentificacaoAndIdNot(String identificacao, UUID id);

    List<Unidade> findAllByOrderByNomeAsc();

    List<Unidade> findByAtivoOrderByNomeAsc(boolean ativo);
}
