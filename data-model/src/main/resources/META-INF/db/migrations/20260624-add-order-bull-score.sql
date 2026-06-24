-- Adds the forecast bull score captured when an order is created.
ALTER TABLE tblOrders ADD COLUMN IF NOT EXISTS bullScore DOUBLE PRECISION;
