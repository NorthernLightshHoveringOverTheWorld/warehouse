package org.example.warehouse.web;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.TransactionException;
import java.util.Map;

@RestControllerAdvice
public class ApiErrors {
    private static final Logger LOG = LoggerFactory.getLogger(ApiErrors.class);

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<Map<String, String>> invalid(IllegalArgumentException exception) {
        return error(400, exception.getMessage());
    }
    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class})
    ResponseEntity<Map<String, String>> invalidInput(Exception exception) {
        return error(400, "Проверьте поля: заполните обязательные значения, количество должно быть положительным целым числом.");
    }
    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<Map<String, String>> conflict(DataIntegrityViolationException exception) {
        return error(409, "Не удалось сохранить запись: проверьте связанные данные и уникальность идентификатора.");
    }
    @ExceptionHandler({DataAccessException.class, TransactionException.class})
    ResponseEntity<Map<String, String>> database(Exception exception) {
        LOG.error("Database operation failed", exception);
        return error(503, "База данных временно недоступна. Обновите данные перед повторной операцией.");
    }
    private ResponseEntity<Map<String, String>> error(int status, String message) {
        return ResponseEntity.status(status).body(Map.of("message", message));
    }
}
