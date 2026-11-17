package com.coworkhub.jwt;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.coworkhub.config.ConfiguracionBeans;
import com.coworkhub.config.UsuarioAutenticado;
import com.coworkhub.negocio.model.Role;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;

/**
 * Crea y valida los JWT. El token lleva el usuario (sub), su rol y su miembroId, firmado con
 * HMAC-SHA. Usa el reloj real (no el de negocio, que puede estar congelado para las pruebas).
 */
@Component
public class UtilJwt {

    private static final String CLAIM_ROL = "rol";
    private static final String CLAIM_MIEMBRO = "miembroId";
    private static final int LARGO_MINIMO_CLAVE = 32;

    private final String secreto;
    private final long minutos;
    private final Clock reloj;
    private SecretKey clave;

    public UtilJwt(@Value("${coworkhub.jwt.secreto}") String secreto,
                   @Value("${coworkhub.jwt.minutos:60}") long minutos,
                   @Qualifier(ConfiguracionBeans.RELOJ_REAL) Clock reloj) {
        this.secreto = secreto;
        this.minutos = minutos;
        this.reloj = reloj;
    }

    /** Valida la configuración al arrancar: una clave corta o una vigencia inválida detienen la app. */
    @PostConstruct
    void validarConfiguracion() {
        if (secreto == null || secreto.getBytes(StandardCharsets.UTF_8).length < LARGO_MINIMO_CLAVE) {
            throw new IllegalStateException("coworkhub.jwt.secreto debe tener al menos " + LARGO_MINIMO_CLAVE + " caracteres");
        }
        if (minutos <= 0) {
            throw new IllegalStateException("coworkhub.jwt.minutos debe ser mayor que 0");
        }
        this.clave = Keys.hmacShaKeyFor(secreto.getBytes(StandardCharsets.UTF_8));
    }

    public record TokenEmitido(String token, Instant expiraEn) {
    }

    public TokenEmitido generar(UsuarioAutenticado usuario) {
        Instant ahora = reloj.instant();
        Instant vence = ahora.plus(Duration.ofMinutes(minutos));
        var constructor = Jwts.builder()
                .subject(usuario.username())
                .claim(CLAIM_ROL, usuario.rol().name())
                .issuedAt(Date.from(ahora))
                .expiration(Date.from(vence))
                .signWith(clave);
        if (usuario.miembroId() != null) {
            constructor.claim(CLAIM_MIEMBRO, usuario.miembroId());
        }
        return new TokenEmitido(constructor.compact(), vence);
    }

    /** Devuelve el usuario del token, o vacío si el token está alterado, vencido o mal formado. */
    public Optional<UsuarioAutenticado> validar(String token) {
        try {
            Claims datos = Jwts.parser()
                    .verifyWith(clave)
                    .clock(() -> Date.from(reloj.instant()))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            Number miembro = datos.get(CLAIM_MIEMBRO, Number.class);
            return Optional.of(new UsuarioAutenticado(datos.getSubject(), null,
                    Role.valueOf(datos.get(CLAIM_ROL, String.class)),
                    miembro == null ? null : miembro.longValue()));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
