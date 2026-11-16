package org.example.warehouse;

import org.example.warehouse.application.*;
import org.example.warehouse.application.port.*;
import org.example.warehouse.domain.*;
import org.example.warehouse.infrastructure.jdbc.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.*;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

/** Opt-in, real PostgreSQL. Refuses to run against the application's database. */
@EnabledIfEnvironmentVariable(named="TEST_DATABASE_URL", matches="jdbc:postgresql://[^/]+/warehouse_test")
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
class PostgresIntegrationTest {
    @DynamicPropertySource static void database(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", () -> System.getenv("TEST_DATABASE_URL"));
    }
    @Autowired JdbcTemplate jdbc;
    @Autowired CatalogService catalog;
    @Autowired InventoryService inventory;
    @Autowired WarehouseRepository warehouses;
    @Autowired ProductRepository products;
    @Autowired CustomerRepository customers;
    @Autowired SupplierRepository suppliers;
    @Autowired PlatformTransactionManager manager;
    @Autowired TestRestTemplate http;
    private Product product;
    private Customer customer;

    @BeforeEach void prepare() {
        assertEquals("warehouse_test", jdbc.queryForObject("SELECT current_database()", String.class));
        jdbc.execute("TRUNCATE shipments,stock,products,customers,suppliers RESTART IDENTITY CASCADE");
        var supplier = catalog.registerSupplier("Поставщик", "contact");
        product = catalog.registerProduct("Молоко", "Продукты", supplier.id());
        customer = catalog.registerCustomer("Иван", "123");
    }

    @Test void dataPersistsAcrossNewRepositoryInstances() {
        inventory.receive(product.id(), 12);
        inventory.ship(product.id(), customer.id(), 3);
        var reconnected = new JdbcWarehouseRepository(jdbc, manager);
        assertEquals(9, reconnected.load().quantityOf(product.id()));
        assertEquals(List.of(new Shipment(product.id(), customer.id(), 3)), reconnected.load().shipments());
        assertEquals(product, new JdbcProductRepository(jdbc).findById(product.id()).orElseThrow());
        assertEquals(customer, new JdbcCustomerRepository(jdbc).findById(customer.id()).orElseThrow());
        assertNotNull(reconnected.shipmentRows().get(0).createdAt());
    }

    @Test void duplicateIdsAndImmutableListsFollowRepositoryContracts() {
        assertThrows(IllegalArgumentException.class, () -> products.add(product));
        assertThrows(IllegalArgumentException.class, () -> customers.add(customer));
        var supplier = suppliers.findAll().get(0);
        assertThrows(IllegalArgumentException.class, () -> suppliers.add(supplier));
        assertThrows(UnsupportedOperationException.class, () -> products.findAll().clear());
        assertThrows(UnsupportedOperationException.class, () -> customers.findAll().clear());
        assertThrows(UnsupportedOperationException.class, () -> suppliers.findAll().clear());
        var snapshot = products.findAll();
        var next = catalog.registerProduct("Хлеб", "Продукты", supplier.id());
        assertEquals(List.of(product), snapshot);
        assertEquals(List.of(product, next), products.findAll());
        assertTrue(products.findById(UUID.randomUUID()).isEmpty());
    }

    @Test void databaseRejectsBrokenLinksAndNegativeQuantities() {
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(
                "INSERT INTO stock VALUES (1,?,?)", UUID.randomUUID(), 2));
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(
                "INSERT INTO stock VALUES (1,?,?)", product.id(), -1));
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(
                "INSERT INTO shipments(warehouse_id,product_id,customer_id,quantity) VALUES (1,?,?,0)",
                product.id(), customer.id()));
    }

    @Test void failureDuringSqlWriteRollsBackStockAndWarehouseName() {
        inventory.receive(product.id(), 5);
        var initial = warehouses.load();
        var invalid = new Warehouse("Should roll back", Map.of(product.id(), 1),
                List.of(new Shipment(product.id(), UUID.randomUUID(), 4)));
        assertThrows(DataIntegrityViolationException.class, () -> warehouses.save(invalid));
        assertEquals(initial, warehouses.load());
    }

    @Test void concurrentReceiptsDoNotLoseUpdates() throws Exception {
        var pool = Executors.newFixedThreadPool(6);
        try {
            var work = new ArrayList<Callable<Void>>();
            for (int n=0; n<12; n++) work.add(() -> { inventory.receive(product.id(), 3); return null; });
            for (var result : pool.invokeAll(work)) result.get(20, TimeUnit.SECONDS);
            assertEquals(36, inventory.quantityOf(product.id()));
        } finally { pool.shutdownNow(); }
    }

    @Test void concurrentShipmentsCannotSellTheLastUnitTwice() throws Exception {
        inventory.receive(product.id(), 1);
        var pool = Executors.newFixedThreadPool(2);
        var start = new CountDownLatch(1);
        Callable<Boolean> ship = () -> {
            start.await();
            try { inventory.ship(product.id(), customer.id(), 1); return true; }
            catch (IllegalArgumentException expected) { return false; }
        };
        try {
            var first=pool.submit(ship);var second=pool.submit(ship);start.countDown();
            assertNotEquals(first.get(20, TimeUnit.SECONDS), second.get(20, TimeUnit.SECONDS));
            assertEquals(0, inventory.quantityOf(product.id()));
            assertEquals(1, inventory.shipmentsFor(customer.id()).size());
        } finally { pool.shutdownNow(); }
    }

    @Test void httpRejectsFractionsAndReturnsUiAndGroups() {
        var response = http.postForEntity("/api/receipts", Map.of("productId", product.id(), "quantity", 1.5), Map.class);
        assertEquals(400, response.getStatusCode().value());
        assertEquals(0, inventory.quantityOf(product.id()));
        var received = http.postForEntity("/api/receipts", Map.of("productId", product.id(), "quantity", 5), Map.class);
        assertEquals(200, received.getStatusCode().value());
        assertEquals(5, received.getBody().get("quantity"));
        assertTrue(http.getForObject("/api/inventory/groups", Map.class).containsKey("Продукты"));
        assertTrue(http.getForObject("/", String.class).contains("Товары и остатки"));
    }
}
