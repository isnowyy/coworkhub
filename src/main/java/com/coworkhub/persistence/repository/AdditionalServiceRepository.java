package com.coworkhub.persistence.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.coworkhub.negocio.model.AdditionalService;

public interface AdditionalServiceRepository extends JpaRepository<AdditionalService, Long> {

    List<AdditionalService> findAllByOrderByNameAsc();

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
}
