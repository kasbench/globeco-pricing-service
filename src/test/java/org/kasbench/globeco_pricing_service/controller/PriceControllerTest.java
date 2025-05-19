package org.kasbench.globeco_pricing_service.controller;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.kasbench.globeco_pricing_service.entity.Price;
import org.kasbench.globeco_pricing_service.service.PriceService;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.junit.jupiter.api.extension.ExtendWith;

import java.math.BigDecimal;
import java.sql.Date;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ExtendWith(MockitoExtension.class)
class PriceControllerTest {
    @Autowired
    private MockMvc mockMvc;

    private static PriceService priceService;

    @BeforeAll
    static void setUpMock() {
        priceService = org.mockito.Mockito.mock(PriceService.class);
    }

    @TestConfiguration
    static class MockConfig {
        @Bean
        @Primary
        PriceService priceService() {
            return PriceControllerTest.priceService;
        }
    }

    @Test
    @DisplayName("GET /api/v1/prices returns list of prices with sampled fields")
    void getAllPrices_returnsSampledPrices() throws Exception {
        Price price = new Price(1, new Date(System.currentTimeMillis()), "AAPL", new BigDecimal("100.00"), 2.0, 1);
        Mockito.when(PriceControllerTest.priceService.getAllPrices()).thenReturn(List.of(price));
        Mockito.when(PriceControllerTest.priceService.toDto(Mockito.any())).thenCallRealMethod();

        mockMvc.perform(get("/api/v1/prices").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].ticker", is("AAPL")))
                .andExpect(jsonPath("$[0].open", notNullValue()))
                .andExpect(jsonPath("$[0].close", notNullValue()))
                .andExpect(jsonPath("$[0].high", notNullValue()))
                .andExpect(jsonPath("$[0].low", notNullValue()));
    }

    @Test
    @DisplayName("GET /api/v1/price/{ticker} returns sampled price for ticker")
    void getPriceByTicker_returnsSampledPrice() throws Exception {
        Price price = new Price(1, new Date(System.currentTimeMillis()), "AAPL", new BigDecimal("100.00"), 2.0, 1);
        Mockito.when(PriceControllerTest.priceService.getPriceByTicker("AAPL")).thenReturn(List.of(price));
        Mockito.when(PriceControllerTest.priceService.toDto(Mockito.any())).thenCallRealMethod();

        mockMvc.perform(get("/api/v1/price/AAPL").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ticker", is("AAPL")))
                .andExpect(jsonPath("$.open", notNullValue()))
                .andExpect(jsonPath("$.close", notNullValue()))
                .andExpect(jsonPath("$.high", notNullValue()))
                .andExpect(jsonPath("$.low", notNullValue()));
    }

    @Test
    @DisplayName("GET /api/v1/price/{ticker} returns 404 if not found")
    void getPriceByTicker_returns404IfNotFound() throws Exception {
        Mockito.when(PriceControllerTest.priceService.getPriceByTicker("MSFT")).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/v1/price/MSFT").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }
} 