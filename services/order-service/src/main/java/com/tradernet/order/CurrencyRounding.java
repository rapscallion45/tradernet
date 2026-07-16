package com.tradernet.order;

import java.math.BigDecimal;
import java.math.RoundingMode;

final class CurrencyRounding {

    private CurrencyRounding() {
    }

    static double roundCurrency(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
