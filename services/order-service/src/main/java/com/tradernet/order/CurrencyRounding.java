package com.tradernet.order;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class CurrencyRounding {

    private CurrencyRounding() {
    }

    public static double roundCurrency(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
