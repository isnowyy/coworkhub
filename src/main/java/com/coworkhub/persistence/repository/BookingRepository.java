package com.coworkhub.persistence.repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.coworkhub.negocio.model.Booking;
import com.coworkhub.negocio.model.BookingStatus;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    // ---------------------------------------------------------------- lecturas sin N+1
    // Una sola colección (items) en el grafo: no hay MultipleBagFetchException.

    @EntityGraph(attributePaths = {"room", "room.location", "member", "items", "items.service"})
    Optional<Booking> findWithDetailsById(Long id);

    @EntityGraph(attributePaths = {"room", "room.location", "member", "items", "items.service"})
    List<Booking> findByMemberIdOrderByStartTimeAsc(Long memberId);

    @EntityGraph(attributePaths = {"room", "room.location", "member", "items", "items.service"})
    List<Booking> findByRoomIdAndStartTimeGreaterThanEqualAndStartTimeLessThanOrderByStartTimeAsc(
            Long roomId, LocalDateTime from, LocalDateTime to);

    @EntityGraph(attributePaths = {"room", "room.location", "member", "items", "items.service"})
    List<Booking> findByStatusOrderByStartTimeAsc(BookingStatus status);

    // ---------------------------------------------------------------- reglas de negocio

    /**
     * RN-01: dos rangos se cruzan si uno empieza antes de que el otro termine y termina después
     * de que el otro empieza. Con desigualdades estrictas, los que solo se tocan no se cruzan.
     */
    @Query("""
           select count(b) > 0 from Booking b
           where b.room.id = :roomId
             and b.status in :statuses
             and b.startTime < :end
             and b.endTime > :start
           """)
    boolean existsOverlap(@Param("roomId") Long roomId,
                          @Param("start") LocalDateTime start,
                          @Param("end") LocalDateTime end,
                          @Param("statuses") Collection<BookingStatus> statuses);

    /** RN-06: reservas activas futuras de un miembro. */
    long countByMemberIdAndStatusInAndStartTimeAfter(Long memberId, Collection<BookingStatus> statuses,
                                                     LocalDateTime now);

    /** RN-07 y RF-10: minutos del plan consumidos por un miembro en un rango (un mes). */
    @Query("""
           select coalesce(sum(b.consumedMinutes), 0) from Booking b
           where b.member.id = :memberId
             and b.startTime >= :from
             and b.startTime < :to
           """)
    long sumConsumedMinutes(@Param("memberId") Long memberId,
                            @Param("from") LocalDateTime from,
                            @Param("to") LocalDateTime to);

    // ---------------------------------------------------------------- RN-12 y RN-13

    boolean existsByRoomId(Long roomId);

    boolean existsByMemberId(Long memberId);

    boolean existsByItems_Service_Id(Long serviceId);

    boolean existsByIdAndMemberId(Long id, Long memberId);
}
