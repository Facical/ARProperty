package com.arproperty.dto;

/** 공통 API 응답 래퍼 (status, data, meta, error) */

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private String status;
    private T data;
    private Object meta;
    private ErrorBody error;

    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder().status("ok").data(data).build();
    }

    public static <T> ApiResponse<T> success(T data, Object meta) {
        return ApiResponse.<T>builder().status("ok").data(data).meta(meta).build();
    }

    public static <T> ApiResponse<T> error(String code, String message) {
        return ApiResponse.<T>builder()
                .status("error")
                .error(new ErrorBody(code, message))
                .build();
    }

    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ErrorBody {
        private String code;
        private String message;
    }
}
