package com.celehub.backend.common;

import java.util.List;
import java.util.function.Function;

import org.springframework.data.domain.Page;

/** Khuôn phân trang chung: data: { content, page, size, totalElements, totalPages } */
public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {
    public static <E, T> PageResponse<T> from(Page<E> p, Function<E, T> mapper) {
        return new PageResponse<>(p.getContent().stream().map(mapper).toList(),
                p.getNumber(), p.getSize(), p.getTotalElements(), p.getTotalPages());
    }
}
