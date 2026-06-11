CREATE EXTENSION IF NOT EXISTS timescaledb;

CREATE TABLE IF NOT EXISTS market_bars (
    symbol TEXT NOT NULL,
    bucket TIMESTAMPTZ NOT NULL,
    open DOUBLE PRECISION NOT NULL,
    high DOUBLE PRECISION NOT NULL,
    low DOUBLE PRECISION NOT NULL,
    close DOUBLE PRECISION NOT NULL,
    volume DOUBLE PRECISION NOT NULL,
    source TEXT NOT NULL DEFAULT 'tradernet',
    PRIMARY KEY (symbol, bucket)
);

SELECT create_hypertable('market_bars', 'bucket', if_not_exists => TRUE);
CREATE INDEX IF NOT EXISTS idx_market_bars_symbol_bucket_desc ON market_bars (symbol, bucket DESC);
