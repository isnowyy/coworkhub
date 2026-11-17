package com.coworkhub.presentacion.dto;

import com.coworkhub.negocio.model.Member;
import com.coworkhub.negocio.model.MemberStatus;

/** Miembro sin datos sensibles: de su cuenta solo se muestra el nombre de usuario. */
public record RespuestaMiembro(Long id, String documento, String nombreCompleto, String email,
                               MemberStatus estado, Long planId, String planNombre, String usuario) {

    /** Requiere el plan y la cuenta cargados (el servicio usa @EntityGraph). */
    public static RespuestaMiembro desde(Member m) {
        return new RespuestaMiembro(m.getId(), m.getDocumentNumber(), m.getFullName(), m.getEmail(),
                m.getStatus(), m.getPlan().getId(), m.getPlan().getName(),
                m.getUserAccount() == null ? null : m.getUserAccount().getUsername());
    }
}
