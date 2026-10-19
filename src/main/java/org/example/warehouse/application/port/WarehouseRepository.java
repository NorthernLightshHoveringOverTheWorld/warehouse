package org.example.warehouse.application.port;

import org.example.warehouse.domain.Warehouse;


public interface WarehouseRepository {
    Warehouse load();
    void save(Warehouse warehouse);
}
