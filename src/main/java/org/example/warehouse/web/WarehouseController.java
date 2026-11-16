package org.example.warehouse.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.example.warehouse.application.*;
import org.example.warehouse.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class WarehouseController {
    private final CatalogService catalog;
    private final InventoryService inventory;

    public WarehouseController(CatalogService catalog, InventoryService inventory) {
        this.catalog = catalog;
        this.inventory = inventory;
    }

    public record ContactRequest(@NotBlank @Size(max=200) String name,
                                 @NotBlank @Size(max=300) String contact) { }
    public record ProductRequest(@NotBlank @Size(max=200) String name,
                                 @NotBlank @Size(max=100) String type, @NotNull UUID supplierId) { }
    public record ReceiptRequest(@NotNull UUID productId, @NotNull @Positive Integer quantity) { }
    public record ShipmentRequest(@NotNull UUID productId, @NotNull UUID customerId,
                                  @NotNull @Positive Integer quantity) { }

    @GetMapping("/health") public Map<String, String> health() {
        inventory.allPositions();
        return Map.of("status", "ok");
    }
    @GetMapping("/products") public List<Product> products() { return catalog.products(); }
    @GetMapping("/customers") public List<Customer> customers() { return catalog.customers(); }
    @GetMapping("/suppliers") public List<Supplier> suppliers() { return catalog.suppliers(); }
    @PostMapping("/products") @ResponseStatus(HttpStatus.CREATED)
    public Product product(@Valid @RequestBody ProductRequest request) {
        return catalog.registerProduct(request.name(), request.type(), request.supplierId());
    }
    @PostMapping("/customers") @ResponseStatus(HttpStatus.CREATED)
    public Customer customer(@Valid @RequestBody ContactRequest request) {
        return catalog.registerCustomer(request.name(), request.contact());
    }
    @PostMapping("/suppliers") @ResponseStatus(HttpStatus.CREATED)
    public Supplier supplier(@Valid @RequestBody ContactRequest request) {
        return catalog.registerSupplier(request.name(), request.contact());
    }
    @GetMapping("/inventory") public List<StockPosition> inventory(@RequestParam(defaultValue="false") boolean available) {
        return available ? inventory.availablePositions() : inventory.allPositions();
    }
    @GetMapping("/inventory/groups") public Map<String, List<StockPosition>> groups() { return inventory.groupByType(); }
    @GetMapping("/inventory/{id}") public Map<String, Integer> quantity(@PathVariable UUID id) {
        return Map.of("quantity", inventory.quantityOf(id));
    }
    @PostMapping("/receipts") public Map<String, Integer> receive(@Valid @RequestBody ReceiptRequest request) {
        inventory.receive(request.productId(), request.quantity());
        return Map.of("quantity", inventory.quantityOf(request.productId()));
    }
    @PostMapping("/shipments") public Map<String, Integer> ship(@Valid @RequestBody ShipmentRequest request) {
        inventory.ship(request.productId(), request.customerId(), request.quantity());
        return Map.of("quantity", inventory.quantityOf(request.productId()));
    }
    @GetMapping("/shipments") public List<Shipment> shipments() { return inventory.shipments(); }
    @GetMapping("/customers/{id}/shipments") public List<Shipment> history(@PathVariable UUID id) {
        return inventory.shipmentsFor(id);
    }
}
