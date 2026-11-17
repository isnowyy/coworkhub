package com.coworkhub.negocio.service;

import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.coworkhub.config.UsuarioAutenticado;
import com.coworkhub.negocio.excepcion.CredencialesInvalidasException;

@Service
public class ServicioAutenticacionImpl implements ServicioAutenticacion {

    private final ServicioDetallesUsuario detallesUsuario;
    private final PasswordEncoder codificador;

    public ServicioAutenticacionImpl(ServicioDetallesUsuario detallesUsuario, PasswordEncoder codificador) {
        this.detallesUsuario = detallesUsuario;
        this.codificador = codificador;
    }

    /** El mismo mensaje para usuario inexistente y contraseña mala: no revela qué usuarios existen. */
    @Override
    public UsuarioAutenticado autenticar(String usuario, String contrasena) {
        try {
            UsuarioAutenticado encontrado = detallesUsuario.loadUserByUsername(usuario.trim());
            if (!codificador.matches(contrasena, encontrado.getPassword())) {
                throw new CredencialesInvalidasException();
            }
            return encontrado.sinContrasena();
        } catch (UsernameNotFoundException e) {
            throw new CredencialesInvalidasException();
        }
    }
}
