package com.avandesk.api.repository;

import com.avandesk.api.entity.Unidade;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface UnidadeRepository extends JpaRepository<Unidade, UUID> {

    boolean existsByIdentificacao(String identificacao);

    boolean existsByIdentificacaoAndIdNot(String identificacao, UUID id);

    List<Unidade> findAllByOrderByNomeAsc();

    List<Unidade> findByAtivoOrderByNomeAsc(boolean ativo);
}
