package com.coworkhub.persistence.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.coworkhub.negocio.model.MembershipPlan;

public interface MembershipPlanRepository extends JpaRepository<MembershipPlan, Long> {

    List<MembershipPlan> findAllByOrderByNameAsc();

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
}
