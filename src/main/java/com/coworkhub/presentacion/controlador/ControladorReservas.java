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

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/reservas")
public class ControladorReservas {

    private final ServicioReservas servicio;

    public ControladorReservas(ServicioReservas servicio) {
        this.servicio = servicio;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RespuestaReserva crear(@Valid @RequestBody SolicitudReserva solicitud) {
        return RespuestaReserva.desde(servicio.crear(solicitud));
    }

    @GetMapping("/{id}")
    public RespuestaReserva buscar(@PathVariable Long id) {
        return RespuestaReserva.desde(servicio.buscar(id));
    }

    @GetMapping
    public List<RespuestaReserva> listar(@RequestParam(required = false) Long miembroId,
                                         @RequestParam(required = false) Long salaId,
                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
                                         @RequestParam(required = false) BookingStatus estado) {
        return servicio.listar(miembroId, salaId, fecha, estado).stream().map(RespuestaReserva::desde).toList();
    }

    @PostMapping("/{id}/servicios")
    public RespuestaReserva agregarServicio(@PathVariable Long id, @Valid @RequestBody ItemServicio item) {
        return RespuestaReserva.desde(servicio.agregarServicio(id, item));
    }

    /** DELETE responde 200 sin cuerpo; el costo recalculado se ve con GET /api/reservas/{id}. */
    @DeleteMapping("/{id}/servicios/{servicioId}")
    public void quitarServicio(@PathVariable Long id, @PathVariable Long servicioId) {
        servicio.quitarServicio(id, servicioId);
    }

    @PostMapping("/{id}/confirmar")
    public RespuestaReserva confirmar(@PathVariable Long id) {
        return RespuestaReserva.desde(servicio.confirmar(id));
    }

    @PostMapping("/{id}/cancelar")
    public RespuestaReserva cancelar(@PathVariable Long id) {
        return RespuestaReserva.desde(servicio.cancelar(id));
    }

    @PostMapping("/{id}/completar")
    public RespuestaReserva completar(@PathVariable Long id) {
        return RespuestaReserva.desde(servicio.completar(id));
    }
}
