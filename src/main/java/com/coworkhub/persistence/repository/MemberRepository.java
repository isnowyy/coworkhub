package com.coworkhub.persistence.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.coworkhub.negocio.model.Member;
import com.coworkhub.negocio.model.MemberStatus;

public interface MemberRepository extends JpaRepository<Member, Long> {

    /** Lista con filtros opcionales (null = sin filtro), trayendo plan y cuenta en la misma consulta. */
    @EntityGraph(attributePaths = {"plan", "userAccount"})
    @Query("""
           select m from Member m
           where (:name is null or lower(m.fullName) like lower(concat('%', :name, '%')))
             and (:status is null or m.status = :status)
           order by m.fullName
           """)
    List<Member> search(@Param("name") String name, @Param("status") MemberStatus status);

    @EntityGraph(attributePaths = {"plan", "userAccount"})
    Optional<Member> findWithDetailsById(Long id);

    Optional<Member> findByUserAccountUsername(String username);

    boolean existsByDocumentNumber(String documentNumber);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    boolean existsByPlanId(Long planId);
}
