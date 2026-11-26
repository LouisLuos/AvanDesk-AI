package com.avandesk.api.usuario.repository;

import com.avandesk.api.usuario.entity.TokenVerificacaoEmail;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TokenVerificacaoEmailRepository extends JpaRepository<TokenVerificacaoEmail, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM TokenVerificacaoEmail t JOIN FETCH t.usuario WHERE t.tokenHash = :tokenHash")
    Optional<TokenVerificacaoEmail> travarPorHash(String tokenHash);

    @Query("""
        SELECT t FROM TokenVerificacaoEmail t
        WHERE t.usuario.id = :usuarioId AND t.utilizadoEm IS NULL AND t.revogadoEm IS NULL
        """)
    List<TokenVerificacaoEmail> abertosDoUsuario(UUID usuarioId);

    Optional<TokenVerificacaoEmail> findFirstByUsuarioIdOrderByCriadoEmDesc(UUID usuarioId);
}
