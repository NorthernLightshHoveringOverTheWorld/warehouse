package org.example.warehouse.infrastructure.jdbc;

import org.example.warehouse.application.port.CustomerRepository;
import org.example.warehouse.domain.Customer;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JdbcCustomerRepository implements CustomerRepository {
    private static final RowMapper<Customer> MAPPER = (rs, row) -> new Customer(
            rs.getObject("id", UUID.class), rs.getString("name"), rs.getString("contact"));
    private final JdbcTemplate jdbc;

    public JdbcCustomerRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override
    public void add(Customer customer) {
        int added = jdbc.update("INSERT INTO customers(id,name,contact) VALUES (?,?,?) ON CONFLICT(id) DO NOTHING",
                customer.id(), customer.name(), customer.contact());
        if (added == 0) throw new IllegalArgumentException("Покупатель с таким id уже существует.");
    }

    @Override
    public Optional<Customer> findById(UUID id) {
        return jdbc.query("SELECT id,name,contact FROM customers WHERE id=?", MAPPER, id).stream().findFirst();
    }

    @Override
    public List<Customer> findAll() {
        return List.copyOf(jdbc.query("SELECT id,name,contact FROM customers ORDER BY registration_order", MAPPER));
    }
}
