package org.example.warehouse.domain;

import java.util.Objects;
import java.util.UUID;

public record Supplier(UUID id, String name, String contact) {
    public Supplier {
        Objects.requireNonNull(id, "id");
        name = Validation.requiredText(name, "Название поставщика");
        contact = Validation.requiredText(contact, "Контакт поставщика");
    }
}
