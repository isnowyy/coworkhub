package com.coworkhub.presentacion.dto;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

import com.coworkhub.negocio.model.Room;
import com.coworkhub.negocio.model.RoomType;

public record RespuestaSala(Long id, String nombre, RoomType tipo, int capacidad, BigDecimal tarifaHora,
                            boolean activa, Long sedeId, String sedeNombre,
                            List<RespuestaEquipamiento> equipamiento) {

    /** Requiere que la sede y el equipamiento estén cargados (el servicio usa @EntityGraph). */
    public static RespuestaSala desde(Room sala) {
        List<RespuestaEquipamiento> equipos = sala.getEquipment().stream()
                .sorted(Comparator.comparing(e -> e.getName()))
                .map(RespuestaEquipamiento::desde)
                .toList();
        return new RespuestaSala(sala.getId(), sala.getName(), sala.getType(), sala.getCapacity(),
                sala.getHourlyRate(), sala.isActive(), sala.getLocation().getId(),
                sala.getLocation().getName(), equipos);
    }
}
