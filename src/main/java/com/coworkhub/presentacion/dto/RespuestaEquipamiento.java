package com.coworkhub.presentacion.dto;

import com.coworkhub.negocio.model.Equipment;

public record RespuestaEquipamiento(Long id, String nombre) {

    public static RespuestaEquipamiento desde(Equipment equipo) {
        return new RespuestaEquipamiento(equipo.getId(), equipo.getName());
    }
}
