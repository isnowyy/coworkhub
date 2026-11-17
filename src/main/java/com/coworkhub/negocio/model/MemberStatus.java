package com.coworkhub.negocio.model;

/** Estado de un miembro. Uno SUSPENDED sigue existiendo, pero no puede reservar (RN-05). */
public enum MemberStatus {
    ACTIVE,
    SUSPENDED
}
