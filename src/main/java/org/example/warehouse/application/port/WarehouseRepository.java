package org.example.warehouse.application.port;

import org.example.warehouse.domain.Warehouse;

/** Хранит один склад. save целиком заменяет состояние; load возвращает неизменяемый снимок. */
public interface WarehouseRepository {
    Warehouse load();
    void save(Warehouse warehouse);
}
