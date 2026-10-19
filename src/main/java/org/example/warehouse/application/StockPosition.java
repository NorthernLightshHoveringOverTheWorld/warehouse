package org.example.warehouse.application;

import org.example.warehouse.domain.Product;


public record StockPosition(Product product, int quantity) {
}
