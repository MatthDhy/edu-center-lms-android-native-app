package com.celehub.backend.security;

import java.io.IOException;

import org.springframework.stereotype.Component;

import com.celehub.backend.common.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletResponse;

/** Filter chạy trước Controller nên không dùng được @RestControllerAdvice — tự ghi JSON lỗi. */
@Component
public class JsonErrorWriter {
    private final ObjectMapper mapper;

    public JsonErrorWriter(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public void write(HttpServletResponse res, int status, String message) throws IOException {
        res.setStatus(status);
        res.setContentType("application/json;charset=UTF-8");
        mapper.writeValue(res.getWriter(), ApiResponse.error(status, message));
    }
}
