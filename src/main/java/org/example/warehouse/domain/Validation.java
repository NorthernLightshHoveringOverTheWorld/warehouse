package org.example.warehouse.domain;

final class Validation {
    private Validation() {
    }

    static String requiredText(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " не может быть пустым.");
        }
        return value.strip();
    }
}
