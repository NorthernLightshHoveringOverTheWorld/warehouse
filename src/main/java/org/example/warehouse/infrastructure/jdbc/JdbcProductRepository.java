package org.example.warehouse.infrastructure.jdbc;

import org.example.warehouse.application.port.ProductRepository;
import org.example.warehouse.domain.Product;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JdbcProductRepository implements ProductRepository {
    private static final RowMapper<Product> MAPPER = (rs, row) -> new Product(
            rs.getObject("id", UUID.class), rs.getString("name"), rs.getString("type"),
            rs.getObject("supplier_id", UUID.class));
    private final JdbcTemplate jdbc;

    public JdbcProductRepository(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override
    public void add(Product product) {
        int added = jdbc.update("INSERT INTO products(id,name,type,supplier_id) VALUES (?,?,?,?) ON CONFLICT(id) DO NOTHING",
                product.id(), product.name(), product.type(), product.supplierId());
        if (added == 0) throw new IllegalArgumentException("Товар с таким id уже существует.");
    }

    @Override
    public Optional<Product> findById(UUID id) {
        return jdbc.query("SELECT id,name,type,supplier_id FROM products WHERE id=?", MAPPER, id).stream().findFirst();
    }

    @Override
    public List<Product> findAll() {
        return List.copyOf(jdbc.query("SELECT id,name,type,supplier_id FROM products ORDER BY registration_order", MAPPER));
    }
}
