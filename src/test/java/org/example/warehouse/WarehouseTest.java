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
        var firstReceipt = empty.receive(productId, 5);
        var received = firstReceipt.receive(productId, 7);
        var shipped = received.ship(productId, customerId, 12);

        assertEquals(0, empty.quantityOf(productId));
        assertEquals(5, firstReceipt.quantityOf(productId));
        assertEquals(12, received.quantityOf(productId));
        assertEquals(0, shipped.quantityOf(productId));
        assertTrue(received.shipments().isEmpty());
        assertEquals(List.of(new Shipment(productId, customerId, 12)), shipped.shipments());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, Integer.MIN_VALUE, 2})
    void invalidShipmentDoesNotChangeStockOrHistory(int quantity) {
        var warehouse = new Warehouse("Склад").receive(productId, 1);
        assertThrows(IllegalArgumentException.class,
                () -> warehouse.ship(productId, customerId, quantity));
        assertEquals(1, warehouse.quantityOf(productId));
        assertTrue(warehouse.shipments().isEmpty());
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, Integer.MIN_VALUE})
    void invalidReceiptDoesNotChangeStockOrHistory(int quantity) {
        var warehouse = new Warehouse("Склад").receive(productId, 5).ship(productId, customerId, 1);
        assertThrows(IllegalArgumentException.class, () -> warehouse.receive(productId, quantity));
        assertEquals(4, warehouse.quantityOf(productId));
        assertEquals(List.of(new Shipment(productId, customerId, 1)), warehouse.shipments());
    }

    @ParameterizedTest
    @ValueSource(ints = {6, Integer.MAX_VALUE})
    void cannotReceiveBeyondIntegerCapacity(int quantity) {
        var warehouse = new Warehouse("Склад", Map.of(productId, Integer.MAX_VALUE - 5), List.of());
        assertThrows(IllegalArgumentException.class, () -> warehouse.receive(productId, quantity));
        assertEquals(Integer.MAX_VALUE - 5, warehouse.quantityOf(productId));
        assertTrue(warehouse.shipments().isEmpty());
    }

    @Test
    void canReceiveExactlyUpToIntegerCapacity() {
        var warehouse = new Warehouse("Склад").receive(productId, Integer.MAX_VALUE - 5);
        var full = warehouse.receive(productId, 5);
        assertEquals(Integer.MAX_VALUE, full.quantityOf(productId));
        assertThrows(IllegalArgumentException.class, () -> full.receive(productId, 1));
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
