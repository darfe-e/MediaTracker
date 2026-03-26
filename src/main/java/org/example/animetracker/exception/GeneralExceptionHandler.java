package org.example.animetracker.exception;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.example.animetracker.dto.error.ErrorResponseDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@ControllerAdvice
public class GeneralExceptionHandler {

  @ExceptionHandler(ResponseStatusException.class)
  public ResponseEntity<ErrorResponseDto> handleResponseStatusException(
      ResponseStatusException ex, WebRequest request) {
    String path = request.getDescription(false).replace("uri=", "");
    log.warn("{} - {}", ex.getStatusCode(), ex.getReason());
    String error = ex.getReason() != null ? ex.getReason()
        : HttpStatus.valueOf(ex.getStatusCode().value()).getReasonPhrase();
    ErrorResponseDto errorResponse = new ErrorResponseDto(
        LocalDateTime.now(),
        ex.getStatusCode().value(),
        error,
        ex.getMessage(),
        path
    );
    return new ResponseEntity<>(errorResponse, ex.getStatusCode());
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponseDto> handleException(
      Exception ex, WebRequest request) {
    String path = request.getDescription(false).replace("uri=", "");
    log.error("Unexpected error at {}: {}", path, ex.getMessage(), ex);
    ErrorResponseDto errorResponse = new ErrorResponseDto(
        LocalDateTime.now(),
        HttpStatus.BAD_REQUEST.value(),
        "Unexpected exception",
        ex.getMessage(),
        path
    );
    return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
  }

  @ExceptionHandler(RuntimeException.class)
  public ResponseEntity<ErrorResponseDto> handleRuntimeException(
      RuntimeException ex, WebRequest request) {
    String path = request.getDescription(false).replace("uri=", "");
    ErrorResponseDto errorResponse = new ErrorResponseDto(
        LocalDateTime.now(),
        HttpStatus.INTERNAL_SERVER_ERROR.value(),
        "Internal Server Error",
        ex.getMessage(),
        path
    );
    return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponseDto> handleValidationExceptions(
      MethodArgumentNotValidException ex, WebRequest request) {
    String path = request.getDescription(false).replace("uri=", "");
    Map<String, String> errors = new HashMap<>();
    ex.getBindingResult().getFieldErrors().forEach(error ->
        errors.put(error.getField(), error.getDefaultMessage())
    );
    log.warn("Validation failed: {} - path: {}", errors, path);
    String message = "Validation failed: " + errors;
    ErrorResponseDto errorResponse = new ErrorResponseDto(
        LocalDateTime.now(),
        HttpStatus.BAD_REQUEST.value(),
        "Validation Error",
        message,
        path
    );
    return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
  }
}
