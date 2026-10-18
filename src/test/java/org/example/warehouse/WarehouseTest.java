package org.example.warehouse;

import org.example.warehouse.domain.Shipment;
import org.example.warehouse.domain.Warehouse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class WarehouseTest {
    private final UUID productId = UUID.randomUUID();
    private final UUID customerId = UUID.randomUUID();

    @Test
    void receivingAndShippingProduceIndependentSnapshots() {
        var empty = new Warehouse("Склад");
        var received = empty.addUnit(productId).addUnit(productId);
        var shipped = received.ship(productId, customerId, 2);

        assertEquals(0, empty.quantityOf(productId));
        assertEquals(2, received.quantityOf(productId));
        assertEquals(0, shipped.quantityOf(productId));
        assertTrue(received.shipments().isEmpty());
        assertEquals(List.of(new Shipment(productId, customerId, 2)), shipped.shipments());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, Integer.MIN_VALUE, 2})
    void invalidShipmentDoesNotChangeStockOrHistory(int quantity) {
        var warehouse = new Warehouse("Склад").addUnit(productId);
        assertThrows(IllegalArgumentException.class,
                () -> warehouse.ship(productId, customerId, quantity));
        assertEquals(1, warehouse.quantityOf(productId));
        assertTrue(warehouse.shipments().isEmpty());
    }

    @Test
    void cannotReceiveBeyondIntegerCapacity() {
        var warehouse = new Warehouse("Склад", Map.of(productId, Integer.MAX_VALUE), List.of());
        assertThrows(IllegalArgumentException.class, () -> warehouse.addUnit(productId));
        assertEquals(Integer.MAX_VALUE, warehouse.quantityOf(productId));
    }

    @Test
    void stateCannotBeMutatedThroughInputOrOutputCollections() {
        var stock = new HashMap<UUID, Integer>();
        stock.put(productId, 1);
        var shipments = new ArrayList<Shipment>();
        var warehouse = new Warehouse("Склад", stock, shipments);
        stock.put(productId, 99);
        shipments.add(new Shipment(productId, customerId, 1));

        assertEquals(1, warehouse.quantityOf(productId));
        assertTrue(warehouse.shipments().isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> warehouse.stock().clear());
        assertThrows(UnsupportedOperationException.class, () -> warehouse.shipments().clear());
    }

    @Test
    void negativeInitialStockIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new Warehouse("Склад", Map.of(productId, -1), List.of()));
    }
}
