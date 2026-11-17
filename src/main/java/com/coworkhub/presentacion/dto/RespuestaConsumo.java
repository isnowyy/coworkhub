package com.coworkhub.presentacion.dto;

import java.math.BigDecimal;
import java.math.RoundingMode;

import com.coworkhub.negocio.service.ConsumoMensual;

/** Consumo mensual expresado en horas (con decimales: 90 min = 1.50 h). */
public record RespuestaConsumo(Long miembroId, String mes, BigDecimal horasIncluidas, BigDecimal horasUsadas,
                               BigDecimal horasRestantes, BigDecimal horasExcedentes) {

    public static RespuestaConsumo desde(ConsumoMensual c) {
        return new RespuestaConsumo(c.miembroId(), c.mes().toString(), horas(c.minutosIncluidos()),
                horas(c.minutosUsados()), horas(c.minutosRestantes()), horas(c.minutosExcedentes()));
    }

    private static BigDecimal horas(long minutos) {
        return BigDecimal.valueOf(minutos).divide(BigDecimal.valueOf(60), 2, RoundingMode.HALF_UP);
    }
}
