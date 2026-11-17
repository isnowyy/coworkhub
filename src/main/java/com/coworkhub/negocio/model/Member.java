package com.coworkhub.negocio.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

/**
 * Miembro: cliente del coworking con un plan. Documento y email únicos (RN-11); el email se
 * guarda en minúsculas para que la unicidad no distinga mayúsculas.
 * <ul>
 * <li>Member → MembershipPlan: Member es el dueño (FK plan_id). LAZY, sin cascade.</li>
 * <li>Member → UserAccount: 1↔1 opcional (0..1 de cada lado). Member es el dueño (FK
 * user_account_id, única). cascade PERSIST: miembro y cuenta se crean juntos en
 * POST /api/miembros. Sin REMOVE: la cuenta se borra explícitamente en el servicio.</li>
 * </ul>
 */
@Entity
@Table(name = "members")
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "document_number", nullable = false, unique = true, length = 20)
    private String documentNumber;

    @Column(name = "full_name", nullable = false, length = 120)
    private String fullName;

    @Column(nullable = false, unique = true, length = 120)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MemberStatus status = MemberStatus.ACTIVE;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_id", nullable = false)
    private MembershipPlan plan;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    @JoinColumn(name = "user_account_id", unique = true)
    private UserAccount userAccount;

    protected Member() {
        // requerido por JPA
    }

    public Member(String documentNumber, String fullName, String email, MembershipPlan plan) {
        this.documentNumber = documentNumber;
        this.fullName = fullName;
        this.email = email;
        this.plan = plan;
    }

    public Long getId() { return id; }

    public String getDocumentNumber() { return documentNumber; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public MemberStatus getStatus() { return status; }
    public void setStatus(MemberStatus status) { this.status = status; }

    public MembershipPlan getPlan() { return plan; }
    public void setPlan(MembershipPlan plan) { this.plan = plan; }

    public UserAccount getUserAccount() { return userAccount; }
    public void setUserAccount(UserAccount userAccount) { this.userAccount = userAccount; }
}
