package com.tradernet.marketai.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarketContextUpdateRequestTest {

    @Test
    void treatsAnExplicitZeroAsAvailableData() {
        MarketContextUpdateRequest request = new MarketContextUpdateRequest();
        request.setFundingRateZScore(0.0);

        MarketContextSnapshot snapshot = request.toSnapshot(MarketContextSnapshot.neutral());

        assertTrue(snapshot.isAvailable());
        assertTrue(snapshot.isFundingRateAvailable());
        assertFalse(snapshot.isEtfFlowAvailable());
    }

    @Test
    void partialUpdatesPreserveExistingFieldAvailability() {
        MarketContextSnapshot current = MarketContextSnapshot.neutral();
        current.setEtfFlowZScore(1.25);
        current.setAvailable(true);

        MarketContextUpdateRequest request = new MarketContextUpdateRequest();
        request.setSentimentZScore(0.0);
        MarketContextSnapshot updated = request.toSnapshot(current);

        assertTrue(updated.isEtfFlowAvailable());
        assertTrue(updated.isSentimentAvailable());
        assertFalse(updated.isFundingRateAvailable());
    }

    @Test
    void rejectsNonFiniteInputs() {
        MarketContextUpdateRequest request = new MarketContextUpdateRequest();
        request.setMvrvZScore(Double.NaN);

        assertTrue(request.hasAnyUpdate());
        assertFalse(request.hasOnlyFiniteValues());
    }
}
