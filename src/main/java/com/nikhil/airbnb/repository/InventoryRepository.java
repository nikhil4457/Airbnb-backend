package com.nikhil.airbnb.repository;

import com.nikhil.airbnb.entity.Hotel;
import com.nikhil.airbnb.entity.Inventory;
import com.nikhil.airbnb.entity.Room;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    void deleteByRoom(Room room);

    // ==================== BOOKING QUERIES (EXCLUDING CHECKOUT DATE) ====================

    @Query("""
            SELECT i
            FROM Inventory i
            WHERE i.room.id = :roomId
                AND i.date >= :checkInDate
                AND i.date < :checkOutDate
                AND (i.totalCount - i.bookedCount - i.reservedCount) >= :roomsCount
                AND i.closed = false
            """)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<Inventory> findAndLockAvailableInventory(
            @Param("roomId") Long roomId,
            @Param("checkInDate") LocalDate checkInDate,
            @Param("checkOutDate") LocalDate checkOutDate,
            @Param("roomsCount") Integer roomsCount
    );

    @Query("""
                SELECT i
                FROM Inventory i
                WHERE i.room.id = :roomId
                  AND i.date >= :checkInDate
                  AND i.date < :checkOutDate
                  AND i.reservedCount >= :numberOfRooms
                  AND i.closed = false
            """)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<Inventory> findAndLockReservedInventory(
            @Param("roomId") Long roomId,
            @Param("checkInDate") LocalDate checkInDate,
            @Param("checkOutDate") LocalDate checkOutDate,
            @Param("numberOfRooms") int numberOfRooms
    );

    @Query("""
                SELECT i
                FROM Inventory i
                WHERE i.room.id = :roomId
                  AND i.date >= :checkInDate
                  AND i.date < :checkOutDate
                  AND i.bookedCount >= :numberOfRooms
                  AND i.closed = false
            """)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<Inventory> findAndLockBookedInventory(
            @Param("roomId") Long roomId,
            @Param("checkInDate") LocalDate checkInDate,
            @Param("checkOutDate") LocalDate checkOutDate,
            @Param("numberOfRooms") int numberOfRooms
    );

    @Modifying
    @Query("""
                UPDATE Inventory i
                SET i.reservedCount = i.reservedCount + :numberOfRooms
                WHERE i.room.id = :roomId
                  AND i.date >= :checkInDate
                  AND i.date < :checkOutDate
                  AND (i.totalCount - i.bookedCount - i.reservedCount) >= :numberOfRooms
                  AND i.closed = false
            """)
    int initBooking(
            @Param("roomId") Long roomId,
            @Param("checkInDate") LocalDate checkInDate,
            @Param("checkOutDate") LocalDate checkOutDate,
            @Param("numberOfRooms") int numberOfRooms
    );

    @Modifying
    @Query("""
        UPDATE Inventory i
        SET i.reservedCount = i.reservedCount - :numberOfRooms,
            i.bookedCount = i.bookedCount + :numberOfRooms
        WHERE i.room.id = :roomId
          AND i.date >= :checkInDate
          AND i.date < :checkOutDate
          AND i.reservedCount >= :numberOfRooms
          AND i.closed = false
    """)
    int confirmBooking(
            @Param("roomId") Long roomId,
            @Param("checkInDate") LocalDate checkInDate,
            @Param("checkOutDate") LocalDate checkOutDate,
            @Param("numberOfRooms") int numberOfRooms
    );

    @Modifying
    @Query("""
                UPDATE Inventory i
                SET i.bookedCount = i.bookedCount - :numberOfRooms
                WHERE i.room.id = :roomId
                  AND i.date >= :checkInDate
                  AND i.date < :checkOutDate
                  AND i.bookedCount >= :numberOfRooms
            """)
    int cancelBooking(
            @Param("roomId") Long roomId,
            @Param("checkInDate") LocalDate checkInDate,
            @Param("checkOutDate") LocalDate checkOutDate,
            @Param("numberOfRooms") int numberOfRooms
    );

    @Modifying
    @Query("""
        UPDATE Inventory i
        SET i.reservedCount = i.reservedCount - :numberOfRooms
        WHERE i.room.id = :roomId
          AND i.date >= :checkInDate
          AND i.date < :checkOutDate
          AND i.reservedCount >= :numberOfRooms
    """)
    int cancelReservation(
            @Param("roomId") Long roomId,
            @Param("checkInDate") LocalDate checkInDate,
            @Param("checkOutDate") LocalDate checkOutDate,
            @Param("numberOfRooms") Integer numberOfRooms
    );

    // ==================== INVENTORY MANAGEMENT (INCLUDING BOTH DATES) ====================

    List<Inventory> findByHotelAndDateBetween(Hotel hotel, LocalDate startDate, LocalDate endDate);

    @Query("""
                SELECT i.id
                FROM Inventory i
                WHERE i.room.id = :roomId
                  AND i.date >= :startDate
                  AND i.date <= :endDate
            """)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<Long> lockInventoryBeforeUpdate(
            @Param("roomId") Long roomId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    @Modifying
    @Query("""
                UPDATE Inventory i
                SET i.surgeFactor = CASE
                                        WHEN :updateSurgeFactor = true THEN :newSurgeFactor
                                        ELSE i.surgeFactor
                                    END,
               i.closed = CASE
                              WHEN :updateClosedStatus = true THEN :newClosedStatus
                              ELSE i.closed
                           END
                WHERE i.room.id = :roomId
                  AND i.date >= :startDate
                  AND i.date <= :endDate
            """)
    void updateInventory(
            @Param("roomId") Long roomId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("newClosedStatus") Boolean newClosedStatus,
            @Param("newSurgeFactor") BigDecimal newSurgeFactor,
            @Param("updateClosedStatus") boolean updateClosedStatus,
            @Param("updateSurgeFactor") boolean updateSurgeFactor
            );

    List<Inventory> findByRoomOrderByDate(Room room);
}