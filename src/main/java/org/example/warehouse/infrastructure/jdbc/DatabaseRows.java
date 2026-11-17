package org.example.warehouse.infrastructure.jdbc;

import org.example.warehouse.domain.Shipment;
import java.time.OffsetDateTime;
import java.util.UUID;


public final class DatabaseRows {
    private DatabaseRows() { }
    public record WarehouseRow(int id, String name) { }
    public record StockRow(int warehouseId, UUID productId, int quantity) { }
    public record ShipmentRow(long id, int warehouseId, UUID productId, UUID customerId,
                              int quantity, OffsetDateTime createdAt) {
        public Shipment toDomain() { return new Shipment(productId, customerId, quantity); }
    }
}
