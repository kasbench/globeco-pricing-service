package org.kasbench.globeco_pricing_service.repository;

import org.kasbench.globeco_pricing_service.entity.Price;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PriceRepository extends JpaRepository<Price, Integer> {
    List<Price> findByTicker(String ticker);
} 