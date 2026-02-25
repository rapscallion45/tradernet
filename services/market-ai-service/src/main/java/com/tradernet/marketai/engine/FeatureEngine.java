package com.tradernet.marketai.engine;

import com.tradernet.marketai.model.FeatureSnapshot;
import com.tradernet.marketai.model.MarketBar;

/**
 * Incremental EMA/RSI feature calculator.
 */
public class FeatureEngine {

    private static final int RSI_PERIOD = 14;
    private static final double ALPHA_FAST = 2.0 / (9.0 + 1.0);
    private static final double ALPHA_SLOW = 2.0 / (21.0 + 1.0);

    private Double emaFast;
    private Double emaSlow;
    private Double avgGain;
    private Double avgLoss;
    private Double lastClose;

    public synchronized FeatureSnapshot onClosedBar(MarketBar bar) {
        if (lastClose == null) {
            lastClose = bar.getClose();
            emaFast = bar.getClose();
            emaSlow = bar.getClose();
            avgGain = 0.0;
            avgLoss = 0.0;
            return new FeatureSnapshot(bar.getSymbol(), bar.getBucketStart(), bar.getClose(), emaFast, emaSlow, 50.0);
        }

        final double change = bar.getClose() - lastClose;
        final double gain = Math.max(change, 0.0);
        final double loss = Math.max(-change, 0.0);

        emaFast = ALPHA_FAST * bar.getClose() + (1 - ALPHA_FAST) * emaFast;
        emaSlow = ALPHA_SLOW * bar.getClose() + (1 - ALPHA_SLOW) * emaSlow;
        avgGain = ((avgGain * (RSI_PERIOD - 1)) + gain) / RSI_PERIOD;
        avgLoss = ((avgLoss * (RSI_PERIOD - 1)) + loss) / RSI_PERIOD;
        lastClose = bar.getClose();

        final double rs = avgLoss == 0.0 ? 100.0 : avgGain / avgLoss;
        final double rsi = avgLoss == 0.0 ? 100.0 : 100.0 - (100.0 / (1.0 + rs));

        return new FeatureSnapshot(bar.getSymbol(), bar.getBucketStart(), bar.getClose(), emaFast, emaSlow, rsi);
    }
}
