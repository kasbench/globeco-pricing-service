package org.kasbench.globeco_pricing_service.repository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.kasbench.globeco_pricing_service.entity.Price;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.math.BigDecimal;
import java.util.Date;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@DataJpaTest
@Import(org.kasbench.globeco_pricing_service.TestcontainersConfiguration.class)
public class PriceRepositoryTest {
    @Autowired
    private PriceRepository priceRepository;

    @Test
    void testSaveAndFindByTicker() {
        Price price = new Price(null, new Date(), "AAPL", new BigDecimal("123.45"), 1.23, 1);
        price = priceRepository.save(price);
        var found = priceRepository.findByTicker("AAPL");
        assertFalse(found.isEmpty());
        assertEquals("AAPL", found.get(0).getTicker());
    }
} 