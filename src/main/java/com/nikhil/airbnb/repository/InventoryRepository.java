package com.nikhil.airbnb.repository;

import com.nikhil.airbnb.entity.Inventory;
import com.nikhil.airbnb.entity.Room;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    @Transactional
    void deleteByDateAfterAndRoom(LocalDate date, Room room);

}