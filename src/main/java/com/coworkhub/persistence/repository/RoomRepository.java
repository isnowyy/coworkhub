package com.coworkhub.persistence.repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.coworkhub.negocio.model.BookingStatus;
import com.coworkhub.negocio.model.Room;

import jakarta.persistence.LockModeType;

public interface RoomRepository extends JpaRepository<Room, Long> {

    // Listados con su sede y equipamiento en la misma consulta (sin N+1).
    @EntityGraph(attributePaths = {"location", "equipment"})
    List<Room> findAllByOrderByNameAsc();

    @EntityGraph(attributePaths = {"location", "equipment"})
    List<Room> findByLocationIdOrderByNameAsc(Long locationId);

    @EntityGraph(attributePaths = {"location", "equipment"})
    Optional<Room> findWithDetailsById(Long id);

    boolean existsByLocationId(Long locationId);

    boolean existsByLocationIdAndNameIgnoreCase(Long locationId, String name);

    boolean existsByLocationIdAndNameIgnoreCaseAndIdNot(Long locationId, String name, Long id);

    boolean existsByEquipment_Id(Long equipmentId);

    /**
     * Bloquea la fila de la sala (SELECT ... FOR UPDATE) hasta que termine la transacción:
     * dos solicitudes simultáneas para la misma sala se procesan una después de la otra (RN-01).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Room r join fetch r.location where r.id = :id")
    Optional<Room> findForBooking(@Param("id") Long id);

    /**
     * Salas activas de una sede, con capacidad suficiente y sin reservas activas que se crucen
     * con el rango pedido (RF-03). El filtro de equipamiento se aplica después, en el servicio.
     */
    @EntityGraph(attributePaths = {"location", "equipment"})
    @Query("""
           select r from Room r
           where r.location.id = :locationId
             and r.active = true
             and r.capacity >= :minCapacity
             and not exists (
                   select b.id from Booking b
                   where b.room = r
                     and b.status in :statuses
                     and b.startTime < :end
                     and b.endTime > :start)
           order by r.name
           """)
    List<Room> findAvailable(@Param("locationId") Long locationId,
                             @Param("minCapacity") int minCapacity,
                             @Param("start") LocalDateTime start,
                             @Param("end") LocalDateTime end,
                             @Param("statuses") Collection<BookingStatus> statuses);
}
