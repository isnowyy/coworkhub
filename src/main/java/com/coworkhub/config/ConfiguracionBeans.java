package com.coworkhub.config;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import jakarta.annotation.PostConstruct;

/**
 * Beans de clases que no son nuestras (Clock y PasswordEncoder), declarados con @Bean.
 */
@Configuration
public class ConfiguracionBeans {

    /** Nombre del reloj que siempre marca la hora real (lo usa el JWT en la fase 8). */
    public static final String RELOJ_REAL = "relojReal";

    private final String relojFijo;

    public ConfiguracionBeans(@Value("${coworkhub.reloj.fijo:}") String relojFijo) {
        this.relojFijo = relojFijo.trim();
    }

    /**
     * Valida la configuración al arrancar: si la fecha del reloj fijo está mal escrita,
     * la aplicación no arranca y lo dice claramente, en vez de fallar en la primera reserva.
     */
    @PostConstruct
    void validarConfiguracion() {
        if (relojFijo.isEmpty()) {
            return;
        }
        try {
            LocalDateTime.parse(relojFijo);
        } catch (DateTimeParseException e) {
            throw new IllegalStateException(
                    "coworkhub.reloj.fijo debe tener el formato yyyy-MM-ddTHH:mm:ss, pero vale '" + relojFijo + "'", e);
        }
    }

    /**
     * Reloj de negocio: el "ahora" de las reglas RN-03 y RN-08. Es el que se inyecta por
     * defecto (@Primary). Si coworkhub.reloj.fijo tiene valor, queda congelado en ese instante.
     */
    @Bean
    @Primary
    public Clock reloj() {
        ZoneId zona = ZoneId.systemDefault();
        if (relojFijo.isEmpty()) {
            return Clock.system(zona);
        }
        return Clock.fixed(LocalDateTime.parse(relojFijo).atZone(zona).toInstant(), zona);
    }

    /**
     * Reloj que siempre marca la hora real. Se pide con @Qualifier(RELOJ_REAL): un token
     * debe vencer en tiempo real aunque el reloj de negocio esté congelado para las pruebas.
     */
    @Bean(RELOJ_REAL)
    public Clock relojReal() {
        return Clock.systemDefaultZone();
    }

    /** BCrypt: las contraseñas nunca se guardan en texto plano. */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
