package com.coworkhub.negocio.service;

import java.math.RoundingMode;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.coworkhub.negocio.excepcion.RecursoNoEncontradoException;
import com.coworkhub.negocio.model.Equipment;
import com.coworkhub.negocio.model.Location;
import com.coworkhub.negocio.model.Room;
import com.coworkhub.persistence.repository.BookingRepository;
import com.coworkhub.persistence.repository.EquipmentRepository;
import com.coworkhub.persistence.repository.RoomRepository;
import com.coworkhub.presentacion.dto.SolicitudSala;

@Service
public class ServicioSalasImpl implements ServicioSalas {

    private final RoomRepository salas;
    private final EquipmentRepository equipamientos;
    private final BookingRepository reservas;
    private final ServicioSedes servicioSedes;

    public ServicioSalasImpl(RoomRepository salas, EquipmentRepository equipamientos, BookingRepository reservas,
                             ServicioSedes servicioSedes) {
        this.salas = salas;
        this.equipamientos = equipamientos;
        this.reservas = reservas;
        this.servicioSedes = servicioSedes;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Room> listar(Long sedeId) {
        if (sedeId == null) {
            return salas.findAllByOrderByNameAsc();
        }
        servicioSedes.buscar(sedeId); // 404 si la sede no existe
        return salas.findByLocationIdOrderByNameAsc(sedeId);
    }

    @Override
    @Transactional(readOnly = true)
    public Room buscar(Long id) {
        return salas.findWithDetailsById(id).orElseThrow(() -> new RecursoNoEncontradoException("Sala", id));
    }

    @Override
    @Transactional
    public Room crear(SolicitudSala s) {
        Location sede = servicioSedes.buscar(s.sedeId());
        String nombre = Validaciones.limpiar(s.nombre());
        Validaciones.exigirUnico(salas.existsByLocationIdAndNameIgnoreCase(sede.getId(), nombre),
                "La sede '" + sede.getName() + "' ya tiene una sala llamada '" + nombre + "'");
        Room sala = new Room(nombre, s.tipo(), s.capacidad(), s.tarifaHora().setScale(2, RoundingMode.HALF_UP),
                s.activa() == null || s.activa(), sede);
        sala.replaceEquipment(buscarEquipamientos(s.equipamientoIds()));
        return salas.save(sala);
    }

    @Override
    @Transactional
    public Room actualizar(Long id, SolicitudSala s) {
        Room sala = buscar(id);
        Location sede = servicioSedes.buscar(s.sedeId());
        String nombre = Validaciones.limpiar(s.nombre());
        Validaciones.exigirUnico(salas.existsByLocationIdAndNameIgnoreCaseAndIdNot(sede.getId(), nombre, id),
                "La sede '" + sede.getName() + "' ya tiene una sala llamada '" + nombre + "'");
        sala.setName(nombre);
        sala.setType(s.tipo());
        sala.setCapacity(s.capacidad());
        sala.setHourlyRate(s.tarifaHora().setScale(2, RoundingMode.HALF_UP));
        if (s.activa() != null) {
            sala.setActive(s.activa());
        }
        sala.setLocation(sede);
        sala.replaceEquipment(buscarEquipamientos(s.equipamientoIds()));
        return sala;
    }

    /** RN-12: una sala con reservas (aunque sean pasadas) no se elimina; se puede desactivar. */
    @Override
    @Transactional
    public void eliminar(Long id) {
        Room sala = buscar(id);
        Validaciones.exigirSinUso(reservas.existsByRoomId(id),
                "La sala '" + sala.getName() + "' tiene reservas y no se puede eliminar; desactívala con activa=false");
        salas.delete(sala);
    }

    private Set<Equipment> buscarEquipamientos(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Set.of();
        }
        Set<Long> pedidos = new HashSet<>(ids);
        List<Equipment> encontrados = equipamientos.findAllById(pedidos);
        if (encontrados.size() != pedidos.size()) {
            Set<Long> encontradosIds = new HashSet<>();
            encontrados.forEach(e -> encontradosIds.add(e.getId()));
            Long faltante = pedidos.stream().filter(idPedido -> !encontradosIds.contains(idPedido)).findFirst().orElseThrow();
            throw new RecursoNoEncontradoException("Equipamiento", faltante);
        }
        return new HashSet<>(encontrados);
    }
}
