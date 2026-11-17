package com.coworkhub.negocio.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.coworkhub.negocio.model.AdditionalService;
import com.coworkhub.negocio.model.BookingItem;
import com.coworkhub.negocio.model.Location;
import com.coworkhub.negocio.model.MembershipPlan;
import com.coworkhub.negocio.model.Room;
import com.coworkhub.negocio.model.RoomType;

/** Pruebas unitarias de las fórmulas de dinero (RN-07 y RN-08). No necesitan Spring ni base de datos. */
class CalculadoraCostoReservaTest {

    private final CalculadoraCostoReserva calculadora = new CalculadoraCostoReserva();
    private final Location sede = new Location("Sede", "Bucaramanga", "Calle 1", LocalTime.of(7, 0), LocalTime.of(20, 0));

    private Room sala(String tarifa) {
        return new Room("Sala", RoomType.MEETING_ROOM, 8, new BigDecimal(tarifa), true, sede);
    }

    @Test
    @DisplayName("RN-07: las horas que caben en el plan no se cobran")
    void horasCubiertasPorElPlan() {
        var plan = new MembershipPlan("Profesional", 10, 10, 3);
        var costo = calculadora.costoSala(sala("30000"), plan, 90, 0);
        assertEquals(new BigDecimal("0.00"), costo.costo());
        assertEquals(90, costo.minutosConsumidos());
    }

    @Test
    @DisplayName("RN-07: sin horas incluidas se cobra la tarifa completa")
    void planSinHoras() {
        var plan = new MembershipPlan("Flex", 0, 0, 2);
        assertEquals(new BigDecimal("45000.00"), calculadora.costoSala(sala("30000"), plan, 90, 0).costo());
    }

    @Test
    @DisplayName("RN-07: reserva parcialmente cubierta; el excedente lleva el descuento")
    void excedenteConDescuento() {
        var plan = new MembershipPlan("Básico", 2, 10, 3);
        // ya usó 1 h de 2; reserva 2 h → 1 h cubierta y 1 h excedente × 20.000 × 0,9
        assertEquals(new BigDecimal("18000.00"), calculadora.costoSala(sala("20000"), plan, 120, 60).costo());
    }

    @Test
    @DisplayName("RN-07: plan agotado en el mes; todo es excedente")
    void planAgotado() {
        var plan = new MembershipPlan("Profesional", 10, 10, 3);
        assertEquals(new BigDecimal("27000.00"), calculadora.costoSala(sala("30000"), plan, 60, 700).costo());
    }

    @Test
    @DisplayName("RN-07: los servicios suman cantidad × precio aplicado, aunque el precio cambie después")
    void serviciosConPrecioAplicado() {
        var cafe = new AdditionalService("Café", new BigDecimal("5000"));
        var refrigerio = new AdditionalService("Refrigerio", new BigDecimal("12000"));
        List<BookingItem> items = List.of(new BookingItem(cafe, 3), new BookingItem(refrigerio, 3));
        cafe.setUnitPrice(new BigDecimal("9000")); // cambiar el precio del catálogo no altera los detalles
        assertEquals(new BigDecimal("51000.00"), calculadora.costoServicios(items));
    }

    @Test
    @DisplayName("RN-08: la cancelación tardía cuesta el 50 % de la sala")
    void cargoDeCancelacionTardia() {
        assertEquals(new BigDecimal("4000.00"), calculadora.cargoCancelacionTardia(new BigDecimal("8000.00")));
        assertEquals(new BigDecimal("0.00"), calculadora.cargoCancelacionTardia(new BigDecimal("0.00")));
    }
}
