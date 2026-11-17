package com.coworkhub.negocio.service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import com.coworkhub.negocio.excepcion.RecursoNoEncontradoException;
import com.coworkhub.negocio.excepcion.ReglaNegocioException;
import com.coworkhub.negocio.excepcion.SolicitudInvalidaException;
import com.coworkhub.negocio.model.AdditionalService;
import com.coworkhub.negocio.model.Booking;
import com.coworkhub.negocio.model.BookingItem;
import com.coworkhub.negocio.model.BookingStatus;
import com.coworkhub.negocio.model.Location;
import com.coworkhub.negocio.model.Member;
import com.coworkhub.negocio.model.MemberStatus;
import com.coworkhub.negocio.model.Room;
import com.coworkhub.persistence.repository.BookingRepository;
import com.coworkhub.persistence.repository.MemberRepository;
import com.coworkhub.persistence.repository.RoomRepository;
import com.coworkhub.presentacion.dto.ItemServicio;
import com.coworkhub.presentacion.dto.SolicitudReserva;

/** Reglas de las reservas: RN-01 a RN-10. */
@Service
public class ServicioReservasImpl implements ServicioReservas {

    static final Duration DURACION_MINIMA = Duration.ofMinutes(30);
    static final Duration DURACION_MAXIMA = Duration.ofHours(8);
    static final Duration ANTICIPACION_MINIMA = Duration.ofHours(1);
    static final Duration CANCELACION_SIN_CARGO = Duration.ofHours(24);
    static final Duration CANCELACION_MINIMA = Duration.ofHours(2);

    private final BookingRepository reservas;
    private final RoomRepository salas;
    private final MemberRepository miembros;
    private final ServicioServiciosAdicionales servicioServicios;
    private final ServicioConsumo servicioConsumo;
    private final CalculadoraCostoReserva calculadora;
    private final Clock reloj;

    public ServicioReservasImpl(BookingRepository reservas, RoomRepository salas, MemberRepository miembros,
                                ServicioServiciosAdicionales servicioServicios, ServicioConsumo servicioConsumo,
                                CalculadoraCostoReserva calculadora, Clock reloj) {
        this.reservas = reservas;
        this.salas = salas;
        this.miembros = miembros;
        this.servicioServicios = servicioServicios;
        this.servicioConsumo = servicioConsumo;
        this.calculadora = calculadora;
        this.reloj = reloj;
    }

    // ---------------------------------------------------------------- crear (RF-04)

