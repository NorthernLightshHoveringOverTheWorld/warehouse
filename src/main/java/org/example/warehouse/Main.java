package org.example.warehouse;

import org.example.warehouse.application.CatalogService;
import org.example.warehouse.application.InventoryService;
import org.example.warehouse.console.ConsoleApplication;
import org.example.warehouse.domain.Warehouse;
import org.example.warehouse.infrastructure.memory.InMemoryCustomerRepository;
import org.example.warehouse.infrastructure.memory.InMemoryProductRepository;
import org.example.warehouse.infrastructure.memory.InMemorySupplierRepository;
import org.example.warehouse.infrastructure.memory.InMemoryWarehouseRepository;

import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;


public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        var products = new InMemoryProductRepository();
        var customers = new InMemoryCustomerRepository();
        var suppliers = new InMemorySupplierRepository();
        var warehouse = new InMemoryWarehouseRepository(new Warehouse("Основной склад"));

        var catalog = new CatalogService(products, customers, suppliers);
        var inventory = new InventoryService(warehouse, products, customers);
        var input = new Scanner(System.in, StandardCharsets.UTF_8);
        var output = new PrintWriter(System.out, true, StandardCharsets.UTF_8);
        new ConsoleApplication(catalog, inventory, input, output).run();
    }
}
