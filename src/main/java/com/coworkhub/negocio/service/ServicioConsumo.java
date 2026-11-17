package com.coworkhub.negocio.service;

import java.time.YearMonth;

/** Consumo mensual de horas de un miembro (RF-10). */
public interface ServicioConsumo {

    /** @param mes en formato yyyy-MM; si es null o vacío, el mes actual. */
    ConsumoMensual consultar(Long miembroId, String mes);

    /** Minutos del plan ya consumidos por el miembro en un mes (también lo usa el cálculo de costos, RN-07). */
    long minutosConsumidos(Long miembroId, YearMonth mes);
}
