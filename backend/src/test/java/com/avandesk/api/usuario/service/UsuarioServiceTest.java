package com.avandesk.api.usuario.service;

import com.avandesk.api.core.exception.ConflitoException;
import com.avandesk.api.core.exception.RecursoNaoEncontradoException;
import com.avandesk.api.core.exception.RegraNegocioException;
import com.avandesk.api.usuario.config.UsuarioProperties;
import com.avandesk.api.usuario.dto.AnalistaCadastradoResponse;
import com.avandesk.api.usuario.dto.AnalistaRequest;
import com.avandesk.api.usuario.dto.AnalistaResponse;
import com.avandesk.api.usuario.entity.PerfilUsuario;
import com.avandesk.api.usuario.entity.Usuario;
import com.avandesk.api.usuario.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Gestão de analistas (HU10). */
@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    private static final UUID ID = UUID.randomUUID();

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private GeradorSenhaTemporaria geradorSenhaTemporaria;

    private UsuarioService usuarioService;

    @BeforeEach
    void setUp() {
        usuarioService = new UsuarioService(usuarioRepository, passwordEncoder, geradorSenhaTemporaria,
            new UsuarioProperties(List.of("lumen.local")));
    }

    @Test
    void deveListarSoAnalistasFiltrandoPelaSituacao() {
        when(usuarioRepository.findByPerfilAndAtivoOrderByNomeAsc(PerfilUsuario.ANALISTA_SUPORTE, true))
            .thenReturn(List.of(analista(true)));

        List<AnalistaResponse> analistas = usuarioService.listarAnalistas(true);

        assertThat(analistas).extracting(AnalistaResponse::email).containsExactly("ana.souza@lumen.local");
    }

    @Test
    void deveCadastrarAnalistaComSenhaTemporariaGuardandoSoOHash() {
        when(usuarioRepository.existsByEmail("ana.souza@lumen.local")).thenReturn(false);
        when(geradorSenhaTemporaria.gerar()).thenReturn("Kp7m-Qx4t-Rz9w");
        when(passwordEncoder.encode("Kp7m-Qx4t-Rz9w")).thenReturn("hash-bcrypt");

        AnalistaCadastradoResponse cadastrado = usuarioService.cadastrarAnalista(
            new AnalistaRequest("  Ana Souza ", " Ana.Souza@LUMEN.local "));

        ArgumentCaptor<Usuario> salvo = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(salvo.capture());
        assertThat(salvo.getValue().getNome()).isEqualTo("Ana Souza");
        assertThat(salvo.getValue().getEmail()).isEqualTo("ana.souza@lumen.local");
        assertThat(salvo.getValue().getSenhaHash()).isEqualTo("hash-bcrypt");
        assertThat(salvo.getValue().getPerfil()).isEqualTo(PerfilUsuario.ANALISTA_SUPORTE);
        assertThat(salvo.getValue().isAtivo()).isTrue();
        assertThat(cadastrado.senhaTemporaria()).isEqualTo("Kp7m-Qx4t-Rz9w");
        assertThat(cadastrado.analista().email()).isEqualTo("ana.souza@lumen.local");
    }

    @Test
    void deveRecusarEmailForaDoDominioCorporativo() {
        assertThatThrownBy(() -> usuarioService.cadastrarAnalista(new AnalistaRequest("Ana", "ana@gmail.com")))
            .isInstanceOf(RegraNegocioException.class)
            .hasMessage("Use um e-mail corporativo (@lumen.local).");

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void deveRecusarEmailRepetidoComConflito() {
        when(usuarioRepository.existsByEmail("ana.souza@lumen.local")).thenReturn(true);

        assertThatThrownBy(() -> usuarioService.cadastrarAnalista(
            new AnalistaRequest("Ana", "ana.souza@lumen.local")))
            .isInstanceOf(ConflitoException.class)
            .hasMessage("Já existe um usuário com o e-mail 'ana.souza@lumen.local'.");

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void deveEditarNomeEEmailIgnorandoOProprioCadastroNaUnicidade() {
        Usuario analista = analista(true);
        when(usuarioRepository.findById(ID)).thenReturn(Optional.of(analista));
        when(usuarioRepository.existsByEmailAndIdNot("ana.lima@lumen.local", ID)).thenReturn(false);

        AnalistaResponse editado = usuarioService.editarAnalista(ID, new AnalistaRequest("Ana Lima", "ana.lima@lumen.local"));

        assertThat(editado.nome()).isEqualTo("Ana Lima");
        assertThat(analista.getEmail()).isEqualTo("ana.lima@lumen.local");
    }

    @Test
    void deveRecusarEdicaoComEmailDeOutroUsuario() {
        when(usuarioRepository.findById(ID)).thenReturn(Optional.of(analista(true)));
        when(usuarioRepository.existsByEmailAndIdNot("lideranca@lumen.local", ID)).thenReturn(true);

        assertThatThrownBy(() -> usuarioService.editarAnalista(ID, new AnalistaRequest("Ana", "lideranca@lumen.local")))
            .isInstanceOf(ConflitoException.class);
    }

    @Test
    void deveTratarUsuarioQueNaoEAnalistaComoNaoEncontrado() {
        Usuario lideranca = analista(true);
        lideranca.setPerfil(PerfilUsuario.LIDERANCA_TI);
        when(usuarioRepository.findById(ID)).thenReturn(Optional.of(lideranca));

        assertThatThrownBy(() -> usuarioService.desativarAnalista(ID))
            .isInstanceOf(RecursoNaoEncontradoException.class)
            .hasMessage("Analista não encontrado.");
    }

    @Test
    void deveDesativarEReativarSemApagarOCadastro() {
        Usuario analista = analista(true);
        when(usuarioRepository.findById(ID)).thenReturn(Optional.of(analista));

        assertThat(usuarioService.desativarAnalista(ID).ativo()).isFalse();
        assertThat(analista.isAtivo()).isFalse();

        assertThat(usuarioService.reativarAnalista(ID).ativo()).isTrue();
        assertThat(analista.isAtivo()).isTrue();
        verify(usuarioRepository, never()).delete(any());
    }

    private static Usuario analista(boolean ativo) {
        Usuario usuario = new Usuario();
        usuario.setId(ID);
        usuario.setNome("Ana Souza");
        usuario.setEmail("ana.souza@lumen.local");
        usuario.setPerfil(PerfilUsuario.ANALISTA_SUPORTE);
        usuario.setAtivo(ativo);
        return usuario;
    }
}
