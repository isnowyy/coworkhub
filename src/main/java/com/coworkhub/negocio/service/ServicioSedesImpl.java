package com.coworkhub.negocio.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.coworkhub.negocio.excepcion.RecursoNoEncontradoException;
import com.coworkhub.negocio.excepcion.SolicitudInvalidaException;
import com.coworkhub.negocio.model.Location;
import com.coworkhub.persistence.repository.LocationRepository;
import com.coworkhub.persistence.repository.RoomRepository;
import com.coworkhub.presentacion.dto.SolicitudSede;

@Service
public class ServicioSedesImpl implements ServicioSedes {

    private final LocationRepository sedes;
    private final RoomRepository salas;

    public ServicioSedesImpl(LocationRepository sedes, RoomRepository salas) {
        this.sedes = sedes;
        this.salas = salas;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Location> listar() {
        return sedes.findAllByOrderByNameAsc();
    }

    @Override
    @Transactional(readOnly = true)
    public Location buscar(Long id) {
        return sedes.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Sede", id));
    }

    @Override
    @Transactional
    public Location crear(SolicitudSede s) {
        validarHorario(s);
        return sedes.save(new Location(Validaciones.limpiar(s.nombre()), Validaciones.limpiar(s.ciudad()),
                Validaciones.limpiar(s.direccion()), s.horaApertura(), s.horaCierre()));
    }

    @Override
    @Transactional
    public Location actualizar(Long id, SolicitudSede s) {
        Location sede = buscar(id);
        validarHorario(s);
        sede.setName(Validaciones.limpiar(s.nombre()));
        sede.setCity(Validaciones.limpiar(s.ciudad()));
        sede.setAddress(Validaciones.limpiar(s.direccion()));
        sede.setOpeningTime(s.horaApertura());
        sede.setClosingTime(s.horaCierre());
        return sede; // la entidad está administrada: el cambio se guarda al confirmar la transacción
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        Location sede = buscar(id);
        Validaciones.exigirSinUso(salas.existsByLocationId(id),
                "La sede '" + sede.getName() + "' tiene salas y no se puede eliminar");
        sedes.delete(sede);
    }

    private void validarHorario(SolicitudSede s) {
        if (!s.horaApertura().isBefore(s.horaCierre())) {
            throw new SolicitudInvalidaException("La hora de apertura debe ser anterior a la de cierre");
        }
    }
}
