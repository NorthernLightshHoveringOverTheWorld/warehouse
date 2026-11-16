package org.example.warehouse.infrastructure.jdbc;

import org.example.warehouse.application.port.SupplierRepository;
import org.example.warehouse.domain.Supplier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JdbcSupplierRepository implements SupplierRepository {
    private static final RowMapper<Supplier> MAPPER = (rs, row) -> new Supplier(
            rs.getObject("id", UUID.class), rs.getString("name"), rs.getString("contact"));
    private final JdbcTemplate jdbc;

    public JdbcSupplierRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override
    public void add(Supplier supplier) {
        int added = jdbc.update("INSERT INTO suppliers(id,name,contact) VALUES (?,?,?) ON CONFLICT(id) DO NOTHING",
                supplier.id(), supplier.name(), supplier.contact());
        if (added == 0) throw new IllegalArgumentException("Поставщик с таким id уже существует.");
    }

    @Override
    public Optional<Supplier> findById(UUID id) {
        return jdbc.query("SELECT id,name,contact FROM suppliers WHERE id=?", MAPPER, id).stream().findFirst();
    }

    @Override
    public List<Supplier> findAll() {
        return List.copyOf(jdbc.query("SELECT id,name,contact FROM suppliers ORDER BY registration_order", MAPPER));
    }
}
