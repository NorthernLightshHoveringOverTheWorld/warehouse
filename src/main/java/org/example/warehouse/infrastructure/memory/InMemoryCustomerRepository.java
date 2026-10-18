package org.example.warehouse.infrastructure.memory;

import org.example.warehouse.application.port.CustomerRepository;
import org.example.warehouse.domain.Customer;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

public final class InMemoryCustomerRepository implements CustomerRepository {
    private final Map<UUID, Customer> customers = new LinkedHashMap<>();

    @Override
    public void add(Customer customer) {
        Objects.requireNonNull(customer);
        if (customers.putIfAbsent(customer.id(), customer) != null) {
            throw new IllegalArgumentException("Покупатель с таким id уже существует.");
        }
    }

    @Override
    public Optional<Customer> findById(UUID id) {
        return Optional.ofNullable(customers.get(Objects.requireNonNull(id)));
    }

    @Override
    public List<Customer> findAll() {
        return List.copyOf(customers.values());
    }
}
