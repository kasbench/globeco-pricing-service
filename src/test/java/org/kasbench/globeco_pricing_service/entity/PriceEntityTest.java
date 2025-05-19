package org.kasbench.globeco_pricing_service.entity;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

public class PriceEntityTest {
    @Test
    void testGettersSettersAndEqualsHashCode() {
        Date date = new Date();
        Price price1 = new Price(1, date, "AAPL", new BigDecimal("123.45"), 1.23, 1);
        Price price2 = new Price(1, date, "AAPL", new BigDecimal("123.45"), 1.23, 1);

        assertEquals(price1, price2);
        assertEquals(price1.hashCode(), price2.hashCode());

        price2.setTicker("GOOG");
        assertNotEquals(price1, price2);

        price2.setTicker("AAPL");
        price2.setPrice(new BigDecimal("200.00"));
        assertNotEquals(price1, price2);
    }
} 