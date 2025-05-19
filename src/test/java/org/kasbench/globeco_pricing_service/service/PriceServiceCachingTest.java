package org.kasbench.globeco_pricing_service.service;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kasbench.globeco_pricing_service.entity.Price;
import org.kasbench.globeco_pricing_service.repository.PriceRepository;
import org.kasbench.globeco_pricing_service.service.impl.PriceServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = PriceServiceCachingTest.TestConfig.class)
@EnableCaching
public class PriceServiceCachingTest {
    @TestConfiguration
    static class TestConfig {
        @Bean
        public PriceServiceImpl priceService(PriceRepository priceRepository) {
            return new PriceServiceImpl(priceRepository);
        }
        @Bean
        public CacheManager cacheManager() {
            CaffeineCacheManager cm = new CaffeineCacheManager("prices", "pricesByTicker");
            cm.setCaffeine(Caffeine.newBuilder().expireAfterWrite(1, java.util.concurrent.TimeUnit.SECONDS));
            return cm;
        }
    }

    @MockBean
    private PriceRepository priceRepository;
    @Autowired
    private PriceService priceService;

    @Test
    void testGetAllPricesCaching() {
        List<Price> prices = Collections.singletonList(new Price(1, new Date(), "AAPL", new BigDecimal("123.45"), 1.23, 1));
        when(priceRepository.findAll()).thenReturn(prices);
        // First call hits repository
        List<Price> result1 = priceService.getAllPrices();
        // Second call should hit cache
        List<Price> result2 = priceService.getAllPrices();
        verify(priceRepository, times(1)).findAll();
        assertEquals(result1, result2);
    }

    @Test
    void testGetPriceByTickerCaching() {
        List<Price> prices = Collections.singletonList(new Price(1, new Date(), "AAPL", new BigDecimal("123.45"), 1.23, 1));
        when(priceRepository.findByTicker("AAPL")).thenReturn(prices);
        // First call hits repository
        List<Price> result1 = priceService.getPriceByTicker("AAPL");
        // Second call should hit cache
        List<Price> result2 = priceService.getPriceByTicker("AAPL");
        verify(priceRepository, times(1)).findByTicker("AAPL");
        assertEquals(result1, result2);
    }
} 