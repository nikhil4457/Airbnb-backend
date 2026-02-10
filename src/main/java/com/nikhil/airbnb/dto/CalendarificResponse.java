package com.nikhil.airbnb.dto;


import lombok.Data;

import java.util.List;

@Data
public class CalendarificResponse {
    private InnerResponse response;

    @Data
    public static class InnerResponse {
        private List<Object> holidays;
    }
}