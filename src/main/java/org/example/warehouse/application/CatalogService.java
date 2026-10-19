package org.example.warehouse.application;

import org.example.warehouse.application.port.CustomerRepository;
import org.example.warehouse.application.port.ProductRepository;
import org.example.warehouse.application.port.SupplierRepository;
import org.example.warehouse.domain.Customer;
import org.example.warehouse.domain.Product;
import org.example.warehouse.domain.Supplier;

import java.util.List;
import java.util.Objects;
import java.util.UUID;


public final class CatalogService {
    private final ProductRepository products;
    private final CustomerRepository customers;
    private final SupplierRepository suppliers;

    public CatalogService(ProductRepository products, CustomerRepository customers,
                          SupplierRepository suppliers) {
        this.products = Objects.requireNonNull(products);
        this.customers = Objects.requireNonNull(customers);
        this.suppliers = Objects.requireNonNull(suppliers);
    }

    public Product registerProduct(String name, String type, UUID supplierId) {
        suppliers.findById(supplierId)
                .orElseThrow(() -> new IllegalArgumentException("Поставщик не найден."));
        Product product = new Product(UUID.randomUUID(), name, type, supplierId);
        products.add(product);
        return product;
    }

    public Customer registerCustomer(String name, String contact) {
        Customer customer = new Customer(UUID.randomUUID(), name, contact);
        customers.add(customer);
        return customer;
    }

    public Supplier registerSupplier(String name, String contact) {
        Supplier supplier = new Supplier(UUID.randomUUID(), name, contact);
        suppliers.add(supplier);
        return supplier;
    }

    public List<Product> products() {
        return products.findAll();
    }

    public List<Customer> customers() {
        return customers.findAll();
    }

    public List<Supplier> suppliers() {
        return suppliers.findAll();
    }
}
