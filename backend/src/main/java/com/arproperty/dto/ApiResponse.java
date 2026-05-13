package com.arproperty.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.Map;

/** 공통 API 응답 래퍼 (status, data, meta) */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(String status, T data, Map<String, Object> meta) {

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>("success", data, null);
    }

    public static <T> ApiResponse<T> success(T data, Map<String, Object> meta) {
        return new ApiResponse<>("success", data, meta);
    }
}
