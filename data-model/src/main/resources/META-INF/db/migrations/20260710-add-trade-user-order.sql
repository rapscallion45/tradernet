ALTER TABLE tblTrades ADD COLUMN IF NOT EXISTS userId BIGINT;
ALTER TABLE tblTrades ADD COLUMN IF NOT EXISTS orderId BIGINT;
ALTER TABLE tblTrades ADD COLUMN IF NOT EXISTS side VARCHAR(32);
ALTER TABLE tblTrades ADD COLUMN IF NOT EXISTS executionType VARCHAR(32);

CREATE INDEX IF NOT EXISTS idx_tblTrades_user_timestamp ON tblTrades (userId, timestamp DESC);
CREATE INDEX IF NOT EXISTS idx_tblTrades_order_execution ON tblTrades (orderId, executionType);
