package org.example.warehouse.console;

import org.example.warehouse.application.CatalogService;
import org.example.warehouse.application.InventoryService;
import org.example.warehouse.application.StockPosition;
import org.example.warehouse.domain.Customer;
import org.example.warehouse.domain.Product;
import org.example.warehouse.domain.Supplier;

import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Scanner;
import java.util.function.Function;


public final class ConsoleApplication {
    private final CatalogService catalog;
    private final InventoryService inventory;
    private final Scanner input;
    private final PrintWriter output;

    public ConsoleApplication(CatalogService catalog, InventoryService inventory,
                              Scanner input, PrintWriter output) {
        this.catalog = Objects.requireNonNull(catalog);
        this.inventory = Objects.requireNonNull(inventory);
        this.input = Objects.requireNonNull(input);
        this.output = Objects.requireNonNull(output);
    }

    public void run() {
        output.println("Склад. Данные хранятся в памяти до завершения программы.");
        while (true) {
            printMenu();
            try {
                if (!execute(readText("Команда: "))) {
                    output.println("Работа завершена.");
                    output.flush();
                    return;
                }
            } catch (IllegalArgumentException exception) {
                output.println("Ошибка: " + exception.getMessage());
            } catch (NoSuchElementException exception) {
                output.println("\nВвод завершён. Работа завершена.");
                output.flush();
                return;
            }
        }
    }

    private void printMenu() {
        output.println("""

                1. Зарегистрировать поставщика
                2. Зарегистрировать покупателя
                3. Зарегистрировать товар (без остатка)
                4. Принять товар на склад
                5. Отпустить товар покупателю
                6. Доступные позиции (остаток > 0)
                7. Проверить наличие товара
                8. Сгруппировать товары по видам
                9. Информация о поставщиках
                10. Информация о покупателях
                11. Все позиции и остатки
                0. Выход
                """);
    }

    private boolean execute(String command) {
        switch (command) {
            case "1" -> {
                catalog.registerSupplier(readText("Название поставщика: "), readText("Контакт: "));
                output.println("Поставщик зарегистрирован.");
            }
            case "2" -> {
                catalog.registerCustomer(readText("Имя покупателя: "), readText("Контакт: "));
                output.println("Покупатель зарегистрирован.");
            }
            case "3" -> registerProduct();
            case "4" -> {
                Product product = choose(catalog.products(), ConsoleApplication::describeProduct);
                int quantity = readNumber("Количество: ");
                inventory.receive(product.id(), quantity);
                output.println("Принято: " + quantity + " шт. Остаток: " + inventory.quantityOf(product.id()));
            }
            case "5" -> ship();
            case "6" -> printPositions(inventory.availablePositions());
            case "7" -> {
                Product product = choose(catalog.products(), ConsoleApplication::describeProduct);
                output.println("В наличии: " + inventory.quantityOf(product.id()) + " шт.");
            }
            case "8" -> printGroups();
            case "9" -> printSuppliers();
            case "10" -> printCustomers();
            case "11" -> printPositions(inventory.allPositions());
            case "0" -> {
                return false;
            }
            default -> output.println("Неизвестная команда. Выберите пункт меню.");
        }
        return true;
    }

    private void registerProduct() {
        Supplier supplier = choose(catalog.suppliers(), value -> value.name() + " | " + value.contact());
        Product product = catalog.registerProduct(readText("Название товара: "),
                readText("Вид товара: "), supplier.id());
        output.println("Зарегистрирован товар: " + product.name() + ". Остаток: 0.");
    }

    private void ship() {
        Product product = choose(catalog.products(), ConsoleApplication::describeProduct);
        Customer customer = choose(catalog.customers(), value -> value.name() + " | " + value.contact());
        int quantity = readNumber("Количество: ");
        inventory.ship(product.id(), customer.id(), quantity);
        output.println("Товар отпущен покупателю " + customer.name()
                + ". Остаток: " + inventory.quantityOf(product.id()));
    }

    private void printPositions(List<StockPosition> positions) {
        if (positions.isEmpty()) {
            output.println("Позиций нет.");
            return;
        }
        for (StockPosition position : positions) {
            output.println(describeProduct(position.product()) + " | " + position.quantity() + " шт.");
        }
    }

    private void printGroups() {
        Map<String, List<StockPosition>> groups = inventory.groupByType();
        if (groups.isEmpty()) {
            output.println("Позиций нет.");
        }
        groups.forEach((type, positions) -> {
            output.println("Вид: " + type);
            printPositions(positions);
        });
    }

    private void printSuppliers() {
        List<Supplier> suppliers = catalog.suppliers();
        if (suppliers.isEmpty()) {
            output.println("Поставщиков нет.");
        }
        for (Supplier supplier : suppliers) {
            output.println(supplier.name() + " | " + supplier.contact() + " | id=" + supplier.id());
            catalog.products().stream().filter(product -> product.supplierId().equals(supplier.id()))
                    .forEach(product -> output.println("  Товар: " + describeProduct(product)));
        }
    }

    private void printCustomers() {
        List<Customer> customers = catalog.customers();
        if (customers.isEmpty()) {
            output.println("Покупателей нет.");
        }
        for (Customer customer : customers) {
            output.println(customer.name() + " | " + customer.contact() + " | id=" + customer.id());
            inventory.shipmentsFor(customer.id()).forEach(shipment -> {
                String productName = catalog.products().stream()
                        .filter(product -> product.id().equals(shipment.productId()))
                        .map(Product::name).findFirst().orElse(shipment.productId().toString());
                output.println("  Получено: " + productName + " | " + shipment.quantity() + " шт.");
            });
        }
    }

    private <T> T choose(List<T> items, Function<T, String> label) {
        if (items.isEmpty()) {
            throw new IllegalArgumentException("Список пуст. Сначала зарегистрируйте нужную запись.");
        }
        for (int index = 0; index < items.size(); index++) {
            output.println((index + 1) + ". " + label.apply(items.get(index)));
        }
        int index = readNumber("Номер записи: ");
        if (index < 1 || index > items.size()) {
            throw new IllegalArgumentException("Номер записи вне списка.");
        }
        return items.get(index - 1);
    }

    private int readNumber(String prompt) {
        try {
            return Integer.parseInt(readText(prompt));
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Введите целое число в диапазоне int.");
        }
    }

    private String readText(String prompt) {
        output.print(prompt);
        output.flush();
        return input.nextLine().strip();
    }

    private static String describeProduct(Product product) {
        return product.name() + " | " + product.type() + " | id=" + product.id();
    }
}
