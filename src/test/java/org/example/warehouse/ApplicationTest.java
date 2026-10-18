package org.example.warehouse;

import org.example.warehouse.application.CatalogService;
import org.example.warehouse.application.InventoryService;
import org.example.warehouse.domain.Customer;
import org.example.warehouse.domain.Product;
import org.example.warehouse.domain.Supplier;
import org.example.warehouse.domain.Warehouse;
import org.example.warehouse.infrastructure.memory.InMemoryCustomerRepository;
import org.example.warehouse.infrastructure.memory.InMemoryProductRepository;
import org.example.warehouse.infrastructure.memory.InMemorySupplierRepository;
import org.example.warehouse.infrastructure.memory.InMemoryWarehouseRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ApplicationTest {
    private final InMemoryProductRepository products = new InMemoryProductRepository();
    private final InMemoryCustomerRepository customers = new InMemoryCustomerRepository();
    private final InMemorySupplierRepository suppliers = new InMemorySupplierRepository();
    private final InMemoryWarehouseRepository warehouse = new InMemoryWarehouseRepository(new Warehouse("Склад"));
    private final CatalogService catalog = new CatalogService(products, customers, suppliers);
    private final InventoryService inventory = new InventoryService(warehouse, products, customers);
    private final Supplier supplier = catalog.registerSupplier("Поставка", "mail@example.com");
    private final Customer customer = catalog.registerCustomer("Анна", "12345");
    private final Product product = catalog.registerProduct("Молоко", "Продукты", supplier.id());

    @Test
    void registrationDoesNotCreateStockAndAvailabilityTracksShipments() {
        assertEquals(0, inventory.quantityOf(product.id()));
        assertEquals(1, inventory.allPositions().size());
        assertTrue(inventory.availablePositions().isEmpty());
        inventory.addUnit(product.id());
        inventory.addUnit(product.id());
        assertEquals(2, inventory.availablePositions().get(0).quantity());
        inventory.ship(product.id(), customer.id(), 2);
        assertTrue(inventory.availablePositions().isEmpty());
        assertEquals(1, inventory.allPositions().size());
        assertEquals(2, inventory.shipmentsFor(customer.id()).get(0).quantity());
    }

    @Test
    void missingReferencesDoNotModifyState() {
        UUID missing = UUID.randomUUID();
        var initial = warehouse.load();
        assertThrows(IllegalArgumentException.class, () -> inventory.addUnit(missing));
        assertThrows(IllegalArgumentException.class, () -> inventory.quantityOf(missing));
        assertThrows(IllegalArgumentException.class, () -> inventory.ship(missing, customer.id(), 1));
        assertThrows(IllegalArgumentException.class, () -> inventory.ship(product.id(), missing, 1));
        assertSame(initial, warehouse.load());
        assertThrows(IllegalArgumentException.class,
                () -> catalog.registerProduct("Другой товар", "Вид", missing));
        assertEquals(List.of(product), catalog.products());
    }

    @Test
    void failedShipmentDoesNotSavePartialResult() {
        inventory.addUnit(product.id());
        var initial = warehouse.load();
        assertThrows(IllegalArgumentException.class, () -> inventory.ship(product.id(), customer.id(), 2));
        assertSame(initial, warehouse.load());
        assertTrue(inventory.shipmentsFor(customer.id()).isEmpty());
    }

    @Test
    void groupingIncludesZeroStockAndKeepsTypesSeparate() {
        var bread = catalog.registerProduct("Хлеб", "Продукты", supplier.id());
        var hammer = catalog.registerProduct("Молоток", "Инструменты", supplier.id());
        inventory.addUnit(hammer.id());
        var groups = inventory.groupByType();
        assertEquals(List.of(product, bread), groups.get("Продукты").stream().map(p -> p.product()).toList());
        assertEquals(hammer, groups.get("Инструменты").get(0).product());
        assertEquals(1, groups.get("Инструменты").get(0).quantity());
        assertThrows(UnsupportedOperationException.class, () -> groups.clear());
        assertThrows(UnsupportedOperationException.class, () -> groups.get("Продукты").clear());
    }

    @Test
    void suppliersAndCustomersRetainTheirInformationAndIndividualHistory() {
        var other = catalog.registerCustomer("Борис", "67890");
        inventory.addUnit(product.id());
        inventory.ship(product.id(), customer.id(), 1);
        assertEquals(List.of(supplier), catalog.suppliers());
        assertEquals(List.of(customer, other), catalog.customers());
        assertEquals(supplier.id(), product.supplierId());
        assertTrue(inventory.shipmentsFor(other.id()).isEmpty());
        assertEquals(customer.id(), inventory.shipmentsFor(customer.id()).get(0).customerId());
    }

    @Test
    void duplicateIdsNeverOverwriteExistingRecords() {
        assertThrows(IllegalArgumentException.class,
                () -> products.add(new Product(product.id(), "Подмена", "Вид", supplier.id())));
        assertThrows(IllegalArgumentException.class,
                () -> customers.add(new Customer(customer.id(), "Подмена", "Контакт")));
        assertThrows(IllegalArgumentException.class,
                () -> suppliers.add(new Supplier(supplier.id(), "Подмена", "Контакт")));
        assertEquals(product, products.findById(product.id()).orElseThrow());
        assertEquals(customer, customers.findById(customer.id()).orElseThrow());
        assertEquals(supplier, suppliers.findById(supplier.id()).orElseThrow());
    }

    @Test
    void catalogListsAreImmutableSnapshotsAndRepositoriesAreIndependent() {
        var snapshot = catalog.products();
        catalog.registerProduct("Хлеб", "Продукты", supplier.id());
        assertEquals(List.of(product), snapshot);
        assertThrows(UnsupportedOperationException.class, snapshot::clear);
        assertThrows(UnsupportedOperationException.class, () -> catalog.customers().clear());
        assertThrows(UnsupportedOperationException.class, () -> catalog.suppliers().clear());
        assertTrue(new InMemoryProductRepository().findAll().isEmpty());
        assertTrue(new InMemoryCustomerRepository().findAll().isEmpty());
        assertTrue(new InMemorySupplierRepository().findAll().isEmpty());
        assertEquals(0, new InMemoryWarehouseRepository(new Warehouse("Другой"))
                .load().quantityOf(product.id()));
    }

    @Test
    void invalidCatalogInputDoesNotCreateRecords() {
        assertThrows(IllegalArgumentException.class, () -> catalog.registerSupplier(" ", "contact"));
        assertThrows(IllegalArgumentException.class, () -> catalog.registerCustomer("name", ""));
        assertThrows(IllegalArgumentException.class, () -> catalog.registerProduct("name", " ", supplier.id()));
        assertThrows(IllegalArgumentException.class, () -> catalog.registerProduct(null, "type", supplier.id()));
        assertEquals(1, catalog.suppliers().size());
        assertEquals(1, catalog.customers().size());
        assertEquals(1, catalog.products().size());
    }
}
