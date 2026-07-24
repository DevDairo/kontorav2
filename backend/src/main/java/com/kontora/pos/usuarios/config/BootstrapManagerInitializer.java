package com.kontora.pos.usuarios.config;

import com.kontora.pos.usuarios.domain.CredencialUsuario;
import com.kontora.pos.usuarios.domain.Rol;
import com.kontora.pos.usuarios.domain.Usuario;
import com.kontora.pos.usuarios.repository.CredencialUsuarioRepository;
import com.kontora.pos.usuarios.repository.RolRepository;
import com.kontora.pos.usuarios.repository.UsuarioRepository;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@ConditionalOnProperty(prefix = "kontora.bootstrap.manager", name = "enabled", havingValue = "true")
public class BootstrapManagerInitializer implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(BootstrapManagerInitializer.class);
    private static final String ROL_GERENTE = "gerente";

    private final BootstrapManagerProperties properties;
    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final CredencialUsuarioRepository credencialUsuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public BootstrapManagerInitializer(
            BootstrapManagerProperties properties,
            UsuarioRepository usuarioRepository,
            RolRepository rolRepository,
            CredencialUsuarioRepository credencialUsuarioRepository,
            PasswordEncoder passwordEncoder) {
        this.properties = properties;
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.credencialUsuarioRepository = credencialUsuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (usuarioRepository.count() > 0) {
            LOGGER.info("Se omite la provisión del gerente inicial porque ya existen usuarios.");
            return;
        }

        validarConfiguracion();
        String nombreUsuario = properties.getUsername().trim();
        Rol rolGerente = rolRepository.findByNombreRol(ROL_GERENTE)
                .orElseThrow(() -> new IllegalStateException("No existe el rol gerente para la provision inicial"));
        OffsetDateTime ahora = OffsetDateTime.now(ZoneOffset.UTC);

        Usuario gerente = new Usuario();
        gerente.setRol(rolGerente);
        gerente.setNombreUsuario(nombreUsuario);
        gerente.setNombreCompleto(properties.getFullName().trim());
        gerente.setEstado("activo");
        gerente.setFechaCreacion(ahora);
        gerente.setFechaActualizacion(ahora);
        Usuario gerenteGuardado = usuarioRepository.save(gerente);

        CredencialUsuario credencial = new CredencialUsuario();
        credencial.setUsuario(gerenteGuardado);
        credencial.setContrasenaHash(passwordEncoder.encode(properties.getPassword()));
        credencial.setRequiereCambioContrasena(false);
        credencial.setIntentosFallidos(0);
        credencial.setEstado("activa");
        credencialUsuarioRepository.save(credencial);

        LOGGER.info("Se provisionó el gerente inicial '{}'.", gerenteGuardado.getNombreUsuario());
    }

    private void validarConfiguracion() {
        String username = properties.getUsername() == null ? "" : properties.getUsername().trim();
        String fullName = properties.getFullName() == null ? "" : properties.getFullName().trim();
        String password = properties.getPassword() == null ? "" : properties.getPassword();

        if (!username.matches("[A-Za-z0-9]{3,50}")) {
            throw new IllegalStateException("BOOTSTRAP_MANAGER_USERNAME debe tener entre 3 y 50 caracteres alfanuméricos");
        }
        if (!StringUtils.hasText(fullName) || fullName.length() > 120) {
            throw new IllegalStateException("BOOTSTRAP_MANAGER_FULL_NAME debe tener entre 1 y 120 caracteres");
        }
        if (password.length() < 8 || password.length() > 72) {
            throw new IllegalStateException("BOOTSTRAP_MANAGER_PASSWORD debe tener entre 8 y 72 caracteres");
        }
    }
}
