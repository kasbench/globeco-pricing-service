package org.kasbench.globeco_pricing_service.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.kasbench.globeco_pricing_service.dto.PriceDto;
import org.kasbench.globeco_pricing_service.entity.Price;
import org.kasbench.globeco_pricing_service.service.PriceService;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class PriceControllerTest {
    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PriceService priceService;

    @Test
    @DisplayName("GET /api/v1/prices returns list of prices with sampled fields")
    void getAllPrices_returnsSampledPrices() throws Exception {
        Price price = new Price(1, new Date(), "AAPL", new BigDecimal("100.00"), 2.0, 1);
        Mockito.when(priceService.getAllPrices()).thenReturn(List.of(price));
        Mockito.when(priceService.toDto(Mockito.any())).thenCallRealMethod();

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
        Price price = new Price(1, new Date(), "AAPL", new BigDecimal("100.00"), 2.0, 1);
        Mockito.when(priceService.getPriceByTicker("AAPL")).thenReturn(List.of(price));
        Mockito.when(priceService.toDto(Mockito.any())).thenCallRealMethod();

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
        Mockito.when(priceService.getPriceByTicker("MSFT")).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/v1/price/MSFT").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }
} 