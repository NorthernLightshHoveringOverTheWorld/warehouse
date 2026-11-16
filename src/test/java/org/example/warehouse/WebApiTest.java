package org.example.warehouse;

import org.example.warehouse.application.*;
import org.example.warehouse.domain.Warehouse;
import org.example.warehouse.infrastructure.memory.*;
import org.example.warehouse.web.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class WebApiTest {
    private MockMvc mvc;
    private CatalogService catalog;
    private InventoryService inventory;

    @BeforeEach void setUp() {
        var products = new InMemoryProductRepository();
        var customers = new InMemoryCustomerRepository();
        catalog = new CatalogService(products, customers, new InMemorySupplierRepository());
        inventory = new InventoryService(new InMemoryWarehouseRepository(new Warehouse("Склад")), products, customers);
        mvc = MockMvcBuilders.standaloneSetup(new WarehouseController(catalog, inventory))
                .setControllerAdvice(new ApiErrors()).build();
    }

    @Test void registrationValidatesRequiredFields() throws Exception {
        mvc.perform(post("/api/customers").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"\",\"contact\":\"123\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").isString());
        mvc.perform(post("/api/customers").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Иван\",\"contact\":\"123\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.name").value("Иван"));
    }

    @Test void receiptAndFailedShipmentExposeConsistentStock() throws Exception {
        var supplier = catalog.registerSupplier("Завод", "123");
        var product = catalog.registerProduct("Молоко", "Продукты", supplier.id());
        var customer = catalog.registerCustomer("Иван", "456");
        mvc.perform(post("/api/receipts").contentType(MediaType.APPLICATION_JSON)
                .content("{\"productId\":\"" + product.id() + "\",\"quantity\":7}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.quantity").value(7));
        mvc.perform(post("/api/shipments").contentType(MediaType.APPLICATION_JSON)
                .content("{\"productId\":\"" + product.id() + "\",\"customerId\":\"" + customer.id() + "\",\"quantity\":8}"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/inventory/" + product.id())).andExpect(jsonPath("$.quantity").value(7));
        mvc.perform(get("/api/shipments")).andExpect(jsonPath("$").isEmpty());
    }

    @Test void malformedIdentifiersAndMissingQuantitiesAreRejected() throws Exception {
        mvc.perform(get("/api/inventory/not-a-uuid")).andExpect(status().isBadRequest());
        mvc.perform(post("/api/receipts").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }
}
