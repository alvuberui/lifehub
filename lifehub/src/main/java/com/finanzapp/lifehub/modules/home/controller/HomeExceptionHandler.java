package com.finanzapp.lifehub.modules.home.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestControllerAdvice(basePackageClasses = HomeController.class)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class HomeExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(HomeExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationErrors(MethodArgumentNotValidException ex) {
        List<Map<String, String>> errors = new ArrayList<>();
        ex.getBindingResult().getFieldErrors().forEach(error -> errors.add(Map.of(
                "field", error.getField(),
                "message", messageOrDefault(error.getDefaultMessage())
        )));
        ex.getBindingResult().getGlobalErrors().forEach(error -> errors.add(Map.of(
                "field", error.getObjectName(),
                "message", messageOrDefault(error.getDefaultMessage())
        )));

        String reason = errors.stream()
                .map(error -> error.get("field") + ": " + error.get("message"))
                .reduce((left, right) -> left + ". " + right)
                .orElse("Revisa los datos obligatorios del artículo");

        return response(400, "Bad Request", "El artículo no es válido: " + reason, Map.of("errors", errors));
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<Map<String, Object>> handleMissingPart(MissingServletRequestPartException ex) {
        return response(400, "Bad Request",
                "Falta una parte obligatoria de la solicitud: " + ex.getRequestPartName(), Map.of());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, Object>> handleUploadTooLarge(MaxUploadSizeExceededException ex) {
        return response(413, "Payload Too Large", "El archivo supera el tamaño máximo permitido de 10 MB", Map.of());
    }

    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class
    })
    public ResponseEntity<Map<String, Object>> handleMalformedRequest(Exception ex) {
        return response(400, "Bad Request",
                "Los datos enviados no tienen un formato válido. Revisa fechas, categoría, estado y tipos de documento.",
                Map.of());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidRequest(IllegalArgumentException ex) {
        String message = ex.getMessage() == null || ex.getMessage().isBlank()
                ? "La solicitud contiene datos no válidos"
                : ex.getMessage();
        return response(400, "Bad Request", message, Map.of());
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleStatusException(ResponseStatusException ex) {
        int status = ex.getStatusCode().value();
        return response(status, ex.getStatusCode().toString(), ex.getReason() == null ? "No se puede completar la operación" : ex.getReason(), Map.of());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleConflict(DataIntegrityViolationException ex) {
        return response(409, "Conflict",
                "No se ha podido guardar el artículo porque entra en conflicto con datos existentes.", Map.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleUnexpected(Exception ex) {
        log.error("Error no controlado en home", ex);
        return response(500, "Internal Server Error",
                "No se ha podido completar la operación. Inténtalo de nuevo; si persiste, contacta con soporte.", Map.of());
    }

    private static String messageOrDefault(String message) {
        return message == null || message.isBlank() ? "Valor inválido" : message;
    }

    private ResponseEntity<Map<String, Object>> response(
            int status,
            String error,
            String message,
            Map<String, Object> extra
    ) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now().toString());
        body.put("status", status);
        body.put("error", error);
        body.put("message", message);
        body.putAll(extra);
        return ResponseEntity.status(status).body(body);
    }
}
