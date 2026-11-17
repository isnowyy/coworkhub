package com.coworkhub.negocio.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.coworkhub.negocio.excepcion.RecursoNoEncontradoException;
import com.coworkhub.negocio.model.Equipment;
import com.coworkhub.persistence.repository.EquipmentRepository;
import com.coworkhub.persistence.repository.RoomRepository;
import com.coworkhub.presentacion.dto.SolicitudEquipamiento;

@Service
public class ServicioEquipamientosImpl implements ServicioEquipamientos {

    private final EquipmentRepository equipamientos;
    private final RoomRepository salas;

    public ServicioEquipamientosImpl(EquipmentRepository equipamientos, RoomRepository salas) {
        this.equipamientos = equipamientos;
        this.salas = salas;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Equipment> listar() {
        return equipamientos.findAllByOrderByNameAsc();
    }

    @Override
    @Transactional(readOnly = true)
    public Equipment buscar(Long id) {
        return equipamientos.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Equipamiento", id));
    }

    @Override
    @Transactional
    public Equipment crear(SolicitudEquipamiento s) {
        String nombre = Validaciones.limpiar(s.nombre());
        Validaciones.exigirUnico(equipamientos.existsByNameIgnoreCase(nombre),
                "Ya existe un equipamiento llamado '" + nombre + "'");
        return equipamientos.save(new Equipment(nombre));
    }

    @Override
    @Transactional
    public Equipment actualizar(Long id, SolicitudEquipamiento s) {
        Equipment equipo = buscar(id);
        String nombre = Validaciones.limpiar(s.nombre());
        Validaciones.exigirUnico(equipamientos.existsByNameIgnoreCaseAndIdNot(nombre, id),
                "Ya existe un equipamiento llamado '" + nombre + "'");
        equipo.setName(nombre);
        return equipo;
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        Equipment equipo = buscar(id);
        Validaciones.exigirSinUso(salas.existsByEquipment_Id(id),
                "El equipamiento '" + equipo.getName() + "' está asignado a una sala y no se puede eliminar");
        equipamientos.delete(equipo);
    }
}
