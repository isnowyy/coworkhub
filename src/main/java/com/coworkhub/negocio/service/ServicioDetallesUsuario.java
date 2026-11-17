package com.coworkhub.negocio.service;

import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.coworkhub.config.UsuarioAutenticado;
import com.coworkhub.negocio.model.Member;
import com.coworkhub.negocio.model.UserAccount;
import com.coworkhub.persistence.repository.MemberRepository;
import com.coworkhub.persistence.repository.UserAccountRepository;

/** Carga una cuenta por usuario para Spring Security, con su rol y su miembro (si tiene). */
@Service
public class ServicioDetallesUsuario implements UserDetailsService {

    private final UserAccountRepository cuentas;
    private final MemberRepository miembros;

    public ServicioDetallesUsuario(UserAccountRepository cuentas, MemberRepository miembros) {
        this.cuentas = cuentas;
        this.miembros = miembros;
    }

    @Override
    @Transactional(readOnly = true)
    public UsuarioAutenticado loadUserByUsername(String username) {
        UserAccount cuenta = cuentas.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario o contraseña incorrectos"));
        Long miembroId = miembros.findByUserAccountUsername(username).map(Member::getId).orElse(null);
        return new UsuarioAutenticado(cuenta.getUsername(), cuenta.getPassword(), cuenta.getRole(), miembroId);
    }
}
