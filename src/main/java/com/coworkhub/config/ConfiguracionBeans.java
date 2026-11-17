package com.coworkhub.config;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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
     * Valida la configuración al arrancar: si el reloj fijo está mal escrito, la aplicación no
     * arranca y lo dice claramente, en vez de fallar en la primera reserva.
     */
    @PostConstruct
    void validarConfiguracion() {
        if (relojFijo.isEmpty()) {
            return;
        }
        try {
            instanteFijo(ZoneId.systemDefault());
        } catch (DateTimeParseException e) {
            throw new IllegalStateException("coworkhub.reloj.fijo debe ser yyyy-MM-ddTHH:mm:ss o HH:mm, pero vale '"
                    + relojFijo + "'", e);
        }
    }

    /**
     * Instante del reloj fijo. Acepta una fecha y hora completas (2030-06-03T08:00:00) o solo una
     * hora (08:00), que se toma como "hoy a esa hora": así coincide con las fechas de import.sql,
     * que también son relativas a hoy, y las pruebas de aceptación sirven cualquier día.
     */
    private LocalDateTime instanteFijo(ZoneId zona) {
        if (relojFijo.contains("T")) {
            return LocalDateTime.parse(relojFijo);
        }
        return LocalDate.now(zona).atTime(LocalTime.parse(relojFijo));
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
        return Clock.fixed(instanteFijo(zona).atZone(zona).toInstant(), zona);
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
