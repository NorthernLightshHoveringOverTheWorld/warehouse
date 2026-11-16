CREATE TABLE suppliers (
    id UUID PRIMARY KEY,
    name TEXT NOT NULL CHECK (length(trim(name)) > 0),
    contact TEXT NOT NULL CHECK (length(trim(contact)) > 0),
    registration_order BIGINT GENERATED ALWAYS AS IDENTITY UNIQUE
);
CREATE TABLE customers (
    id UUID PRIMARY KEY,
    name TEXT NOT NULL CHECK (length(trim(name)) > 0),
    contact TEXT NOT NULL CHECK (length(trim(contact)) > 0),
    registration_order BIGINT GENERATED ALWAYS AS IDENTITY UNIQUE
);
CREATE TABLE products (
    id UUID PRIMARY KEY,
    name TEXT NOT NULL CHECK (length(trim(name)) > 0),
    type TEXT NOT NULL CHECK (length(trim(type)) > 0),
    supplier_id UUID NOT NULL REFERENCES suppliers(id),
    registration_order BIGINT GENERATED ALWAYS AS IDENTITY UNIQUE
);
CREATE INDEX products_supplier_idx ON products(supplier_id);
CREATE TABLE warehouses (
    id INTEGER PRIMARY KEY CHECK (id = 1),
    name TEXT NOT NULL CHECK (length(trim(name)) > 0)
);
INSERT INTO warehouses(id, name) VALUES (1, 'Основной склад');
CREATE TABLE stock (
    warehouse_id INTEGER NOT NULL REFERENCES warehouses(id),
    product_id UUID NOT NULL REFERENCES products(id),
    quantity INTEGER NOT NULL CHECK (quantity >= 0),
    PRIMARY KEY (warehouse_id, product_id)
);
CREATE TABLE shipments (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    warehouse_id INTEGER NOT NULL REFERENCES warehouses(id),
    product_id UUID NOT NULL REFERENCES products(id),
    customer_id UUID NOT NULL REFERENCES customers(id),
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX shipments_customer_idx ON shipments(customer_id);
CREATE INDEX shipments_warehouse_idx ON shipments(warehouse_id, id);
