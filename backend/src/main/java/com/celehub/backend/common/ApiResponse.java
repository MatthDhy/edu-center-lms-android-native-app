package com.celehub.backend.common;

/** Khuôn response thống nhất: { status, message, data } */
public record ApiResponse<T>(int status, String message, T data) {
    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>(200, message, data);
    }
    public static <T> ApiResponse<T> success(T data) {
        return success("OK", data);
    }
    public static <T> ApiResponse<T> error(int status, String message) {
        return new ApiResponse<>(status, message, null);
    }
}
