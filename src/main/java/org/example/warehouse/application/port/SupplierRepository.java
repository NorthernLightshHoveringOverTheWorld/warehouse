package org.example.warehouse.application.port;

import org.example.warehouse.domain.Supplier;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** add запрещает повторный id; findAll возвращает неизменяемый снимок в порядке добавления. */
public interface SupplierRepository {
    void add(Supplier supplier);
    Optional<Supplier> findById(UUID id);
    List<Supplier> findAll();
}
