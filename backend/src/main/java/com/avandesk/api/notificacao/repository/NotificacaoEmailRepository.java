package com.avandesk.api.notificacao.repository;

import com.avandesk.api.notificacao.entity.NotificacaoEmail;
import com.avandesk.api.notificacao.entity.SituacaoNotificacao;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificacaoEmailRepository extends JpaRepository<NotificacaoEmail, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT n FROM NotificacaoEmail n WHERE n.id = :id")
    Optional<NotificacaoEmail> travarParaEnvio(UUID id);

    @Query("""
        SELECT n.id FROM NotificacaoEmail n
        WHERE n.situacao IN :situacoes AND n.proximaTentativaEm <= :agora
        ORDER BY n.proximaTentativaEm
        """)
    List<UUID> vencidas(Collection<SituacaoNotificacao> situacoes, Instant agora, Pageable lote);

    List<NotificacaoEmail> findByChamadoIdOrderByCriadaEmAsc(UUID chamadoId);
}
