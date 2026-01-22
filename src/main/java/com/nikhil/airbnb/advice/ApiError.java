package com.nikhil.airbnb.advice;

import lombok.Builder;
import lombok.Data;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

import java.util.List;

@Data @Builder
public class ApiError {
    private HttpStatus status;
    private String message;
    private List<String> subErrors;
}
