package org.kasbench.globeco_pricing_service.controller;

import org.kasbench.globeco_pricing_service.dto.PriceDto;
import org.kasbench.globeco_pricing_service.entity.Price;
import org.kasbench.globeco_pricing_service.service.PriceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1")
public class PriceController {
    private static final Logger logger = LoggerFactory.getLogger(PriceController.class);
    private static final ThreadLocal<Random> RANDOM = ThreadLocal.withInitial(Random::new);
    
    private final PriceService priceService;

    @Autowired
    public PriceController(PriceService priceService) {
        this.priceService = priceService;
    }

    @GetMapping("/prices")
    public List<PriceDto> getAllPrices() {
        return priceService.getAllPrices().stream()
                .map(this::toSampledDto)
                .collect(Collectors.toList());
    }

    @GetMapping("/price/{ticker}")
    public PriceDto getPriceByTicker(@PathVariable String ticker) {
        List<Price> prices = priceService.getPriceByTicker(ticker);
        if (prices == null || prices.isEmpty()) {
            throw new ResourceNotFoundException("Price not found for ticker: " + ticker);
        }
        return toSampledDto(prices.get(0));
    }

    private PriceDto toSampledDto(Price price) {
        try {
            // Sample from normal distribution with mean=price, std=price_std, round to nearest penny
            double mean = price.getPrice().doubleValue();
            double std = price.getPriceStd();
            
            // Use ThreadLocal Random with nextGaussian() for efficient sampling
            // Transform standard normal (mean=0, std=1) to desired distribution
            double sampled = mean + (RANDOM.get().nextGaussian() * std);
            
            BigDecimal sampledPrice = BigDecimal.valueOf(Math.max(0.01, Math.round(sampled * 100.0) / 100.0)).setScale(2, RoundingMode.HALF_UP);
            PriceDto dto = priceService.toDto(price);
            dto.setOpen(sampledPrice);
            dto.setClose(sampledPrice);
            dto.setHigh(sampledPrice);
            dto.setLow(sampledPrice);
            return dto;
        } catch (Exception e) {
            logger.error("Failed to generate sampled price for ticker {}", price.getTicker(), e);
            // Fallback: return mean price without sampling
            PriceDto dto = priceService.toDto(price);
            BigDecimal meanPrice = price.getPrice();
            dto.setOpen(meanPrice);
            dto.setClose(meanPrice);
            dto.setHigh(meanPrice);
            dto.setLow(meanPrice);
            return dto;
        }
    }

    @ResponseStatus(code = org.springframework.http.HttpStatus.NOT_FOUND)
    public static class ResourceNotFoundException extends RuntimeException {
        public ResourceNotFoundException(String message) { super(message); }
    }
} 