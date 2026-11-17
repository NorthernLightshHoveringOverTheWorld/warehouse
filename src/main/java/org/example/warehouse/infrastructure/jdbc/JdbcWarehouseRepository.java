package org.example.warehouse.infrastructure.jdbc;

import org.example.warehouse.application.port.WarehouseRepository;
import org.example.warehouse.domain.Warehouse;
import org.example.warehouse.domain.Shipment;
import org.example.warehouse.infrastructure.jdbc.DatabaseRows.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.UnaryOperator;

@Repository
public class JdbcWarehouseRepository implements WarehouseRepository {
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transaction;

    public JdbcWarehouseRepository(JdbcTemplate jdbc, PlatformTransactionManager manager) {
        this.jdbc = jdbc;
        this.transaction = new TransactionTemplate(manager);
        this.transaction.setTimeout(15);
    }

    @Override
    public Warehouse load() {
        return transaction.execute(status -> {
            lock("FOR SHARE");
            return read();
        });
    }

    @Override
    public void update(UnaryOperator<Warehouse> operation) {
        transaction.executeWithoutResult(status -> {
            lock("FOR UPDATE");
            Warehouse current = read();
            write(current, Objects.requireNonNull(operation.apply(current)));
        });
    }

    @Override
    public void save(Warehouse warehouse) {
        transaction.executeWithoutResult(status -> {
            lock("FOR UPDATE");
            write(read(), Objects.requireNonNull(warehouse));
        });
    }

    private void lock(String mode) {
        jdbc.queryForObject("SELECT id FROM warehouses WHERE id=1 " + mode, Integer.class);
    }

    private Warehouse read() {
        WarehouseRow row = jdbc.queryForObject("SELECT id,name FROM warehouses WHERE id=1",
                (rs, n) -> new WarehouseRow(rs.getInt("id"), rs.getString("name")));
        var stock = new HashMap<UUID, Integer>();
        jdbc.query("SELECT warehouse_id,product_id,quantity FROM stock WHERE warehouse_id=1",
                (rs, n) -> new StockRow(rs.getInt("warehouse_id"), rs.getObject("product_id", UUID.class),
                        rs.getInt("quantity")))
                .forEach(item -> stock.put(item.productId(), item.quantity()));
        List<Shipment> history = shipmentRows().stream().map(ShipmentRow::toDomain).toList();
        return new Warehouse(Objects.requireNonNull(row).name(), stock, history);
    }

    public List<ShipmentRow> shipmentRows() {
        return List.copyOf(jdbc.query("SELECT * FROM shipments WHERE warehouse_id=1 ORDER BY id",
                (rs, n) -> new ShipmentRow(rs.getLong("id"), rs.getInt("warehouse_id"),
                        rs.getObject("product_id", UUID.class), rs.getObject("customer_id", UUID.class),
                        rs.getInt("quantity"), rs.getObject("created_at", OffsetDateTime.class))));
    }

    private void write(Warehouse current, Warehouse updated) {
        int count = current.shipments().size();
        if (updated.shipments().size() < count || !updated.shipments().subList(0, count).equals(current.shipments())) {
            jdbc.update("DELETE FROM shipments WHERE warehouse_id=1");
            count = 0;
        }
        jdbc.update("UPDATE warehouses SET name=? WHERE id=1", updated.name());
        for (var entry : updated.stock().entrySet()) {
            jdbc.update("""
                    INSERT INTO stock(warehouse_id,product_id,quantity) VALUES (1,?,?)
                    ON CONFLICT(warehouse_id,product_id) DO UPDATE SET quantity=EXCLUDED.quantity
                    """, entry.getKey(), entry.getValue());
        }
        for (UUID removed : current.stock().keySet()) {
            if (!updated.stock().containsKey(removed)) {
                jdbc.update("DELETE FROM stock WHERE warehouse_id=1 AND product_id=?", removed);
            }
        }
        for (Shipment shipment : updated.shipments().subList(count, updated.shipments().size())) {
            jdbc.update("INSERT INTO shipments(warehouse_id,product_id,customer_id,quantity) VALUES (1,?,?,?)",
                    shipment.productId(), shipment.customerId(), shipment.quantity());
        }
    }
}
