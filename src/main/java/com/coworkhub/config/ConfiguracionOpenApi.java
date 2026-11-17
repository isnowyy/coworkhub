package com.coworkhub.config;

import java.util.Map;
import java.util.Set;

import org.springdoc.core.customizers.GlobalOpenApiCustomizer;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

/**
 * Documentación de la API (Swagger UI en /doc/swagger-ui.html): información general, esquema
 * de seguridad JWT (botón Authorize), grupos y respuestas de error comunes en cada operación.
 */
@Configuration
public class ConfiguracionOpenApi {

    public static final String ESQUEMA_JWT = "bearerAuth";
    private static final String REF_ERROR = "#/components/schemas/RespuestaError";

    /** Rutas públicas: no llevan candado ni respuestas 401/403. */
    private static final Set<String> RUTAS_PUBLICAS = Set.of("/auth/login");

    @Bean
    public OpenAPI openApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("CoWorkHub API")
                        .version("1.0.0")
                        .description("""
                                API REST de reservas para una red de coworking: sedes, salas, planes, \
                                miembros, disponibilidad y reservas con sus reglas de negocio (RN-01 a RN-13).

                                **Cómo probarla:** ejecuta `POST /auth/login` con un usuario de prueba \
                                (`admin`/`admin123`, `recepcion`/`recep123`, `ana`/`ana123`, `luis`/`luis123`), \
                                copia el `token` y pégalo en el botón **Authorize**.

                                Todo error responde `{"error", "codigo", "timestamp"}`, donde `codigo` es la \
                                regla violada (RN-01…) o el tipo de error.""")
                        .contact(new Contact().name("Proyecto final Spring Boot R2")))
                .components(new Components()
                        .addSecuritySchemes(ESQUEMA_JWT, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Token obtenido en POST /auth/login. Vence a los 60 minutos.")))
                .addSecurityItem(new SecurityRequirement().addList(ESQUEMA_JWT));
    }

    // ---------------------------------------------------------------- grupos (selector arriba a la derecha)

    @Bean
    public GroupedOpenApi grupoCompleto() {
        return GroupedOpenApi.builder().group("0-completa").displayName("API completa")
                .pathsToMatch("/auth/**", "/api/**").build();
    }

    @Bean
    public GroupedOpenApi grupoAutenticacion() {
        return GroupedOpenApi.builder().group("1-autenticacion").displayName("Autenticación")
                .pathsToMatch("/auth/**").build();
    }

    @Bean
    public GroupedOpenApi grupoCatalogos() {
        return GroupedOpenApi.builder().group("2-catalogos").displayName("Catálogos")
                .pathsToMatch("/api/sedes/**", "/api/salas/**", "/api/equipamientos/**",
                        "/api/servicios-adicionales/**", "/api/planes/**")
                .build();
    }

    @Bean
    public GroupedOpenApi grupoMiembros() {
        return GroupedOpenApi.builder().group("3-miembros").displayName("Miembros y consumo")
                .pathsToMatch("/api/miembros/**").build();
    }

    @Bean
    public GroupedOpenApi grupoReservas() {
        return GroupedOpenApi.builder().group("4-reservas").displayName("Disponibilidad y reservas")
                .pathsToMatch("/api/disponibilidad/**", "/api/reservas/**").build();
    }

    // ---------------------------------------------------------------- errores comunes

    /**
     * Agrega a cada operación las respuestas de error con el cuerpo estándar y un ejemplo, sin
     * repetir @ApiResponse en cada método. Se aplica a todos los grupos (GlobalOpenApiCustomizer).
     */
    @Bean
    public GlobalOpenApiCustomizer respuestasDeErrorComunes() {
        return openApi -> {
            openApi.getComponents().addSchemas("RespuestaError", new Schema<>()
                    .type("object")
                    .description("Cuerpo único de todos los errores")
                    .addProperty("error", new Schema<String>().type("string").example("La sala ya está reservada en ese horario"))
                    .addProperty("codigo", new Schema<String>().type("string").example("RN-01"))
                    .addProperty("timestamp", new Schema<String>().type("string").format("date-time")
                            .example("2030-06-03T13:00:00Z")));
            if (openApi.getPaths() == null) {
                return;
            }
            openApi.getPaths().forEach((ruta, item) -> item.readOperationsMap().forEach((metodo, operacion) -> {
                agregar(operacion, "400", "Datos faltantes o con formato inválido", "nombre: El nombre es obligatorio", "VALIDACION");
                if (RUTAS_PUBLICAS.contains(ruta)) {
                    agregar(operacion, "401", "Usuario o contraseña incorrectos", "Usuario o contraseña incorrectos", "NO_AUTENTICADO");
                    operacion.setSecurity(java.util.List.of());
                    return;
                }
                agregar(operacion, "401", "Sin token, o con un token alterado o vencido", "Debes iniciar sesión con un token válido", "NO_AUTENTICADO");
                agregar(operacion, "403", "El usuario no tiene permiso", "No tienes permiso para esta operación", "ACCESO_DENEGADO");
                if (ruta.contains("{")) {
                    agregar(operacion, "404", "El recurso no existe", "Reserva con id 99 no existe", "NO_ENCONTRADO");
                }
                if (!"GET".equals(metodo.name())) {
                    agregar(operacion, "409", "Se viola una regla de negocio o hay un duplicado", "La sala ya está reservada en ese horario", "RN-01");
                }
            }));
        };
    }

    private static void agregar(Operation operacion, String codigo, String descripcion, String mensaje, String tipo) {
        ApiResponses respuestas = operacion.getResponses();
        if (respuestas == null || respuestas.containsKey(codigo)) {
            return;
        }
        MediaType json = new MediaType()
                .schema(new Schema<>().$ref(REF_ERROR))
                .example(Map.of("error", mensaje, "codigo", tipo, "timestamp", "2030-06-03T13:00:00Z"));
        respuestas.addApiResponse(codigo, new ApiResponse().description(descripcion)
                .content(new Content().addMediaType("application/json", json)));
    }
}
