package org.example.warehouse;

import org.example.warehouse.application.CatalogService;
import org.example.warehouse.application.InventoryService;
import org.example.warehouse.application.port.*;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class WebApplication {
    public static void main(String[] args) {
        SpringApplication.run(WebApplication.class, args);
    }

    @Bean
    CatalogService catalogService(ProductRepository products, CustomerRepository customers,
                                  SupplierRepository suppliers) {
        return new CatalogService(products, customers, suppliers);
    }

    @Bean
    InventoryService inventoryService(WarehouseRepository warehouse, ProductRepository products,
                                      CustomerRepository customers) {
        return new InventoryService(warehouse, products, customers);
    }
}
