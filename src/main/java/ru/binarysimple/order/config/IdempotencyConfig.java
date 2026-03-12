package ru.binarysimple.order.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

/**
 * Глобальный обработчик исключений для идемпотентности.
 * Обрабатывает DataIntegrityViolationException при создании записи идемпотентности
 */
@Slf4j
@RestControllerAdvice
public class IdempotencyConfig {

    /**
     * Обработка одновременных запросов с одним идемпотентным ключом.
     * Если два запроса с одинаковым ключом приходят почти одновременно,
     * второй получит ошибку уникального constraint (unique constraint violation).
     * В этом случае возвращаем 409 Conflict.
     */
    @ExceptionHandler(Exception.class)
    public Object handleIdempotencyConflict(Exception e) throws Exception {
        // Проверяем, является ли это ошибкой уникального constraint
        String message = e.getMessage();
        
        if (message != null && (message.contains("Duplicate entry") || 
                message.contains("unique constraint") || 
                message.contains("key") ||
                message.contains("duplicate"))) {
            log.warn("Idempotency conflict detected: {}", message);
            return new ErrorResponse(
                "Idempotency conflict detected",
                "Request with this idempotency key is already being processed", 
                HttpStatus.CONFLICT
            );
        }
        
        // Передаём остальные исключения как есть
        throw e;
    }

    private record ErrorResponse(String message, String detail, HttpStatus status) {
    }
}