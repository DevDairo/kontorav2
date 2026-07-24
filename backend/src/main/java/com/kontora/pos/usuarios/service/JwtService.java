package com.kontora.pos.usuarios.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kontora.pos.common.exception.ApiException;
import com.kontora.pos.usuarios.domain.Usuario;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class JwtService {

    private static final String ALGORITHM = "HmacSHA256";
    private static final String JWT_ALGORITHM = "HS256";
    private static final String DEV_SECRET = "kontora-pos-dev-secret-change-me-32-characters";

    private final ObjectMapper objectMapper;
    private final byte[] secret;
    private final long expirationMinutes;

    public JwtService(
            ObjectMapper objectMapper,
            @Value("${JWT_SECRET:}") String jwtSecret,
            @Value("${JWT_EXPIRATION_MINUTES:60}") long expirationMinutes) {
        this.objectMapper = objectMapper;
        this.secret = (jwtSecret == null || jwtSecret.isBlank() ? DEV_SECRET : jwtSecret)
                .getBytes(StandardCharsets.UTF_8);
        this.expirationMinutes = expirationMinutes;
    }

    public TokenGenerado generarToken(Usuario usuario) {
        Instant fechaInicio = Instant.now();
        Instant fechaExpiracion = fechaInicio.plusSeconds(expirationMinutes * 60);
        String tokenIdentificador = UUID.randomUUID().toString();
        Map<String, Object> claims = new LinkedHashMap<>();
        claims.put("sub", usuario.getIdUsuario().toString());
        claims.put("nombre_usuario", usuario.getNombreUsuario());
        claims.put("rol", usuario.getRol().getNombreRol());
        claims.put("jti", tokenIdentificador);
        claims.put("iat", fechaInicio.getEpochSecond());
        claims.put("exp", fechaExpiracion.getEpochSecond());

        return new TokenGenerado(
                firmar(claims),
                tokenIdentificador,
                fechaInicio,
                fechaExpiracion,
                expirationMinutes);
    }

    public TokenValidado validarToken(String token) {
        String[] partes = token.split("\\.");
        if (partes.length != 3) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Token invalido");
        }

        String firmaEsperada = hmac(partes[0] + "." + partes[1]);
        if (!MessageDigestSupport.equals(firmaEsperada, partes[2])) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Token invalido");
        }

        Map<String, Object> claims = leerJson(partes[1]);
        Instant fechaExpiracion = Instant.ofEpochSecond(numero(claims.get("exp")));
        if (!fechaExpiracion.isAfter(Instant.now())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Token expirado");
        }

        return new TokenValidado(
                UUID.fromString(texto(claims.get("sub"))),
                texto(claims.get("nombre_usuario")),
                texto(claims.get("rol")),
                texto(claims.get("jti")),
                fechaExpiracion);
    }

    private String firmar(Map<String, Object> claims) {
        Map<String, Object> header = Map.of("alg", JWT_ALGORITHM, "typ", "JWT");
        String encodedHeader = encodeJson(header);
        String encodedPayload = encodeJson(claims);
        String firma = hmac(encodedHeader + "." + encodedPayload);
        return encodedHeader + "." + encodedPayload + "." + firma;
    }

    private String encodeJson(Object value) {
        try {
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(objectMapper.writeValueAsBytes(value));
        } catch (Exception exception) {
            throw new IllegalStateException("No fue posible generar el token", exception);
        }
    }

    private Map<String, Object> leerJson(String encodedPayload) {
        try {
            byte[] decoded = Base64.getUrlDecoder().decode(encodedPayload);
            return objectMapper.readValue(decoded, new TypeReference<>() {
            });
        } catch (Exception exception) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Token invalido");
        }
    }

    private String hmac(String data) {
        try {
            Mac mac = Mac.getInstance(ALGORITHM);
            mac.init(new SecretKeySpec(secret, ALGORITHM));
            byte[] signature = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(signature);
        } catch (Exception exception) {
            throw new IllegalStateException("No fue posible firmar el token", exception);
        }
    }

    private String texto(Object value) {
        if (value == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Token invalido");
        }
        return value.toString();
    }

    private long numero(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        throw new ApiException(HttpStatus.UNAUTHORIZED, "Token invalido");
    }

    public record TokenGenerado(
            String token,
            String tokenIdentificador,
            Instant fechaInicio,
            Instant fechaExpiracion,
            long expiraEnMinutos
    ) {
    }

    public record TokenValidado(
            UUID idUsuario,
            String nombreUsuario,
            String nombreRol,
            String tokenIdentificador,
            Instant fechaExpiracion
    ) {
    }

    private static final class MessageDigestSupport {

        private MessageDigestSupport() {
        }

        private static boolean equals(String expected, String actual) {
            return java.security.MessageDigest.isEqual(
                    expected.getBytes(StandardCharsets.UTF_8),
                    actual.getBytes(StandardCharsets.UTF_8));
        }
    }
}

