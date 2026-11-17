package com.coworkhub.presentacion.controlador;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.coworkhub.config.UsuarioAutenticado;
import com.coworkhub.jwt.UtilJwt;
import com.coworkhub.negocio.service.ServicioAutenticacion;
import com.coworkhub.presentacion.dto.RespuestaLogin;
import com.coworkhub.presentacion.dto.RespuestaPerfil;
import com.coworkhub.presentacion.dto.SolicitudLogin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/auth")
@Tag(name = "Autenticación", description = "Inicio de sesión con JWT y datos del usuario actual.")
public class ControladorAutenticacion {

    private final ServicioAutenticacion servicioAutenticacion;
    private final UtilJwt utilJwt;

    public ControladorAutenticacion(ServicioAutenticacion servicioAutenticacion, UtilJwt utilJwt) {
        this.servicioAutenticacion = servicioAutenticacion;
        this.utilJwt = utilJwt;
    }

    @PostMapping("/login")
    @Operation(summary = "Iniciar sesión", description = "RF-11. Público. Devuelve un JWT que vence en 60 minutos. "
            + "Usuarios de prueba: admin/admin123, recepcion/recep123, ana/ana123, luis/luis123.")
    public RespuestaLogin login(@Valid @RequestBody SolicitudLogin solicitud) {
        UsuarioAutenticado usuario = servicioAutenticacion.autenticar(solicitud.usuario(), solicitud.contrasena());
        UtilJwt.TokenEmitido emitido = utilJwt.generar(usuario);
        return new RespuestaLogin(emitido.token(), "Bearer", emitido.expiraEn(), usuario.username(), usuario.rol(),
                usuario.miembroId());
    }

    @GetMapping("/perfil")
    @Operation(summary = "Usuario actual", description = "RF-14. Cualquier usuario autenticado: usuario, rol y miembroId del token.")
    public RespuestaPerfil perfil(@Parameter(hidden = true) @AuthenticationPrincipal UsuarioAutenticado usuario) {
        return RespuestaPerfil.desde(usuario);
    }
}
