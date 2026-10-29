package com.avandesk.api.usuario.repository;

import com.avandesk.api.usuario.entity.PerfilUsuario;
import com.avandesk.api.usuario.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UsuarioRepository extends JpaRepository<Usuario, UUID> {

    Optional<Usuario> findByEmail(String email);

    List<Usuario> findByPerfilAndAtivoTrueOrderByNomeAsc(PerfilUsuario perfil);
}
