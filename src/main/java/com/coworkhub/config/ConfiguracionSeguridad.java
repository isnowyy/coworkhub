package com.coworkhub.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.coworkhub.jwt.FiltroAutenticacionJwt;
import com.coworkhub.jwt.UtilJwt;

/**
 * Reglas de acceso (RN-13). Por URL: lo público y la escritura de catálogos (solo ADMIN). Lo que
 * depende del rol y del dueño del recurso (miembros y reservas) va con @PreAuthorize en los
 * controladores. Sin sesión (stateless): cada solicitud trae su JWT, que valida FiltroAutenticacionJwt.
 */
@Configuration
@EnableMethodSecurity
public class ConfiguracionSeguridad {

    static final String[] RUTAS_PUBLICAS = {
        "/auth/login", "/actuator/health", "/doc/**", "/v3/api-docs/**", "/swagger-ui/**", "/error"
    };

    static final String[] CATALOGOS = {
        "/api/sedes/**", "/api/salas/**", "/api/equipamientos/**", "/api/servicios-adicionales/**", "/api/planes/**"
    };

    private final PuntoEntradaNoAutenticado puntoEntrada;
    private final ManejadorAccesoDenegado accesoDenegado;
    private final UtilJwt utilJwt;

    public ConfiguracionSeguridad(PuntoEntradaNoAutenticado puntoEntrada, ManejadorAccesoDenegado accesoDenegado,
                                  UtilJwt utilJwt) {
        this.puntoEntrada = puntoEntrada;
        this.accesoDenegado = accesoDenegado;
        this.utilJwt = utilJwt;
    }

    @Bean
    public SecurityFilterChain cadenaDeSeguridad(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable) // API sin cookies de sesión: CSRF no aplica
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(a -> a
                        .requestMatchers(RUTAS_PUBLICAS).permitAll()
                        .requestMatchers(HttpMethod.POST, CATALOGOS).hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, CATALOGOS).hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, CATALOGOS).hasRole("ADMIN")
                        .anyRequest().authenticated())
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .addFilterBefore(new FiltroAutenticacionJwt(utilJwt), UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(e -> e.authenticationEntryPoint(puntoEntrada).accessDeniedHandler(accesoDenegado))
                .build();
    }
}
