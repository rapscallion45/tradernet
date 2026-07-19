package com.tradernet.marketai;

import com.tradernet.marketai.model.MarketBar;
import jakarta.annotation.Resource;
import jakarta.ejb.Asynchronous;
import jakarta.ejb.Stateless;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;

/**
 * Persists closed market bars for downstream forecasting.
 */
@Stateless
public class MarketBarPersistence {

    private static final String INSERT_MARKET_BAR_SQL = "INSERT INTO market_bars "
            + "(symbol, bucket, open, high, low, close, volume, source) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

    @Resource(lookup = "java:/jdbc/TradernetDS")
    private DataSource dataSource;

    @Asynchronous
    public void storeAsync(MarketBar bar) {
        store(bar);
    }

    public void store(MarketBar bar) {
        if (dataSource == null || bar == null) {
            return;
        }

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(INSERT_MARKET_BAR_SQL)) {
            statement.setString(1, MarketSymbolNormalizer.normalizeSymbol(bar.getSymbol()));
            statement.setTimestamp(2, new Timestamp(bar.getBucketStart()));
            statement.setDouble(3, bar.getOpen());
            statement.setDouble(4, bar.getHigh());
            statement.setDouble(5, bar.getLow());
            statement.setDouble(6, bar.getClose());
            statement.setDouble(7, bar.getVolume());
            statement.setString(8, "binance-trade-stream");
            statement.executeUpdate();
        } catch (SQLException ex) {
            // Forecasting should degrade gracefully if persistence is unavailable or a duplicate bar arrives.
        }
    }
}
