package com.nikhil.airbnb.service;

import com.nikhil.airbnb.entity.Room;

public interface InventoryService {

    void initializeRoomsForAYear(Room room);
    void deleteFutureInventories(Room room);
}
