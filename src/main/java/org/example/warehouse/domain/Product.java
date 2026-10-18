package org.example.warehouse.domain;

import java.util.Objects;
import java.util.UUID;

/** Позиция каталога. Остаток хранится отдельно, на складе. */
public record Product(UUID id, String name, String type, UUID supplierId) {
    public Product {
        Objects.requireNonNull(id, "id");
        name = Validation.requiredText(name, "Название товара");
        type = Validation.requiredText(type, "Вид товара");
        Objects.requireNonNull(supplierId, "supplierId");
    }
}
