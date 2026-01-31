package com.nikhil.airbnb.service.serviceImplementations;

import com.nikhil.airbnb.entity.Hotel;
import com.nikhil.airbnb.entity.HotelMinPrice;
import com.nikhil.airbnb.entity.Inventory;
import com.nikhil.airbnb.repository.HotelMinPriceRepository;
import com.nikhil.airbnb.repository.HotelRepository;
import com.nikhil.airbnb.repository.InventoryRepository;
import com.nikhil.airbnb.strategy.PricingService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Transactional
public class PricingUpdateService {
    // =====================================================================================================================
    // Scheduler to periodically ( every hour ) update pricing based on various strategies
    HotelMinPriceRepository hotelMinPriceRepository;
    InventoryRepository inventoryRepository;
    HotelRepository hotelRepository;
    PricingService pricingService;
    // =====================================================================================================================

    @Scheduled(cron = "*/5 * * * * *") // Every 5 minutes for demonstration; change to "0 0 * * * *" for hourly in production)
    public void updatePrice(){
        int page = 0;
        int batchSize = 100;
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
    private void updateHotelPrice(Hotel hotel){
        log.info("Updating price for hotel: {}", hotel.getId());
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusYears(1);
        List<Inventory> inventoryList = inventoryRepository.findByHotelAndDateBetween(hotel, startDate, endDate);
        updateInventoryPrice(inventoryList);
        updateHotelMinPrice(hotel, inventoryList, startDate, endDate);

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
    private void updateHotelMinPrice(Hotel hotel, List<Inventory> inventoryList, LocalDate startDate, LocalDate endDate){

        Map<LocalDate, BigDecimal> dailyMinPrice = new HashMap<>();
        for(Inventory inventory : inventoryList){
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
        List<HotelMinPrice> hotelPrices = new ArrayList<>();
        dailyMinPrice.forEach((date, price) -> {
            HotelMinPrice hotelMinPrice = hotelMinPriceRepository.findByHotelAndDate(hotel, date)
                    .orElseGet(() -> HotelMinPrice.builder()
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
