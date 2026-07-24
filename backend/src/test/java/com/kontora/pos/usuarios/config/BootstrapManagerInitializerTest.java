package com.kontora.pos.usuarios.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.kontora.pos.usuarios.domain.CredencialUsuario;
import com.kontora.pos.usuarios.domain.Rol;
import com.kontora.pos.usuarios.domain.Usuario;
import com.kontora.pos.usuarios.repository.CredencialUsuarioRepository;
import com.kontora.pos.usuarios.repository.RolRepository;
import com.kontora.pos.usuarios.repository.UsuarioRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

class BootstrapManagerInitializerTest {

    @Test
    void provisionaUnGerenteActivoConCredencialHasheadaCuandoNoExistenUsuarios() throws Exception {
        BootstrapManagerProperties properties = propiedadesValidas();
        UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
        RolRepository rolRepository = mock(RolRepository.class);
        CredencialUsuarioRepository credencialRepository = mock(CredencialUsuarioRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        Rol rolGerente = new Rol();

        when(usuarioRepository.count()).thenReturn(0L);
        when(rolRepository.findByNombreRol("gerente")).thenReturn(Optional.of(rolGerente));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(passwordEncoder.encode("ClaveLocal2026")).thenReturn("bcrypt-hash");

        crearInicializador(properties, usuarioRepository, rolRepository, credencialRepository, passwordEncoder).run(null);

        ArgumentCaptor<Usuario> usuarioCaptor = ArgumentCaptor.forClass(Usuario.class);
        ArgumentCaptor<CredencialUsuario> credencialCaptor = ArgumentCaptor.forClass(CredencialUsuario.class);
        verify(usuarioRepository).save(usuarioCaptor.capture());
        verify(credencialRepository).save(credencialCaptor.capture());

        Usuario usuario = usuarioCaptor.getValue();
        CredencialUsuario credencial = credencialCaptor.getValue();
        assertThat(usuario.getRol()).isSameAs(rolGerente);
        assertThat(usuario.getNombreUsuario()).isEqualTo("gerenteLocal");
        assertThat(usuario.getNombreCompleto()).isEqualTo("Gerente Local");
        assertThat(usuario.getEstado()).isEqualTo("activo");
        assertThat(credencial.getUsuario()).isSameAs(usuario);
        assertThat(credencial.getContrasenaHash()).isEqualTo("bcrypt-hash");
        assertThat(credencial.getEstado()).isEqualTo("activa");
        assertThat(credencial.isRequiereCambioContrasena()).isFalse();
    }

    @Test
    void noHaceCambiosCuandoYaExisteAlMenosUnUsuario() throws Exception {
        UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
        RolRepository rolRepository = mock(RolRepository.class);
        CredencialUsuarioRepository credencialRepository = mock(CredencialUsuarioRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        when(usuarioRepository.count()).thenReturn(1L);

        crearInicializador(propiedadesValidas(), usuarioRepository, rolRepository, credencialRepository, passwordEncoder).run(null);

        verify(rolRepository, never()).findByNombreRol(any());
        verify(usuarioRepository, never()).save(any());
        verify(credencialRepository, never()).save(any());
    }

    private BootstrapManagerProperties propiedadesValidas() {
        BootstrapManagerProperties properties = new BootstrapManagerProperties();
        properties.setEnabled(true);
        properties.setUsername("gerenteLocal");
        properties.setFullName("Gerente Local");
        properties.setPassword("ClaveLocal2026");
        return properties;
    }

    private BootstrapManagerInitializer crearInicializador(
            BootstrapManagerProperties properties,
            UsuarioRepository usuarioRepository,
            RolRepository rolRepository,
            CredencialUsuarioRepository credencialRepository,
            PasswordEncoder passwordEncoder) {
        return new BootstrapManagerInitializer(
                properties,
                usuarioRepository,
                rolRepository,
                credencialRepository,
                passwordEncoder);
    }
}
