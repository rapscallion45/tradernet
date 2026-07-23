package com.tradernet.portfolio;

import com.tradernet.portfolio.dto.PortfolioSummaryDto;
import jakarta.ejb.Local;

/**
 * User-scoped portfolio query contract.
 */
@Local
public interface PortfolioQueryService {

    PortfolioSummaryDto getPortfolio(long userId, String currency);
}
