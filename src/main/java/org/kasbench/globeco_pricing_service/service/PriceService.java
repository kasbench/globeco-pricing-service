package org.kasbench.globeco_pricing_service.service;

import org.kasbench.globeco_pricing_service.dto.PriceDto;
import org.kasbench.globeco_pricing_service.entity.Price;
import java.util.List;
import java.time.LocalDate;
import java.time.ZoneId;

public interface PriceService {
    List<Price> getAllPrices();
    List<Price> getPriceByTicker(String ticker);

    // Mapping from entity to DTO
    default PriceDto toDto(Price price) {
        if (price == null) return null;
        LocalDate localDate = price.getPriceDate() == null ? null : price.getPriceDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        return new PriceDto(
            price.getId() == null ? null : price.getId().longValue(),
            price.getTicker(),
            localDate,
            price.getPrice(), // open
            price.getPrice(), // close
            price.getPrice(), // high
            price.getPrice(), // low
            price.getPriceStd() == null ? null : price.getPriceStd().longValue() // volume
        );
    }

    // Mapping from DTO to entity
    default Price toEntity(PriceDto dto) {
        if (dto == null) return null;
        Price price = new Price();
        price.setId(dto.getId() == null ? null : dto.getId().intValue());
        price.setTicker(dto.getTicker());
        price.setPriceDate(dto.getDate() == null ? null : java.util.Date.from(dto.getDate().atStartOfDay(ZoneId.systemDefault()).toInstant()));
        price.setPrice(dto.getOpen());
        price.setPriceStd(dto.getVolume() == null ? null : dto.getVolume().doubleValue());
        return price;
    }
} 