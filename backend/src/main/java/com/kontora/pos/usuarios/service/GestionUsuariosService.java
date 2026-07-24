package com.kontora.pos.usuarios.service;

import com.kontora.pos.auditoria.service.AuditoriaService;
import com.kontora.pos.common.exception.ApiException;
import com.kontora.pos.common.security.PrincipalUsuario;
import com.kontora.pos.usuarios.domain.CredencialUsuario;
import com.kontora.pos.usuarios.domain.Rol;
import com.kontora.pos.usuarios.domain.SesionUsuario;
import com.kontora.pos.usuarios.domain.Usuario;
import com.kontora.pos.usuarios.dto.ActualizarEstadoUsuarioRequest;
import com.kontora.pos.usuarios.dto.ActualizarUsuarioRequest;
import com.kontora.pos.usuarios.dto.CrearUsuarioRequest;
import com.kontora.pos.usuarios.dto.RestablecerContrasenaUsuarioRequest;
import com.kontora.pos.usuarios.dto.RolGestionResponse;
import com.kontora.pos.usuarios.dto.UsuarioGestionResponse;
import com.kontora.pos.usuarios.repository.CredencialUsuarioRepository;
import com.kontora.pos.usuarios.repository.RolRepository;
import com.kontora.pos.usuarios.repository.SesionUsuarioRepository;
import com.kontora.pos.usuarios.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static com.kontora.pos.common.audit.AuditoriaValores.valores;

@Service
public class GestionUsuariosService {

    private static final String ROL_GERENTE = "gerente";

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final CredencialUsuarioRepository credencialUsuarioRepository;
    private final SesionUsuarioRepository sesionUsuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditoriaService auditoriaService;

    public GestionUsuariosService(
            UsuarioRepository usuarioRepository,
            RolRepository rolRepository,
            CredencialUsuarioRepository credencialUsuarioRepository,
            SesionUsuarioRepository sesionUsuarioRepository,
            PasswordEncoder passwordEncoder,
            AuditoriaService auditoriaService) {
        this.usuarioRepository = usuarioRepository;
        this.rolRepository = rolRepository;
        this.credencialUsuarioRepository = credencialUsuarioRepository;
        this.sesionUsuarioRepository = sesionUsuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.auditoriaService = auditoriaService;
    }

    @Transactional(readOnly = true)
    public List<UsuarioGestionResponse> listarUsuarios(PrincipalUsuario principalUsuario) {
        validarGerente(principalUsuario);
        return usuarioRepository.findAllByOrderByNombreCompletoAsc().stream()
                .map(this::aRespuesta)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RolGestionResponse> listarRolesActivos(PrincipalUsuario principalUsuario) {
        validarGerente(principalUsuario);
        return rolRepository.findActivos().stream()
                .map(rol -> new RolGestionResponse(rol.getIdRol(), rol.getNombreRol()))
                .toList();
    }

    @Transactional
    public UsuarioGestionResponse crearUsuario(CrearUsuarioRequest request, PrincipalUsuario principalUsuario) {
        validarGerente(principalUsuario);
        String nombreUsuario = normalizarNombreUsuario(request.nombreUsuario());
        validarNombreUsuarioDisponible(nombreUsuario, null);
        Rol rol = obtenerRolActivo(request.nombreRol());
        OffsetDateTime ahora = OffsetDateTime.now();

        Usuario usuario = new Usuario();
        usuario.setRol(rol);
        usuario.setNombreUsuario(nombreUsuario);
        usuario.setNombreCompleto(normalizarTexto(request.nombreCompleto()));
        usuario.setEstado("activo");
        usuario.setFechaCreacion(ahora);
        usuario.setFechaActualizacion(ahora);
        Usuario usuarioGuardado = usuarioRepository.saveAndFlush(usuario);

        CredencialUsuario credencial = new CredencialUsuario();
        credencial.setUsuario(usuarioGuardado);
        credencial.setContrasenaHash(passwordEncoder.encode(request.contrasena()));
        credencial.setRequiereCambioContrasena(true);
        credencial.setIntentosFallidos(0);
        credencial.setEstado("activa");
        credencialUsuarioRepository.saveAndFlush(credencial);

        auditoriaService.registrar(
                principalUsuario.idUsuario(),
                "usuarios",
                usuarioGuardado.getIdUsuario(),
                "crear",
                null,
                valoresUsuario(usuarioGuardado),
                "Creacion de usuario");
        return aRespuesta(usuarioGuardado);
    }

    @Transactional
    public UsuarioGestionResponse actualizarUsuario(
            UUID idUsuario,
            ActualizarUsuarioRequest request,
            PrincipalUsuario principalUsuario) {
        validarGerente(principalUsuario);
        Usuario usuario = obtenerUsuario(idUsuario);
        var valorAnterior = valoresUsuario(usuario);
        String nombreUsuario = normalizarNombreUsuario(request.nombreUsuario());
        validarNombreUsuarioDisponible(nombreUsuario, usuario.getIdUsuario());

        usuario.setRol(obtenerRolActivo(request.nombreRol()));
        usuario.setNombreUsuario(nombreUsuario);
        usuario.setNombreCompleto(normalizarTexto(request.nombreCompleto()));
        usuario.setFechaActualizacion(OffsetDateTime.now());
        Usuario usuarioGuardado = usuarioRepository.saveAndFlush(usuario);

        auditoriaService.registrar(
                principalUsuario.idUsuario(),
                "usuarios",
                usuarioGuardado.getIdUsuario(),
                "editar",
                valorAnterior,
                valoresUsuario(usuarioGuardado),
                "Actualizacion de usuario");
        return aRespuesta(usuarioGuardado);
    }

    @Transactional
    public UsuarioGestionResponse actualizarEstado(
            UUID idUsuario,
            ActualizarEstadoUsuarioRequest request,
            PrincipalUsuario principalUsuario) {
        validarGerente(principalUsuario);
        Usuario usuario = obtenerUsuario(idUsuario);
        String estado = request.estado().trim().toLowerCase(Locale.ROOT);

        if (usuario.getIdUsuario().equals(principalUsuario.idUsuario()) && !"activo".equals(estado)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "No puedes inactivar o bloquear tu propio usuario");
        }

        if (estado.equals(usuario.getEstado())) {
            return aRespuesta(usuario);
        }

        var valorAnterior = valoresUsuario(usuario);
        usuario.setEstado(estado);
        usuario.setFechaActualizacion(OffsetDateTime.now());
        Usuario usuarioGuardado = usuarioRepository.saveAndFlush(usuario);

        auditoriaService.registrar(
                principalUsuario.idUsuario(),
                "usuarios",
                usuarioGuardado.getIdUsuario(),
                "editar",
                valorAnterior,
                valoresUsuario(usuarioGuardado),
                "Cambio de estado de usuario");
        return aRespuesta(usuarioGuardado);
    }

