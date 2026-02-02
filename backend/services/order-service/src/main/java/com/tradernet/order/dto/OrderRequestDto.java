package com.tradernet.order.dto;

import com.tradernet.model.Order;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.io.Serializable;
import java.util.Objects;

/**
 * Request payload for creating an order.
 */
public class OrderRequestDto implements Serializable {

    @NotBlank
    private String symbol;

    @NotNull
    private Order.Side side;

    @Positive
    private Double quantity;

    @Positive
    private Double price;

    public OrderRequestDto() {
    }

    public OrderRequestDto(String symbol, Order.Side side, Double quantity, Double price) {
        this.symbol = symbol;
        this.side = side;
        this.quantity = quantity;
        this.price = price;
    }

    public String getSymbol() {
        return symbol;
    }

    public void setSymbol(String symbol) {
        this.symbol = symbol;
    }

    public Order.Side getSide() {
        return side;
    }

    public void setSide(Order.Side side) {
        this.side = side;
    }

    public Double getQuantity() {
        return quantity;
    }

    public void setQuantity(Double quantity) {
        this.quantity = quantity;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        OrderRequestDto that = (OrderRequestDto) o;
        return Objects.equals(symbol, that.symbol)
            && side == that.side
            && Objects.equals(quantity, that.quantity)
            && Objects.equals(price, that.price);
    }

    @Override
    public int hashCode() {
        return Objects.hash(symbol, side, quantity, price);
    }

    @Override
    public String toString() {
        return "OrderRequestDto{" +
            "symbol='" + symbol + '\'' +
            ", side=" + side +
            ", quantity=" + quantity +
            ", price=" + price +
            '}';
    }
}
