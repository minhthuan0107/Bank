package com.example.bank.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import org.springframework.http.HttpStatus;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    private int status;
    private String message;
    private T data;

    public static <T> ApiResponse<T> success(int status,String message, T data) {
        return ApiResponse.<T>builder()
                .status(status)
                .message(message)
                .data(data)
                .build();
    }
    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .status(HttpStatus.OK.value())
                .message(null)
                .data(data)
                .build();
    }
    public static <T> ApiResponse<T> ok(int status,String message) {
        return ApiResponse.<T>builder()
                .status(status)
                .message(message)
                .build();
    }
    public static <T> ApiResponse<T> created(int status,String message) {
        return ApiResponse.<T>builder()
                .status(status)
                .message(message)
                .build();
    }
    public static <T> ApiResponse<T> created(int status,String message,T data) {
        return ApiResponse.<T>builder()
                .status(status)
                .message(message)
                .data(data)
                .build();
    }
    public static <T> ApiResponse<T> error(int status, String message) {
        return ApiResponse.<T>builder()
                .status(status)
                .message(message)
                .build();
    }
}