package com.avandesk.api.exception;

import com.avandesk.api.dto.ApiResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(RegraNegocioException.class)
    public ResponseEntity<ApiResponse<Void>> tratarRegraNegocio(RegraNegocioException ex) {
        return responder(HttpStatus.BAD_REQUEST, "Erro de validação", List.of(ex.getMessage()));
    }

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<ApiResponse<Void>> tratarRecursoNaoEncontrado(RecursoNaoEncontradoException ex) {
        return responder(HttpStatus.NOT_FOUND, ex.getMessage(), null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> tratarErroInesperado(Exception ex) {
        logger.error("Erro inesperado ao processar a requisição", ex);
        return responder(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno", null);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpHeaders headers,
                                                                  HttpStatusCode status, WebRequest request) {
        List<String> erros = ex.getBindingResult().getFieldErrors().stream()
            .map(erro -> "Campo '%s' %s.".formatted(paraSnakeCase(erro.getField()), erro.getDefaultMessage()))
            .toList();
        ApiResponse<Void> corpo = ApiResponse.erro(HttpStatus.BAD_REQUEST, "Erro de validação", erros);
        return handleExceptionInternal(ex, corpo, headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex, HttpHeaders headers,
                                                                  HttpStatusCode status, WebRequest request) {
        ApiResponse<Void> corpo = ApiResponse.erro(HttpStatus.BAD_REQUEST, "Requisição inválida",
            List.of("Corpo da requisição malformado ou com valor não permitido em algum campo."));
        return handleExceptionInternal(ex, corpo, headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers,
                                                             HttpStatusCode statusCode, WebRequest request) {
        HttpStatus status = HttpStatus.valueOf(statusCode.value());
        if (status.is5xxServerError()) {
            logger.error("Erro interno do Spring MVC ao processar a requisição", ex);
        }
        if (!(body instanceof ApiResponse)) {
            body = ApiResponse.erro(status, mensagemPara(status), null);
        }
        return super.handleExceptionInternal(ex, body, headers, statusCode, request);
    }

    private static String mensagemPara(HttpStatus status) {
        return switch (status) {
            case NOT_FOUND -> "Recurso não encontrado";
            case METHOD_NOT_ALLOWED -> "Método não permitido";
            case UNSUPPORTED_MEDIA_TYPE -> "Tipo de conteúdo não suportado";
            default -> status.is4xxClientError() ? "Requisição inválida" : "Erro interno";
        };
    }

    private static String paraSnakeCase(String campo) {
        return campo.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase();
    }

    private ResponseEntity<ApiResponse<Void>> responder(HttpStatus status, String message, List<String> errors) {
        return ResponseEntity.status(status).body(ApiResponse.erro(status, message, errors));
    }
}
