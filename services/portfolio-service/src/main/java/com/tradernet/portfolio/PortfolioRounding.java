package com.tradernet.portfolio;

import java.math.BigDecimal;
import java.math.RoundingMode;

final class PortfolioRounding {

    private PortfolioRounding() {
    }

    static double roundCurrency(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
