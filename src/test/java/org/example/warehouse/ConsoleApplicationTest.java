package org.example.warehouse;

import org.example.warehouse.application.CatalogService;
import org.example.warehouse.application.InventoryService;
import org.example.warehouse.console.ConsoleApplication;
import org.example.warehouse.domain.Warehouse;
import org.example.warehouse.infrastructure.memory.InMemoryCustomerRepository;
import org.example.warehouse.infrastructure.memory.InMemoryProductRepository;
import org.example.warehouse.infrastructure.memory.InMemorySupplierRepository;
import org.example.warehouse.infrastructure.memory.InMemoryWarehouseRepository;
import org.junit.jupiter.api.Test;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;

class ConsoleApplicationTest {
    private final InMemoryProductRepository products = new InMemoryProductRepository();
    private final InMemoryCustomerRepository customers = new InMemoryCustomerRepository();
    private final CatalogService catalog = new CatalogService(products, customers, new InMemorySupplierRepository());
    private final InventoryService inventory = new InventoryService(
            new InMemoryWarehouseRepository(new Warehouse("Склад")), products, customers);

    @Test
    void fullScenarioRunsThroughTheConsole() {
        String output = run(String.join("\n",
                "1", "Поставщик", "s@example.com",
                "2", "Анна", "12345",
                "3", "1", "Молоко", "Продукты",
                "4", "1", "4", "1", "6", "7", "1", "8", "9",
                "5", "1", "1", "1", "10", "11", "0"));
        assertFalse(output.contains("Ошибка:"));
        assertTrue(output.contains("В наличии: 2 шт."));
        assertTrue(output.contains("Вид: Продукты"));
        assertTrue(output.contains("s@example.com"));
        assertTrue(output.contains("Получено: Молоко | 1 шт."));
        assertTrue(output.contains("Работа завершена."));
        assertEquals(1, inventory.quantityOf(catalog.products().get(0).id()));
    }

    @Test
    void invalidCommandsAndSelectionsAllowAnotherAttempt() {
        var supplier = catalog.registerSupplier("Поставщик", "Контакт");
        var product = catalog.registerProduct("Молоко", "Продукты", supplier.id());
        catalog.registerCustomer("Анна", "Контакт");
        String output = run(String.join("\n", "unknown", "4", "text", "4", "9",
                "5", "1", "1", "-1", "4", "1", "0"));
        assertTrue(output.contains("Неизвестная команда"));
        assertTrue(output.contains("Введите целое число"));
        assertTrue(output.contains("Номер записи вне списка"));
        assertTrue(output.contains("Количество должно быть положительным"));
        assertEquals(1, inventory.quantityOf(product.id()));
    }

    @Test
    void emptyCatalogsAndEndOfInputAreHandled() {
        String output = run("6\n8\n9\n10\n4\n");
        assertTrue(output.contains("Позиций нет."));
        assertTrue(output.contains("Поставщиков нет."));
        assertTrue(output.contains("Покупателей нет."));
        assertTrue(output.contains("Список пуст."));
        assertTrue(output.contains("Ввод завершён."));
    }

    @Test
    void endOfInputDuringRegistrationDoesNotCreatePartialRecord() {
        String output = run("1\nПоставщик\n");
        assertTrue(output.contains("Ввод завершён."));
        assertTrue(catalog.suppliers().isEmpty());
    }

    private String run(String input) {
        var output = new StringWriter();
        new ConsoleApplication(catalog, inventory, new Scanner(input), new PrintWriter(output)).run();
        return output.toString();
    }
}
