package org.example.warehouse.application.port;

import org.example.warehouse.domain.Product;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** add запрещает повторный id; findAll возвращает неизменяемый снимок в порядке добавления. */
public interface ProductRepository {
    void add(Product product);
    Optional<Product> findById(UUID id);
    List<Product> findAll();
}
