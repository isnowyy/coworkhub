package com.coworkhub.negocio.service;

import java.time.YearMonth;

/** Consumo de un miembro en un mes (RF-10), en minutos. */
public record ConsumoMensual(Long miembroId, YearMonth mes, long minutosIncluidos, long minutosUsados) {

    public long minutosRestantes() {
        return Math.max(0, minutosIncluidos - minutosUsados);
    }

    public long minutosExcedentes() {
        return Math.max(0, minutosUsados - minutosIncluidos);
    }
}
