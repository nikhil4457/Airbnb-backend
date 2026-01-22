package com.nikhil.airbnb.repository;

import com.nikhil.airbnb.entity.Room;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomRepository extends JpaRepository<Room, Long> {
}