    @Transactional
    public void restablecerContrasena(
            UUID idUsuario,
            RestablecerContrasenaUsuarioRequest request,
            PrincipalUsuario principalUsuario) {
        validarGerente(principalUsuario);
        Usuario usuario = obtenerUsuario(idUsuario);
        CredencialUsuario credencial = credencialUsuarioRepository.findByUsuario_IdUsuario(usuario.getIdUsuario())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Credencial de usuario no encontrada"));
        OffsetDateTime ahora = OffsetDateTime.now();
        var valorAnterior = valoresCredencial(credencial);

        credencial.setContrasenaHash(passwordEncoder.encode(request.nuevaContrasena()));
        credencial.setIntentosFallidos(0);
        credencial.setFechaCambioContrasena(ahora);
        CredencialUsuario credencialGuardada = credencialUsuarioRepository.saveAndFlush(credencial);
        revocarSesionesActivas(usuario.getIdUsuario(), principalUsuario, ahora);

        auditoriaService.registrar(
                principalUsuario.idUsuario(),
                "credenciales_usuario",
                credencialGuardada.getIdCredencialUsuario(),
                "editar",
                valorAnterior,
                valoresCredencial(credencialGuardada),
                "Cambio de contrasena por gerente");
    }

    private void validarGerente(PrincipalUsuario principalUsuario) {
        if (!ROL_GERENTE.equals(principalUsuario.nombreRol())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Solo el gerente puede gestionar usuarios");
        }
    }

    private Usuario obtenerUsuario(UUID idUsuario) {
        return usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
    }

    private Rol obtenerRolActivo(String nombreRol) {
        String rolNormalizado = normalizarTexto(nombreRol).toLowerCase(Locale.ROOT);
        Rol rol = rolRepository.findByNombreRol(rolNormalizado)
                .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "El rol no existe"));
        if (!"activo".equals(rol.getEstado())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "El rol debe estar activo");
        }
        return rol;
    }

    private void validarNombreUsuarioDisponible(String nombreUsuario, UUID idUsuarioActual) {
        usuarioRepository.findByNombreUsuario(nombreUsuario).ifPresent(usuario -> {
            if (!usuario.getIdUsuario().equals(idUsuarioActual)) {
                throw new ApiException(HttpStatus.CONFLICT, "El nombre de usuario ya existe");
            }
        });
    }

    private String normalizarNombreUsuario(String nombreUsuario) {
        return nombreUsuario.trim();
    }

    private String normalizarTexto(String valor) {
        return valor.trim();
    }

    private UsuarioGestionResponse aRespuesta(Usuario usuario) {
        return new UsuarioGestionResponse(
                usuario.getIdUsuario(),
                usuario.getNombreUsuario(),
                usuario.getNombreCompleto(),
                usuario.getRol().getIdRol(),
                usuario.getRol().getNombreRol(),
                usuario.getEstado(),
                usuario.getFechaCreacion(),
                usuario.getFechaActualizacion());
    }

    private void revocarSesionesActivas(
            UUID idUsuario,
            PrincipalUsuario principalUsuario,
            OffsetDateTime fechaRevocacion) {
        List<SesionUsuario> sesiones = sesionUsuarioRepository.findAllByUsuario_IdUsuario(idUsuario);
        sesiones.stream()
                .filter(sesion -> "activa".equals(sesion.getEstadoSesion()))
                .filter(sesion -> !sesion.getTokenIdentificador().equals(principalUsuario.tokenIdentificador()))
                .forEach(sesion -> {
                    sesion.setEstadoSesion("revocada");
                    sesion.setFechaCierre(fechaRevocacion);
                });
        sesionUsuarioRepository.saveAll(sesiones);
    }

    private java.util.Map<String, Object> valoresCredencial(CredencialUsuario credencial) {
        return valores(
                "id_usuario", credencial.getUsuario().getIdUsuario(),
                "requiere_cambio_contrasena", credencial.isRequiereCambioContrasena(),
                "intentos_fallidos", credencial.getIntentosFallidos(),
                "fecha_cambio_contrasena", credencial.getFechaCambioContrasena(),
                "estado", credencial.getEstado());
    }

    private java.util.Map<String, Object> valoresUsuario(Usuario usuario) {
        return valores(
                "id_usuario", usuario.getIdUsuario(),
                "nombre_usuario", usuario.getNombreUsuario(),
                "nombre_completo", usuario.getNombreCompleto(),
                "nombre_rol", usuario.getRol().getNombreRol(),
                "estado", usuario.getEstado());
    }
}
