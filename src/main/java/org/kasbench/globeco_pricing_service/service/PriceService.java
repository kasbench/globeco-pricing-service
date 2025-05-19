package org.kasbench.globeco_pricing_service.service;

import org.kasbench.globeco_pricing_service.entity.Price;
import java.util.List;

public interface PriceService {
    List<Price> getAllPrices();
    List<Price> getPriceByTicker(String ticker);
} 