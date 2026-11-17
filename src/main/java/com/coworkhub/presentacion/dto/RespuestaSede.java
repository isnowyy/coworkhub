package com.coworkhub.presentacion.dto;

import java.time.LocalTime;

import com.coworkhub.negocio.model.Location;

public record RespuestaSede(Long id, String nombre, String ciudad, String direccion,
                            LocalTime horaApertura, LocalTime horaCierre) {

    public static RespuestaSede desde(Location sede) {
        return new RespuestaSede(sede.getId(), sede.getName(), sede.getCity(), sede.getAddress(),
                sede.getOpeningTime(), sede.getClosingTime());
    }
}
