package com.coworkhub.negocio.service;

import java.util.List;
import java.util.Locale;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.coworkhub.negocio.excepcion.RecursoNoEncontradoException;
import com.coworkhub.negocio.model.Member;
import com.coworkhub.negocio.model.MemberStatus;
import com.coworkhub.negocio.model.MembershipPlan;
import com.coworkhub.negocio.model.Role;
import com.coworkhub.negocio.model.UserAccount;
import com.coworkhub.persistence.repository.BookingRepository;
import com.coworkhub.persistence.repository.MemberRepository;
import com.coworkhub.persistence.repository.UserAccountRepository;
import com.coworkhub.presentacion.dto.SolicitudActualizarMiembro;
import com.coworkhub.presentacion.dto.SolicitudMiembro;

@Service
public class ServicioMiembrosImpl implements ServicioMiembros {

    private final MemberRepository miembros;
    private final UserAccountRepository cuentas;
    private final BookingRepository reservas;
    private final ServicioPlanes servicioPlanes;
    private final PasswordEncoder codificador;

    public ServicioMiembrosImpl(MemberRepository miembros, UserAccountRepository cuentas, BookingRepository reservas,
                                ServicioPlanes servicioPlanes, PasswordEncoder codificador) {
        this.miembros = miembros;
        this.cuentas = cuentas;
        this.reservas = reservas;
        this.servicioPlanes = servicioPlanes;
        this.codificador = codificador;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Member> listar(String nombre, MemberStatus estado) {
        String filtro = (nombre == null || nombre.isBlank()) ? null : nombre.trim();
        return miembros.search(filtro, estado);
    }

    @Override
    @Transactional(readOnly = true)
    public Member buscar(Long id) {
        return miembros.findWithDetailsById(id).orElseThrow(() -> new RecursoNoEncontradoException("Miembro", id));
    }

    @Override
    @Transactional
    public Member crear(SolicitudMiembro s) {
        String documento = s.documento().trim();
        String email = normalizarEmail(s.email());
        String usuario = s.usuario().trim();
        Validaciones.exigirUnico(miembros.existsByDocumentNumber(documento),
                "Ya existe un miembro con el documento " + documento);
        Validaciones.exigirUnico(miembros.existsByEmailIgnoreCase(email), "Ya existe un miembro con el email " + email);
        Validaciones.exigirUnico(cuentas.existsByUsernameIgnoreCase(usuario), "El usuario '" + usuario + "' ya existe");
        MembershipPlan plan = servicioPlanes.buscar(s.planId());

        Member miembro = new Member(documento, s.nombreCompleto().trim(), email, plan);
        miembro.setUserAccount(new UserAccount(usuario, codificador.encode(s.contrasena()), Role.MEMBER));
        return miembros.save(miembro); // cascade PERSIST guarda también la cuenta
    }

    @Override
    @Transactional
    public Member actualizar(Long id, SolicitudActualizarMiembro s) {
        Member miembro = buscar(id);
        String email = normalizarEmail(s.email());
        Validaciones.exigirUnico(miembros.existsByEmailIgnoreCaseAndIdNot(email, id),
                "Ya existe un miembro con el email " + email);
        miembro.setFullName(s.nombreCompleto().trim());
        miembro.setEmail(email);
        miembro.setPlan(servicioPlanes.buscar(s.planId()));
        return miembro;
    }

    @Override
    @Transactional
    public Member suspender(Long id) {
        Member miembro = buscar(id);
        miembro.setStatus(MemberStatus.SUSPENDED);
        return miembro;
    }

    @Override
    @Transactional
    public Member activar(Long id) {
        Member miembro = buscar(id);
        miembro.setStatus(MemberStatus.ACTIVE);
        return miembro;
    }

    /** RN-12: un miembro con reservas no se elimina. Si no tiene, se borra junto con su cuenta. */
    @Override
    @Transactional
    public void eliminar(Long id) {
        Member miembro = buscar(id);
        Validaciones.exigirSinUso(reservas.existsByMemberId(id),
                "El miembro '" + miembro.getFullName() + "' tiene reservas y no se puede eliminar; puedes suspenderlo");
        UserAccount cuenta = miembro.getUserAccount();
        miembros.delete(miembro);
        if (cuenta != null) {
            miembros.flush(); // primero se borra el miembro (tiene la FK) y después la cuenta
            cuentas.delete(cuenta);
        }
    }

    /** RN-11: el email se guarda en minúsculas para que la unicidad no distinga mayúsculas. */
    private static String normalizarEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
