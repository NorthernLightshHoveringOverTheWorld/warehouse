package org.example.warehouse.domain;

import java.util.Objects;
import java.util.UUID;

public record Customer(UUID id, String name, String contact) {
    public Customer {
        Objects.requireNonNull(id, "id");
        name = Validation.requiredText(name, "Имя покупателя");
        contact = Validation.requiredText(contact, "Контакт покупателя");
    }
}
