package com.tradernet.dao;

import com.tradernet.entities.TradeEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

/**
 * JDBC implementation of TradeDao using Spring JDBC.
 */
@Repository
public class TradeDaoJDBC implements TradeDao {

    private final JdbcTemplate jdbcTemplate;

    private final RowMapper<TradeEntity> rowMapper = (rs, rowNum) -> {
        TradeEntity trade = new TradeEntity();
        trade.setId(rs.getLong("id"));
        trade.setSymbol(rs.getString("symbol"));
        trade.setQuantity(rs.getDouble("quantity"));
        trade.setPrice(rs.getDouble("price"));
        Timestamp timestamp = rs.getTimestamp("timestamp");
        if (timestamp != null) {
            trade.setTimestamp(timestamp.toLocalDateTime());
        }
        return trade;
    };

    public TradeDaoJDBC(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void save(TradeEntity trade) {
        jdbcTemplate.update(
            "INSERT INTO tblTrades (symbol, quantity, price, timestamp) VALUES (?, ?, ?, ?)",
            trade.getSymbol(),
            trade.getQuantity(),
            trade.getPrice(),
            toTimestamp(trade.getTimestamp())
        );
    }

    @Override
    public List<TradeEntity> findAll() {
        return jdbcTemplate.query("SELECT * FROM tblTrades", rowMapper);
    }

    @Override
    public List<TradeEntity> findBySymbol(String symbol) {
        return jdbcTemplate.query(
            "SELECT * FROM tblTrades WHERE symbol = ?",
            rowMapper,
            symbol
        );
    }

    @Override
    public void deleteAll() {
        jdbcTemplate.update("DELETE FROM tblTrades");
    }

    private static Timestamp toTimestamp(LocalDateTime localDateTime) {
        if (localDateTime == null) {
            return null;
        }
        return Timestamp.valueOf(localDateTime);
    }
}
