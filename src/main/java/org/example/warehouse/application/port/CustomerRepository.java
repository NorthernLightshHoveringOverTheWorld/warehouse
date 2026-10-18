package org.example.warehouse.application.port;

import org.example.warehouse.domain.Customer;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** add запрещает повторный id; findAll возвращает неизменяемый снимок в порядке добавления. */
public interface CustomerRepository {
    void add(Customer customer);
    Optional<Customer> findById(UUID id);
    List<Customer> findAll();
}
