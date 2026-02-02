package com.tradernet.dao;

import com.tradernet.model.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * JDBC implementation of OrderDao using Spring JDBC.
 * <p>
 * Provides basic CRUD operations for the 'orders' table.
 */
@Repository
public class OrderDaoJDBC implements OrderDao {

    /**
     * Spring JdbcTemplate used for executing SQL queries.
     */
    private final JdbcTemplate jdbcTemplate;

    /**
     * RowMapper to convert SQL ResultSet rows into Order objects.
     * Maps the "side" string to the Side enum.
     */
    private final RowMapper<Order> rowMapper = (rs, rowNum) -> {
        String sideString = rs.getString("side");
        Order.Side side = Order.Side.valueOf(sideString.toUpperCase()); // assumes DB stores "BUY" or "SELL"

        return new Order(
            rs.getString("symbol"),
            rs.getInt("quantity"),
            rs.getDouble("price"),
            side
        );
    };

    /**
     * Constructs a JdbcOrderDao with the given JdbcTemplate.
     *
     * @param jdbcTemplate JDBC template for database access
     */
    public OrderDaoJDBC(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Saves a new order into the database.
     *
     * @param order the order to save
     */
    @Override
    public void save(Order order) {
        jdbcTemplate.update(
            "INSERT INTO orders (symbol, quantity, price, side) VALUES (?, ?, ?, ?)",
            order.getSymbol(),
            order.getQuantity(),
            order.getPrice(),
            order.getSide()
        );
    }

    /**
     * Returns all orders from the database.
     *
     * @return list of all orders
     */
    @Override
    public List<Order> findAll() {
        return jdbcTemplate.query("SELECT * FROM orders", rowMapper);
    }

    /**
     * Returns all orders for a specific symbol.
     *
     * @param symbol the trading symbol
     * @return list of orders for the symbol
     */
    @Override
    public List<Order> findBySymbol(String symbol) {
        return jdbcTemplate.query(
            "SELECT * FROM orders WHERE symbol = ?",
            rowMapper,
            symbol
        );
    }

    /**
     * Deletes all orders from the database.
     */
    @Override
    public void deleteAll() {
        jdbcTemplate.update("DELETE FROM orders");
    }
}
