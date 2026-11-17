package com.coworkhub.negocio.service;

import java.time.Clock;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.coworkhub.negocio.excepcion.SolicitudInvalidaException;
import com.coworkhub.negocio.model.Member;
import com.coworkhub.persistence.repository.BookingRepository;

@Service
public class ServicioConsumoImpl implements ServicioConsumo {

    private final ServicioMiembros servicioMiembros;
    private final BookingRepository reservas;
    private final Clock reloj;

    public ServicioConsumoImpl(ServicioMiembros servicioMiembros, BookingRepository reservas, Clock reloj) {
        this.servicioMiembros = servicioMiembros;
        this.reservas = reservas;
        this.reloj = reloj;
    }

    @Override
    @Transactional(readOnly = true)
    public ConsumoMensual consultar(Long miembroId, String mes) {
        Member miembro = servicioMiembros.buscar(miembroId);
        YearMonth periodo = interpretarMes(mes);
        long usados = minutosConsumidos(miembroId, periodo);
        long incluidos = miembro.getPlan().getIncludedHoursPerMonth() * 60L;
        return new ConsumoMensual(miembroId, periodo, incluidos, usados);
    }

    @Override
    @Transactional(readOnly = true)
    public long minutosConsumidos(Long miembroId, YearMonth mes) {
        return reservas.sumConsumedMinutes(miembroId, mes.atDay(1).atStartOfDay(),
                mes.plusMonths(1).atDay(1).atStartOfDay());
    }

    private YearMonth interpretarMes(String mes) {
        if (mes == null || mes.isBlank()) {
            return YearMonth.now(reloj);
        }
        try {
            return YearMonth.parse(mes.trim());
        } catch (DateTimeParseException e) {
            throw new SolicitudInvalidaException("El mes debe tener el formato yyyy-MM, por ejemplo 2026-11");
        }
    }
}
