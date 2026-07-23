package com.tradernet.order;

import jakarta.ejb.Local;

import java.util.List;

/**
 * Cross-module contract exposing user-scoped order facts for portfolio calculation.
 */
@Local
public interface OrderPortfolioQueryService {

    List<OrderPortfolioItem> getPortfolioItems(long userId);
}
