package com.coworkhub.presentacion.controlador;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.coworkhub.negocio.model.BookingStatus;
import com.coworkhub.negocio.service.ServicioReservas;
import com.coworkhub.presentacion.dto.ItemServicio;
import com.coworkhub.presentacion.dto.RespuestaReserva;
import com.coworkhub.presentacion.dto.SolicitudReserva;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/reservas")
@Tag(name = "Reservas", description = "Reservas con sus servicios, costos y estados (RN-01 a RN-10). Un MEMBER solo opera las suyas.")
public class ControladorReservas {

    private final ServicioReservas servicio;

    public ControladorReservas(ServicioReservas servicio) {
        this.servicio = servicio;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Crear una reserva", description = "RF-04. ADMIN y RECEPTIONIST; un MEMBER solo a su nombre. Aplica RN-01 a RN-07 y devuelve el costo calculado.")
    public RespuestaReserva crear(@Valid @RequestBody SolicitudReserva solicitud) {
        return RespuestaReserva.desde(servicio.crear(solicitud));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consultar una reserva", description = "RF-05. ADMIN y RECEPTIONIST; un MEMBER solo si es suya.")
    public RespuestaReserva buscar(@PathVariable Long id) {
        return RespuestaReserva.desde(servicio.buscar(id));
    }

    @GetMapping
    @Operation(summary = "Listar reservas con un filtro", description = "RF-05. Exactamente un filtro: miembroId, salaId + fecha, o estado. ADMIN y RECEPTIONIST; un MEMBER solo con su propio miembroId.")
    public List<RespuestaReserva> listar(
            @Parameter(description = "Por miembro", example = "1") @RequestParam(required = false) Long miembroId,
            @Parameter(description = "Por sala (con fecha)", example = "1") @RequestParam(required = false) Long salaId,
            @Parameter(description = "Fecha yyyy-MM-dd (con salaId)", example = "2026-12-01") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
            @Parameter(description = "Por estado", example = "PENDING") @RequestParam(required = false) BookingStatus estado) {
        return servicio.listar(miembroId, salaId, fecha, estado).stream().map(RespuestaReserva::desde).toList();
    }

    @PostMapping("/{id}/servicios")
    @Operation(summary = "Agregar un servicio", description = "RF-06. Solo con la reserva PENDING (RN-10). Si ya estaba, suma la cantidad. Devuelve la reserva con el costo recalculado.")
    public RespuestaReserva agregarServicio(@PathVariable Long id, @Valid @RequestBody ItemServicio item) {
        return RespuestaReserva.desde(servicio.agregarServicio(id, item));
    }

    /** DELETE responde 200 sin cuerpo; el costo recalculado se ve con GET /api/reservas/{id}. */
    @DeleteMapping("/{id}/servicios/{servicioId}")
    @Operation(summary = "Quitar un servicio", description = "RF-06. Solo con la reserva PENDING (RN-10). Borra el detalle y recalcula el costo. Responde 200 sin cuerpo.")
    public void quitarServicio(@PathVariable Long id, @PathVariable Long servicioId) {
        servicio.quitarServicio(id, servicioId);
    }

    @PostMapping("/{id}/confirmar")
    @Operation(summary = "Confirmar", description = "RF-07. ADMIN y RECEPTIONIST. Solo desde PENDING (RN-09).")
    public RespuestaReserva confirmar(@PathVariable Long id) {
        return RespuestaReserva.desde(servicio.confirmar(id));
    }

    @PostMapping("/{id}/cancelar")
    @Operation(summary = "Cancelar", description = "RF-08. ADMIN y RECEPTIONIST; un MEMBER solo las suyas. Con 24 h o más: sin cargo. Entre 24 h y 2 h: 50 % del costo de la sala. Con menos de 2 h o ya iniciada: 409 (RN-08).")
    public RespuestaReserva cancelar(@PathVariable Long id) {
        return RespuestaReserva.desde(servicio.cancelar(id));
    }

    @PostMapping("/{id}/completar")
    @Operation(summary = "Completar", description = "RF-09. ADMIN y RECEPTIONIST. Solo desde CONFIRMED (RN-09).")
    public RespuestaReserva completar(@PathVariable Long id) {
        return RespuestaReserva.desde(servicio.completar(id));
    }
}
