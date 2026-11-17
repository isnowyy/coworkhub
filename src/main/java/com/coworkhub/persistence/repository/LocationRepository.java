package com.coworkhub.persistence.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.coworkhub.negocio.model.Location;

public interface LocationRepository extends JpaRepository<Location, Long> {

    List<Location> findAllByOrderByNameAsc();
}
