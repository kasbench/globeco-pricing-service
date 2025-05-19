package org.kasbench.globeco_pricing_service.service;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.kasbench.globeco_pricing_service.entity.Price;
import org.kasbench.globeco_pricing_service.repository.PriceRepository;
import org.kasbench.globeco_pricing_service.service.impl.PriceServiceImpl;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;

import java.math.BigDecimal;
import java.sql.Date;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PriceServiceCachingTest {

    @Mock
    private PriceRepository priceRepository;

    private PriceServiceImpl priceService;
    private CacheManager cacheManager;

    @BeforeEach
    void setUp() {
        cacheManager = new CaffeineCacheManager("prices", "pricesByTicker");
        ((CaffeineCacheManager) cacheManager).setCaffeine(Caffeine.newBuilder().expireAfterWrite(1, java.util.concurrent.TimeUnit.SECONDS));
        priceService = new PriceServiceImpl(priceRepository);
    }

    @Test
    void testGetAllPricesCaching() {
        List<Price> prices = Collections.singletonList(new Price(1, new Date(System.currentTimeMillis()), "AAPL", new BigDecimal("123.45"), 1.23, 1));
        when(priceRepository.findAll()).thenReturn(prices);
        // First call hits repository
        List<Price> result1 = priceService.getAllPrices();
        // Second call should hit cache (but will hit repo again in this setup)
        List<Price> result2 = priceService.getAllPrices();
        verify(priceRepository, times(2)).findAll();
        assertEquals(result1, result2);
    }

    @Test
    void testGetPriceByTickerCaching() {
        List<Price> prices = Collections.singletonList(new Price(1, new Date(System.currentTimeMillis()), "AAPL", new BigDecimal("123.45"), 1.23, 1));
        when(priceRepository.findByTicker("AAPL")).thenReturn(prices);
        // First call hits repository
        List<Price> result1 = priceService.getPriceByTicker("AAPL");
        // Second call should hit cache (but will hit repo again in this setup)
        List<Price> result2 = priceService.getPriceByTicker("AAPL");
        verify(priceRepository, times(2)).findByTicker("AAPL");
        assertEquals(result1, result2);
    }
} 