package com.tradernet.order.dto;

import com.tradernet.model.Order;

import java.io.Serializable;
import java.util.Objects;

/**
 * Response payload for order data.
 */
public class OrderResponseDto implements Serializable {
    private String symbol;
    private String side;
    private Double quantity;
    private Double price;

    public OrderResponseDto() {
    }

    public OrderResponseDto(String symbol, String side, Double quantity, Double price) {
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

    public String getSide() {
        return side;
    }

    public void setSide(String side) {
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

    public static OrderResponseDto fromOrder(Order order) {
        return new OrderResponseDto(
            order.getSymbol(),
            order.getSide().name(),
            order.getQuantity(),
            order.getPrice()
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        OrderResponseDto that = (OrderResponseDto) o;
        return Objects.equals(symbol, that.symbol)
            && Objects.equals(side, that.side)
            && Objects.equals(quantity, that.quantity)
            && Objects.equals(price, that.price);
    }

    @Override
    public int hashCode() {
        return Objects.hash(symbol, side, quantity, price);
    }

    @Override
    public String toString() {
        return "OrderResponseDto{" +
            "symbol='" + symbol + '\'' +
            ", side='" + side + '\'' +
            ", quantity=" + quantity +
            ", price=" + price +
            '}';
    }
}
