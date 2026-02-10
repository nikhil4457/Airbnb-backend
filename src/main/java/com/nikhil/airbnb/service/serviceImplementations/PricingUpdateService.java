package com.nikhil.airbnb.service.serviceImplementations;

import com.nikhil.airbnb.entity.Hotel;
import com.nikhil.airbnb.entity.HotelMinPrice;
import com.nikhil.airbnb.entity.Inventory;
import com.nikhil.airbnb.entity.Room;
import com.nikhil.airbnb.repository.HotelMinPriceRepository;
import com.nikhil.airbnb.repository.HotelRepository;
import com.nikhil.airbnb.repository.InventoryRepository;
import com.nikhil.airbnb.strategy.PricingService;
import jakarta.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class PricingUpdateService {
    // =====================================================================================================================
    // Scheduler to periodically ( every hour ) update pricing based on various strategies
    HotelMinPriceRepository hotelMinPriceRepository;
    InventoryRepository inventoryRepository;
    HotelRepository hotelRepository;
    PricingService pricingService;
    // =====================================================================================================================

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void runOnceAtStartup() {
        updatePrice();
    }
    //-x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x-
    @Scheduled(cron = "0 */30 * * * *")
    public void updatePrice(){
        log.info("Updating inventory prices ... ");
        int page = 0;
        int batchSize = 500;
        while(true){
            Page<Hotel> hotelPage = hotelRepository.findAll(PageRequest.of(page, batchSize));
            if(hotelPage.isEmpty()){
                break;
            }
            hotelPage.getContent().forEach(this::updateHotelPrice);
            page++;
        }
    }
    //-x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x-
    @Transactional
    public void updateHotelPrice(Hotel hotel){
        log.info("Updating price for hotel: {}", hotel.getId());
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusYears(1);
        List<Inventory> inventoryList = inventoryRepository.findByHotelAndDateBetween(hotel, startDate, endDate);
        updateInventoryPrice(inventoryList);
        updateHotelMinPrice(hotel, inventoryList);

    }
    //-x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x-
    @Transactional
    public void updateRoomPrice(Room room){
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusYears(1);
        Hotel hotel = room.getHotel();
        List<Inventory> inventoryList = inventoryRepository.findByHotelAndDateBetween(hotel, startDate, endDate);
        updateInventoryPrice(inventoryList.stream().filter(inventory ->
                inventory.getRoom().getId().equals(room.getId())).toList()); // only updating the affected inventories
        updateHotelMinPrice(room.getHotel(), inventoryList);
    }
    //-x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x-
    private void updateInventoryPrice(List<Inventory> inventoryList){
        inventoryList.forEach(inventory -> {
            BigDecimal price = pricingService.calculateDynamicPricing(inventory);
            inventory.setPrice(price);
        });
        inventoryRepository.saveAll(inventoryList);
    }
    //-x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x--x-x-x-x-x-x-x-x-x-x-x-x-x-
    private void updateHotelMinPrice(Hotel hotel, List<Inventory> nextOneYearInventoryList){
        Map<LocalDate, BigDecimal> dailyMinPrice = new HashMap<>();
        for(Inventory inventory : nextOneYearInventoryList){
            if(inventory.getClosed()){
                continue;
            }
            LocalDate date = inventory.getDate();
            BigDecimal price = inventory.getPrice();
            dailyMinPrice.putIfAbsent(date, price);
            if(price.compareTo(dailyMinPrice.get(date)) < 0){
                dailyMinPrice.put(date, price);
            }
        }
        List<HotelMinPrice> existingPrices = hotelMinPriceRepository
                .findByHotelAndDateIn(hotel, new ArrayList<>(dailyMinPrice.keySet()));
        Map<LocalDate, HotelMinPrice> existingPriceMap = existingPrices.stream()
                .collect(Collectors.toMap(HotelMinPrice::getDate, p -> p));
        List<HotelMinPrice> hotelPrices = new ArrayList<>();
        dailyMinPrice.forEach((date, price) -> {
            HotelMinPrice hotelMinPrice = existingPriceMap.getOrDefault(date,
                    HotelMinPrice.builder()
                            .hotel(hotel)
                            .date(date)
                            .build());
            hotelMinPrice.setMinPrice(price);
            hotelPrices.add(hotelMinPrice);
        });
        hotelMinPriceRepository.saveAll(hotelPrices);
    }

    // =====================================================================================================================
}
