package com.avandesk.api.dto;

import org.springframework.http.HttpStatus;
import java.util.List;

/**
 * Envelope padrão de todas as respostas da API (docs/06-documentacao-api-mvp.md).
 */
public record ApiResponse<T>(
    int statusCode,
    String message,
    T data,
    List<String> errors
) {

    public static <T> ApiResponse<T> sucesso(HttpStatus status, String message, T data) {
        return new ApiResponse<>(status.value(), message, data, null);
    }

    public static ApiResponse<Void> erro(HttpStatus status, String message, List<String> errors) {
        return new ApiResponse<>(status.value(), message, null, errors);
    }
}
