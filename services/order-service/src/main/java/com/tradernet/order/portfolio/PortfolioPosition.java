package com.tradernet.order.portfolio;

/**
 * Net position state produced by replaying signed position events.
 */
public final class PortfolioPosition {

    private static final double POSITION_EPSILON = 1e-9;

    private double netQuantity;
    private double netCost;
    private double lastKnownPrice;

    void apply(double quantityDelta, double tradePrice) {
        if (Math.abs(quantityDelta) < POSITION_EPSILON) {
            return;
        }

        if (isFlat() || hasSameSign(netQuantity, quantityDelta)) {
            netQuantity += quantityDelta;
            netCost += quantityDelta * tradePrice;
            clearIfFlat();
            return;
        }

        final double newQuantity = netQuantity + quantityDelta;
        final double existingAverageCost = getAverageCost();
        if (Math.abs(newQuantity) < POSITION_EPSILON) {
            netQuantity = 0.0;
            netCost = 0.0;
            return;
        }

        if (hasSameSign(netQuantity, newQuantity)) {
            netQuantity = newQuantity;
            netCost = Math.copySign(Math.abs(newQuantity) * existingAverageCost, newQuantity);
            return;
        }

        netQuantity = newQuantity;
        netCost = newQuantity * tradePrice;
    }

    void setLastKnownPrice(double lastKnownPrice) {
        this.lastKnownPrice = lastKnownPrice;
    }

    public double getNetQuantity() {
        return netQuantity;
    }

    public double getLastKnownPrice() {
        return lastKnownPrice;
    }

    public boolean isFlat() {
        return Math.abs(netQuantity) < POSITION_EPSILON;
    }

    public double getAverageCost() {
        return isFlat() ? 0.0 : Math.abs(netCost / netQuantity);
    }

    private void clearIfFlat() {
        if (isFlat()) {
            netQuantity = 0.0;
            netCost = 0.0;
        }
    }

    private boolean hasSameSign(double left, double right) {
        return (left > 0.0 && right > 0.0) || (left < 0.0 && right < 0.0);
    }
}
