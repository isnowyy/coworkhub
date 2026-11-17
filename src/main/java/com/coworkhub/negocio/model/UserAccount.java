package com.coworkhub.negocio.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Cuenta para iniciar sesión (de un miembro o de un empleado). Se llama UserAccount y no
 * User para no confundirse con la clase User de Spring Security.
 * <p>
 * La relación con Member la guarda Member (lado dueño). Aquí no hay lado inverso: un
 * @OneToOne inverso no se puede cargar LAZY en Hibernate y provocaría una consulta extra por
 * cada cuenta; cuando hace falta el miembro de una cuenta se busca con el repositorio.
 */
@Entity
@Table(name = "user_accounts")
public class UserAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    /** Hash BCrypt (60 caracteres). Nunca texto plano. */
    @Column(nullable = false, length = 60)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    protected UserAccount() {
        // requerido por JPA
    }

    public UserAccount(String username, String password, Role role) {
        this.username = username;
        this.password = password;
        this.role = role;
    }

    public Long getId() { return id; }

    public String getUsername() { return username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public Role getRole() { return role; }
}
