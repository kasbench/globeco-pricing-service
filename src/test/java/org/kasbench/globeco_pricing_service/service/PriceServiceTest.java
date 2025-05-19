package org.kasbench.globeco_pricing_service.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kasbench.globeco_pricing_service.entity.Price;
import org.kasbench.globeco_pricing_service.repository.PriceRepository;
import org.kasbench.globeco_pricing_service.service.impl.PriceServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@Import(org.kasbench.globeco_pricing_service.TestcontainersConfiguration.class)
public class PriceServiceTest {
    @Autowired
    private PriceRepository priceRepository;
    @Autowired
    private PriceServiceImpl priceService;

    @Test
    void testGetAllPricesAndGetPriceByTicker() {
        Price price = new Price(null, new Date(), "AAPL", new BigDecimal("123.45"), 1.23, 1);
        priceRepository.save(price);
        assertFalse(priceService.getAllPrices().isEmpty());
        var found = priceService.getPriceByTicker("AAPL");
        assertFalse(found.isEmpty());
        assertEquals("AAPL", found.get(0).getTicker());
    }
} 