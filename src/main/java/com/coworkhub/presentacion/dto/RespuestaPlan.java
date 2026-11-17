package com.coworkhub.presentacion.dto;

import com.coworkhub.negocio.model.MembershipPlan;

public record RespuestaPlan(Long id, String nombre, int horasIncluidasMes, int descuentoExcedente,
                            int maxReservasActivas) {

    public static RespuestaPlan desde(MembershipPlan plan) {
        return new RespuestaPlan(plan.getId(), plan.getName(), plan.getIncludedHoursPerMonth(),
                plan.getOverageDiscount(), plan.getMaxActiveBookings());
    }
}
