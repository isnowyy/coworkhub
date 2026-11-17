package com.coworkhub.negocio.service;

import java.math.RoundingMode;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.coworkhub.negocio.excepcion.RecursoNoEncontradoException;
import com.coworkhub.negocio.model.AdditionalService;
import com.coworkhub.persistence.repository.AdditionalServiceRepository;
import com.coworkhub.persistence.repository.BookingRepository;
import com.coworkhub.presentacion.dto.SolicitudServicioAdicional;

@Service
public class ServicioServiciosAdicionalesImpl implements ServicioServiciosAdicionales {

    private final AdditionalServiceRepository servicios;
    private final BookingRepository reservas;

    public ServicioServiciosAdicionalesImpl(AdditionalServiceRepository servicios, BookingRepository reservas) {
        this.servicios = servicios;
        this.reservas = reservas;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdditionalService> listar() {
        return servicios.findAllByOrderByNameAsc();
    }

    @Override
    @Transactional(readOnly = true)
    public AdditionalService buscar(Long id) {
        return servicios.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Servicio adicional", id));
    }

    @Override
    @Transactional
    public AdditionalService crear(SolicitudServicioAdicional s) {
        String nombre = Validaciones.limpiar(s.nombre());
        Validaciones.exigirUnico(servicios.existsByNameIgnoreCase(nombre),
                "Ya existe un servicio adicional llamado '" + nombre + "'");
        return servicios.save(new AdditionalService(nombre, s.precioUnitario().setScale(2, RoundingMode.HALF_UP)));
    }

    /** Cambiar el precio no altera las reservas existentes: cada detalle guardó su propio precio (RN-07). */
    @Override
    @Transactional
    public AdditionalService actualizar(Long id, SolicitudServicioAdicional s) {
        AdditionalService servicio = buscar(id);
        String nombre = Validaciones.limpiar(s.nombre());
        Validaciones.exigirUnico(servicios.existsByNameIgnoreCaseAndIdNot(nombre, id),
                "Ya existe un servicio adicional llamado '" + nombre + "'");
        servicio.setName(nombre);
        servicio.setUnitPrice(s.precioUnitario().setScale(2, RoundingMode.HALF_UP));
        return servicio;
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        AdditionalService servicio = buscar(id);
        Validaciones.exigirSinUso(reservas.existsByItems_Service_Id(id),
                "El servicio '" + servicio.getName() + "' se usó en reservas y no se puede eliminar");
        servicios.delete(servicio);
    }
}
