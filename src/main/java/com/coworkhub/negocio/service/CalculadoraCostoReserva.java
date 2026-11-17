package com.coworkhub.negocio.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import org.springframework.stereotype.Component;

import com.coworkhub.negocio.model.BookingItem;
import com.coworkhub.negocio.model.MembershipPlan;
import com.coworkhub.negocio.model.Room;

/**
 * Todas las fórmulas de dinero (RN-07 y RN-08) en un solo bean. No accede a la base de datos:
 * recibe los datos y devuelve resultados, por eso es fácil de leer y de probar.
 * El dinero se calcula con BigDecimal (nunca double) y se redondea a 2 decimales al final.
 */
@Component
public class CalculadoraCostoReserva {

    private static final BigDecimal CIEN = BigDecimal.valueOf(100);
    private static final BigDecimal SESENTA = BigDecimal.valueOf(60);
    private static final BigDecimal CINCUENTA_POR_CIENTO = new BigDecimal("0.50");

    /** Resultado del costo de la sala: cuántos minutos del plan consume y cuánto se cobra. */
    public record CostoSala(int minutosConsumidos, BigDecimal costo) {
    }

    /**
     * RN-07: los minutos que todavía caben en las horas incluidas del plan no se cobran; los
     * excedentes valen tarifa × horas × (1 − descuento).
     *
     * @param minutosYaUsados minutos del plan que el miembro ya consumió en el mes de la reserva
     */
    public CostoSala costoSala(Room sala, MembershipPlan plan, long minutosReserva, long minutosYaUsados) {
        long incluidos = plan.getIncludedHoursPerMonth() * 60L;
        long disponibles = Math.max(0, incluidos - minutosYaUsados);
        long cubiertos = Math.min(minutosReserva, disponibles);
        long excedentes = minutosReserva - cubiertos;

        BigDecimal factorDescuento = CIEN.subtract(BigDecimal.valueOf(plan.getOverageDiscount()))
                .divide(CIEN, 4, RoundingMode.HALF_UP);
        BigDecimal costo = sala.getHourlyRate()
                .multiply(BigDecimal.valueOf(excedentes))
                .divide(SESENTA, 10, RoundingMode.HALF_UP)
                .multiply(factorDescuento)
                .setScale(2, RoundingMode.HALF_UP);
        return new CostoSala((int) minutosReserva, costo);
    }

    /** RN-07: suma cantidad × precio aplicado (el precio guardado al agregar el servicio). */
    public BigDecimal costoServicios(List<BookingItem> items) {
        return items.stream()
                .map(BookingItem::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }

    /** RN-08: cancelar entre 24 h y 2 h antes cuesta el 50 % del costo de la sala. */
    public BigDecimal cargoCancelacionTardia(BigDecimal costoSala) {
        return costoSala.multiply(CINCUENTA_POR_CIENTO).setScale(2, RoundingMode.HALF_UP);
    }
}
