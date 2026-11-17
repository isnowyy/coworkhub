package com.coworkhub.negocio.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.coworkhub.negocio.excepcion.RecursoNoEncontradoException;
import com.coworkhub.negocio.model.MembershipPlan;
import com.coworkhub.persistence.repository.MemberRepository;
import com.coworkhub.persistence.repository.MembershipPlanRepository;
import com.coworkhub.presentacion.dto.SolicitudPlan;

@Service
public class ServicioPlanesImpl implements ServicioPlanes {

    private final MembershipPlanRepository planes;
    private final MemberRepository miembros;

    public ServicioPlanesImpl(MembershipPlanRepository planes, MemberRepository miembros) {
        this.planes = planes;
        this.miembros = miembros;
    }

    @Override
    @Transactional(readOnly = true)
    public List<MembershipPlan> listar() {
        return planes.findAllByOrderByNameAsc();
    }

    @Override
    @Transactional(readOnly = true)
    public MembershipPlan buscar(Long id) {
        return planes.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("Plan", id));
    }

    @Override
    @Transactional
    public MembershipPlan crear(SolicitudPlan s) {
        String nombre = Validaciones.limpiar(s.nombre());
        Validaciones.exigirUnico(planes.existsByNameIgnoreCase(nombre), "Ya existe un plan llamado '" + nombre + "'");
        return planes.save(new MembershipPlan(nombre, s.horasIncluidasMes(), s.descuentoExcedente(),
                s.maxReservasActivas()));
    }

    @Override
    @Transactional
    public MembershipPlan actualizar(Long id, SolicitudPlan s) {
        MembershipPlan plan = buscar(id);
        String nombre = Validaciones.limpiar(s.nombre());
        Validaciones.exigirUnico(planes.existsByNameIgnoreCaseAndIdNot(nombre, id),
                "Ya existe un plan llamado '" + nombre + "'");
        plan.setName(nombre);
        plan.setIncludedHoursPerMonth(s.horasIncluidasMes());
        plan.setOverageDiscount(s.descuentoExcedente());
        plan.setMaxActiveBookings(s.maxReservasActivas());
        return plan;
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        MembershipPlan plan = buscar(id);
        Validaciones.exigirSinUso(miembros.existsByPlanId(id),
                "El plan '" + plan.getName() + "' tiene miembros y no se puede eliminar");
        planes.delete(plan);
    }
}
