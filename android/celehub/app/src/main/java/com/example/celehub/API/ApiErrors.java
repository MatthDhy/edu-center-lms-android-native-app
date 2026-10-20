package com.example.celehub.API;

import com.google.gson.Gson;

import retrofit2.Response;

import com.example.celehub.Model.ApiResponse;

/** Lấy câu thông báo lỗi tiếng Việt mà backend gửi kèm khi trả về lỗi (401, 400, 403...). */
public final class ApiErrors {

    public static final String NETWORK_ERROR =
            "Không thể kết nối máy chủ. Hãy kiểm tra mạng rồi thử lại.";

    private ApiErrors() { }

    public static String messageOf(Response<?> response, String fallback) {
        try {
            if (response.errorBody() != null) {
                String body = response.errorBody().string();
                ApiResponse<?> parsed = new Gson().fromJson(body, ApiResponse.class);
                if (parsed != null && parsed.getMessage() != null && !parsed.getMessage().isEmpty()) {
                    return parsed.getMessage();
                }
            }
        } catch (Exception ignored) {
            // không đọc được thì dùng câu mặc định bên dưới
        }
        return fallback;
    }
}