package com.workshop.proxy.controller;

import com.workshop.proxy.service.ServiceException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(ServiceException.class)
    public ResponseEntity<ApiError> handleServiceError(ServiceException error) {
        return ResponseEntity.status(error.getHttpStatus()).body(new ApiError(error.getHttpStatus(), error.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpectedError(Exception error) {
        logger.error("Unexpected error", error);
        return ResponseEntity.internalServerError().body(new ApiError(500, "Internal server error"));
    }

    // Spring's own errors (bad JSON, wrong filter value, 404, 405) use the same JSON format
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception error, Object body, HttpHeaders headers,
                                                             HttpStatusCode status, WebRequest request) {
        ApiError apiError = new ApiError(status.value(), HttpStatus.valueOf(status.value()).getReasonPhrase());
        return new ResponseEntity<>(apiError, headers, status);
    }

    public record ApiError(int status, String message) {
    }
}
