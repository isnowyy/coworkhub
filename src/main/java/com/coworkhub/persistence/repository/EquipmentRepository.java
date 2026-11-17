package com.coworkhub.persistence.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.coworkhub.negocio.model.Equipment;

public interface EquipmentRepository extends JpaRepository<Equipment, Long> {

    List<Equipment> findAllByOrderByNameAsc();

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
}
