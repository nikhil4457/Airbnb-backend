package com.nikhil.airbnb.service.serviceImplementations;

import com.nikhil.airbnb.dto.HotelPriceDto;
import com.nikhil.airbnb.dto.HotelSearchRequest;
import com.nikhil.airbnb.entity.Inventory;
import com.nikhil.airbnb.entity.Room;
import com.nikhil.airbnb.repository.HotelMinPriceRepository;
import com.nikhil.airbnb.repository.InventoryRepository;
import com.nikhil.airbnb.service.serviceInterfaces.InventoryService;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class InventoryServiceImpl implements InventoryService {
    // =====================================================================================================================
    InventoryRepository inventoryRepository;
    HotelMinPriceRepository hotelMinPriceRepository;
    // =====================================================================================================================

    @Override
    @Transactional
    public void initializeRoomsForAYear(Room room) {
        log.info("Initializing inventory for room: {}", room.getId());
        LocalDate today = LocalDate.now();
        LocalDate endDate = today.plusYears(1);
        List<Inventory> inventoriesToBeSaved = new ArrayList<>();
        for(; !today.isAfter(endDate); today = today.plusDays(1)){
            Inventory inventory = Inventory.builder()
                    .hotel(room.getHotel())
                    .room(room)
                    .bookedCount(0)
                    .reservedCount(0)
                    .city(room.getHotel().getCity())
                    .date(today)
                    .price(room.getBasePrice())
                    .surgeFactor(BigDecimal.ONE)
                    .totalCount(room.getTotalCount())
                    .closed(false)
                    .build();
            inventoriesToBeSaved.add(inventory);
        }
        inventoryRepository.saveAll(inventoriesToBeSaved);
    }
    //-x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x-
    @Override
    public void deleteByRoom(Room room) {
        log.info("Deleting inventory for room: {}", room.getId());
        inventoryRepository.deleteByRoom(room);
    }
    //-x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x-
    @Override
    public Page<HotelPriceDto> searchHotels(HotelSearchRequest request) {
        log.info("Searching hotels for city: {}, start date: {}, end date: {}, rooms count: {}, page: {}, size: {}", request.getCity(), request.getStartDate(), request.getEndDate(), request.getRoomsCount(), request.getPage(), request.getSize());
        Pageable pageable = PageRequest.of(request.getPage(), request.getSize());
        return hotelMinPriceRepository.findHotelsWithAvailableInventory(
                request.getCity(),
                request.getStartDate(),
                request.getEndDate(),
                pageable
        );
    }

    // =====================================================================================================================
}