    /**
     * Concurrencia (RN-01): la fila de la sala se bloquea (SELECT ... FOR UPDATE) hasta el final de
     * la transacción, así dos solicitudes para la misma sala se procesan una tras otra. El
     * aislamiento READ_COMMITTED es imprescindible: con el REPEATABLE READ por defecto de MySQL, la
     * segunda transacción, al obtener el bloqueo, no vería la reserva que la primera acaba de
     * confirmar y las dos pasarían la verificación de solapamiento.
     */
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Booking crear(SolicitudReserva s) {
        Room sala = salas.findForBooking(s.salaId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Sala", s.salaId()));
        Member miembro = miembros.findWithDetailsById(s.miembroId())
                .orElseThrow(() -> new RecursoNoEncontradoException("Miembro", s.miembroId()));
        LocalDateTime ahora = LocalDateTime.now(reloj);

        validarMiembroActivo(miembro);                                   // RN-05
        validarSala(sala, s.asistentes());                               // RN-04
        long minutos = validarDuracion(s.inicio(), s.fin());             // RN-02
        validarHorario(sala.getLocation(), s.inicio(), s.fin(), ahora);  // RN-03
        validarSinCruce(sala, s.inicio(), s.fin());                      // RN-01
        validarLimiteDelPlan(miembro, ahora);                            // RN-06

        Booking reserva = new Booking(sala, miembro, s.inicio(), s.fin(), s.asistentes());
        if (s.servicios() != null) {
            s.servicios().forEach(item -> agregarOSumar(reserva, item));
        }

        long yaUsados = servicioConsumo.minutosConsumidos(miembro.getId(), YearMonth.from(s.inicio()));
        CalculadoraCostoReserva.CostoSala costo = calculadora.costoSala(sala, miembro.getPlan(), minutos, yaUsados);
        reserva.setConsumedMinutes(costo.minutosConsumidos());
        reserva.setRoomCost(costo.costo());
        recalcularTotales(reserva);
        return reservas.save(reserva);
    }

    // ---------------------------------------------------------------- consultas (RF-05)

    @Override
    @Transactional(readOnly = true)
    public Booking buscar(Long id) {
        return reservas.findWithDetailsById(id).orElseThrow(() -> new RecursoNoEncontradoException("Reserva", id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Booking> listar(Long miembroId, Long salaId, LocalDate fecha, BookingStatus estado) {
        boolean porMiembro = miembroId != null;
        boolean porSala = salaId != null || fecha != null;
        boolean porEstado = estado != null;
        int filtros = (porMiembro ? 1 : 0) + (porSala ? 1 : 0) + (porEstado ? 1 : 0);
        if (filtros != 1) {
            throw new SolicitudInvalidaException(
                    "Indica exactamente un filtro: miembroId, salaId y fecha, o estado");
        }
        if (porMiembro) {
            return reservas.findByMemberIdOrderByStartTimeAsc(miembroId);
        }
        if (porSala) {
            if (salaId == null || fecha == null) {
                throw new SolicitudInvalidaException("Para filtrar por sala indica salaId y fecha");
            }
            return reservas.findByRoomIdAndStartTimeGreaterThanEqualAndStartTimeLessThanOrderByStartTimeAsc(
                    salaId, fecha.atStartOfDay(), fecha.plusDays(1).atStartOfDay());
        }
        return reservas.findByStatusOrderByStartTimeAsc(estado);
    }

    // ---------------------------------------------------------------- servicios (RF-06)

    @Override
    @Transactional
    public Booking agregarServicio(Long reservaId, ItemServicio item) {
        Booking reserva = buscar(reservaId);
        exigirPendienteParaServicios(reserva);
        agregarOSumar(reserva, item);
        recalcularTotales(reserva);
        return reserva;
    }

    @Override
    @Transactional
    public Booking quitarServicio(Long reservaId, Long servicioId) {
        Booking reserva = buscar(reservaId);
        exigirPendienteParaServicios(reserva);
        BookingItem item = reserva.getItems().stream()
                .filter(i -> i.getService().getId().equals(servicioId))
                .findFirst()
                .orElseThrow(() -> new RecursoNoEncontradoException("Servicio en la reserva", servicioId));
        reserva.removeItem(item); // orphanRemoval borra el detalle de la base al confirmar
        recalcularTotales(reserva);
        return reserva;
    }

    // ---------------------------------------------------------------- estados (RF-07, RF-08, RF-09)

    @Override
    @Transactional
    public Booking confirmar(Long id) {
        Booking reserva = buscar(id);
        exigirEstado(reserva, BookingStatus.PENDING, BookingStatus.CONFIRMED);
        reserva.setStatus(BookingStatus.CONFIRMED);
        return reserva;
    }

    @Override
    @Transactional
    public Booking completar(Long id) {
        Booking reserva = buscar(id);
        exigirEstado(reserva, BookingStatus.CONFIRMED, BookingStatus.COMPLETED);
        reserva.setStatus(BookingStatus.COMPLETED);
        return reserva;
    }

    /**
     * RN-08: con 24 h o más de anticipación, sin cargo y se devuelven las horas del plan; entre
     * 24 h y 2 h, se cobra el 50 % del costo de la sala y las horas no se devuelven; con menos de
     * 2 h, o si ya empezó, no se puede cancelar. Límites: 24 h justas = sin cargo; 2 h justas = con cargo.
     */
    @Override
    @Transactional
    public Booking cancelar(Long id) {
        Booking reserva = buscar(id);
        if (!BookingStatus.ACTIVE.contains(reserva.getStatus())) {
            throw new ReglaNegocioException("RN-09",
                    "Solo se pueden cancelar reservas PENDING o CONFIRMED (estado actual: " + reserva.getStatus() + ")");
        }
        LocalDateTime ahora = LocalDateTime.now(reloj);
        if (!ahora.isBefore(reserva.getStartTime())) {
            throw new ReglaNegocioException("RN-08", "La reserva ya comenzó y no se puede cancelar");
        }
        Duration anticipacion = Duration.between(ahora, reserva.getStartTime());
        if (anticipacion.compareTo(CANCELACION_MINIMA) < 0) {
            throw new ReglaNegocioException("RN-08", "No se puede cancelar con menos de 2 horas de anticipación");
        }

        BigDecimal cargo = BigDecimal.ZERO.setScale(2);
        if (anticipacion.compareTo(CANCELACION_SIN_CARGO) >= 0) {
            reserva.setConsumedMinutes(0); // se devuelven las horas del plan
        } else {
            cargo = calculadora.cargoCancelacionTardia(reserva.getRoomCost());
        }
        reserva.setStatus(BookingStatus.CANCELLED);
        reserva.setCancellationFee(cargo);
        reserva.setTotalCost(cargo); // de una reserva cancelada solo se cobra el cargo
        return reserva;
    }

    // ---------------------------------------------------------------- reglas (privadas)

    // RN-05
    private static void validarMiembroActivo(Member miembro) {
        if (miembro.getStatus() != MemberStatus.ACTIVE) {
            throw new ReglaNegocioException("RN-05", "El miembro está suspendido y no puede reservar");
        }
    }

    // RN-04
    private static void validarSala(Room sala, int asistentes) {
        if (!sala.isActive()) {
            throw new ReglaNegocioException("RN-04", "La sala '" + sala.getName() + "' está inactiva");
        }
        if (asistentes > sala.getCapacity()) {
            throw new ReglaNegocioException("RN-04", "Los asistentes (" + asistentes
                    + ") superan la capacidad de la sala (" + sala.getCapacity() + ")");
        }
    }

    // RN-02: devuelve la duración en minutos
    private static long validarDuracion(LocalDateTime inicio, LocalDateTime fin) {
        if (!fin.isAfter(inicio)) {
            throw new ReglaNegocioException("RN-02", "El fin debe ser posterior al inicio");
        }
        if (!inicio.toLocalDate().equals(fin.toLocalDate())) {
            throw new ReglaNegocioException("RN-02", "La reserva debe empezar y terminar el mismo día");
        }
        Duration duracion = Duration.between(inicio, fin);
        if (duracion.compareTo(DURACION_MINIMA) < 0 || duracion.compareTo(DURACION_MAXIMA) > 0
                || duracion.toSeconds() % DURACION_MINIMA.toSeconds() != 0) {
            throw new ReglaNegocioException("RN-02",
                    "La reserva debe durar entre 30 minutos y 8 horas, en múltiplos de 30 minutos");
        }
        return duracion.toMinutes();
    }

    // RN-03
    private static void validarHorario(Location sede, LocalDateTime inicio, LocalDateTime fin, LocalDateTime ahora) {
        if (inicio.toLocalTime().isBefore(sede.getOpeningTime()) || fin.toLocalTime().isAfter(sede.getClosingTime())) {
            throw new ReglaNegocioException("RN-03", "La reserva debe estar dentro del horario de la sede ("
                    + sede.getOpeningTime() + " a " + sede.getClosingTime() + ")");
        }
        if (inicio.isBefore(ahora.plus(ANTICIPACION_MINIMA))) {
            throw new ReglaNegocioException("RN-03",
                    "La reserva debe ser futura y hacerse con al menos 1 hora de anticipación");
        }
    }

    // RN-01
    private void validarSinCruce(Room sala, LocalDateTime inicio, LocalDateTime fin) {
        if (reservas.existsOverlap(sala.getId(), inicio, fin, BookingStatus.ACTIVE)) {
            throw new ReglaNegocioException("RN-01", "La sala ya está reservada en ese horario");
        }
    }

    // RN-06
    private void validarLimiteDelPlan(Member miembro, LocalDateTime ahora) {
        long activas = reservas.countByMemberIdAndStatusInAndStartTimeAfter(miembro.getId(), BookingStatus.ACTIVE, ahora);
        int maximo = miembro.getPlan().getMaxActiveBookings();
        if (activas >= maximo) {
            throw new ReglaNegocioException("RN-06", "El plan " + miembro.getPlan().getName() + " permite "
                    + maximo + " reservas activas futuras y el miembro ya tiene " + activas);
        }
    }

    // RN-09
    private static void exigirEstado(Booking reserva, BookingStatus requerido, BookingStatus destino) {
        if (reserva.getStatus() != requerido) {
            throw new ReglaNegocioException("RN-09", "No se puede pasar de " + reserva.getStatus() + " a " + destino
                    + "; solo se permite desde " + requerido);
        }
    }

    // RN-10
    private static void exigirPendienteParaServicios(Booking reserva) {
        if (reserva.getStatus() != BookingStatus.PENDING) {
            throw new ReglaNegocioException("RN-10",
                    "Los servicios solo se pueden cambiar mientras la reserva está PENDING (estado actual: "
                            + reserva.getStatus() + ")");
        }
    }

    /** Agrega el servicio con el precio vigente; si ya estaba, suma la cantidad y conserva su precio. */
    private void agregarOSumar(Booking reserva, ItemServicio item) {
        AdditionalService servicio = servicioServicios.buscar(item.servicioId());
        reserva.getItems().stream()
                .filter(i -> i.getService().getId().equals(servicio.getId()))
                .findFirst()
                .ifPresentOrElse(existente -> existente.addQuantity(item.cantidad()),
                        () -> reserva.addItem(new BookingItem(servicio, item.cantidad())));
    }

    /** RN-07: costo de servicios y total. El costo de la sala ya está calculado. */
    private void recalcularTotales(Booking reserva) {
        BigDecimal servicios = calculadora.costoServicios(reserva.getItems());
        reserva.setServicesCost(servicios);
        reserva.setTotalCost(reserva.getRoomCost().add(servicios));
    }
}
