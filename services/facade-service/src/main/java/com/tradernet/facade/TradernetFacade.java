package com.tradernet.facade;

import com.tradernet.jpa.dao.OrderDao;
import com.tradernet.jpa.entities.OrderEntity;
import com.tradernet.order.OrderBookService;
import com.tradernet.signal.SignalService;

import java.util.List;

/**
 * Facade for the Tradernet application.
 * <p>
 * Encapsulates the business logic for order placement, validation,
 * and signal processing. Provides a simplified API for clients.
 */
public class TradernetFacade {

    private final SignalService signalService;
    private final OrderBookService orderBookService;
    private final OrderDao orderDao;

    /**
     * Constructs the TradernetFacade with its dependencies.
     *
     * @param signalService    service that validates trading signals
     * @param orderBookService service that manages the order book
     * @param orderDao         DAO for persisting orders
     */
    public TradernetFacade(SignalService signalService,
                           OrderBookService orderBookService,
                           OrderDao orderDao) {
        this.signalService = signalService;
        this.orderBookService = orderBookService;
        this.orderDao = orderDao;
    }

    /**
     * Executes a trade if the order is valid.
     *
     * @param order the order to execute
     * @return true if executed successfully, false otherwise
     */
    public boolean executeOrder(OrderEntity order) {
        // Validate the trading signal
        if (!signalService.isValidOrder(order)) {
            return false;
        }

        // Execute order in the order book
        boolean executed = orderBookService.addOrder(order);
        if (executed) {
            // Persist order in database
            orderDao.save(order);
        }

        return executed;
    }

    /**
     * Returns all orders stored in the system.
     *
     * @return list of all orders
     */
    public List<OrderEntity> getAllOrders() {
        return orderDao.findAll();
    }

    /**
     * Returns all orders for a specific trading symbol.
     *
     * @param symbol the trading symbol
     * @return list of orders
     */
    public List<OrderEntity> getOrdersBySymbol(String symbol) {
        return orderDao.findBySymbol(symbol);
    }

    /**
     * Clears all orders from both the order book and the database.
     */
    public void clearAllOrders() {
        orderBookService.clearOrders();
        orderDao.deleteAll();
    }
}
