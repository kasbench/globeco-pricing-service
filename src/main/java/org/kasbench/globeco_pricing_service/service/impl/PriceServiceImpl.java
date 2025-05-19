package org.kasbench.globeco_pricing_service.service.impl;

import org.kasbench.globeco_pricing_service.entity.Price;
import org.kasbench.globeco_pricing_service.repository.PriceRepository;
import org.kasbench.globeco_pricing_service.service.PriceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.cache.annotation.Cacheable;

import java.util.List;
import java.util.Optional;

@Service
public class PriceServiceImpl implements PriceService {
    private final PriceRepository priceRepository;

    @Autowired
    public PriceServiceImpl(PriceRepository priceRepository) {
        this.priceRepository = priceRepository;
    }

    @Override
    @Cacheable(value = "prices", unless = "#result == null || #result.isEmpty()")
    public List<Price> getAllPrices() {
        return priceRepository.findAll();
    }

    @Override
    @Cacheable(value = "pricesByTicker", key = "#ticker", unless = "#result == null || #result.isEmpty()")
    public List<Price> getPriceByTicker(String ticker) {
        return priceRepository.findByTicker(ticker);
    }
} 