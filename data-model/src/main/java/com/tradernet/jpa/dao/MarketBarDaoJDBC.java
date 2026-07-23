package com.tradernet.jpa.dao;

import com.tradernet.jpa.common.exception.DataAccessException;
import jakarta.annotation.Resource;
import jakarta.ejb.Stateless;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;

/**
 * JDBC implementation used for high-frequency market-bar inserts.
 */
@Stateless
public class MarketBarDaoJDBC implements MarketBarDao {

    private static final Logger LOG = LoggerFactory.getLogger(MarketBarDaoJDBC.class);
    private static final String DUPLICATE_KEY_SQL_STATE = "23505";
    private static final String INSERT_SQL = "INSERT INTO market_bars "
        + "(symbol, bucket, open, high, low, close, volume, source) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

    @Resource(lookup = "java:/jdbc/TradernetDS")
    private DataSource dataSource;

    @Override
    public boolean insert(
        String symbol,
        Instant bucket,
        double open,
        double high,
        double low,
        double close,
        double volume,
        String source
    ) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(INSERT_SQL)) {
            statement.setString(1, symbol);
            statement.setTimestamp(2, Timestamp.from(bucket));
            statement.setDouble(3, open);
            statement.setDouble(4, high);
            statement.setDouble(5, low);
            statement.setDouble(6, close);
            statement.setDouble(7, volume);
            statement.setString(8, source);
            return statement.executeUpdate() == 1;
        } catch (SQLException ex) {
            if (DUPLICATE_KEY_SQL_STATE.equals(ex.getSQLState())) {
                LOG.debug("Ignored duplicate market bar for {} at {}", symbol, bucket);
                return false;
            }
            throw new DataAccessException("Could not persist market bar for " + symbol + " at " + bucket, ex);
        }
    }
}
