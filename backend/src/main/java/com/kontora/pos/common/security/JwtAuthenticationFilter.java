package com.kontora.pos.common.security;

import com.kontora.pos.usuarios.domain.SesionUsuario;
import com.kontora.pos.usuarios.domain.Usuario;
import com.kontora.pos.usuarios.repository.SesionUsuarioRepository;
import com.kontora.pos.usuarios.service.JwtService;
import com.kontora.pos.usuarios.service.JwtService.TokenValidado;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final SesionUsuarioRepository sesionUsuarioRepository;

    public JwtAuthenticationFilter(JwtService jwtService, SesionUsuarioRepository sesionUsuarioRepository) {
        this.jwtService = jwtService;
        this.sesionUsuarioRepository = sesionUsuarioRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith("Bearer ")) {
            autenticarConToken(authorization.substring(7));
        }

        filterChain.doFilter(request, response);
    }

    private void autenticarConToken(String token) {
        try {
            TokenValidado tokenValidado = jwtService.validarToken(token);
            sesionUsuarioRepository.findByTokenIdentificador(tokenValidado.tokenIdentificador())
                    .filter(this::sesionActiva)
                    .map(SesionUsuario::getUsuario)
                    .filter(usuario -> "activo".equals(usuario.getEstado()))
                    .ifPresent(usuario -> SecurityContextHolder.getContext()
                            .setAuthentication(crearAuthentication(usuario, tokenValidado.tokenIdentificador())));
        } catch (RuntimeException exception) {
            SecurityContextHolder.clearContext();
        }
    }

    private boolean sesionActiva(SesionUsuario sesion) {
        return "activa".equals(sesion.getEstadoSesion())
                && sesion.getFechaExpiracion().isAfter(OffsetDateTime.now());
    }

    private UsernamePasswordAuthenticationToken crearAuthentication(Usuario usuario, String tokenIdentificador) {
        String nombreRol = usuario.getRol().getNombreRol();
        PrincipalUsuario principal = new PrincipalUsuario(
                usuario.getIdUsuario(),
                usuario.getNombreUsuario(),
                usuario.getNombreCompleto(),
                nombreRol,
                tokenIdentificador);
        List<SimpleGrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_" + nombreRol.toUpperCase()));
        return new UsernamePasswordAuthenticationToken(principal, null, authorities);
    }
}

