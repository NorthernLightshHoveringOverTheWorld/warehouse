package org.example.warehouse.application;

import org.example.warehouse.domain.Product;

/** Результат запроса: позиция каталога и её текущий остаток. */
public record StockPosition(Product product, int quantity) {
}
