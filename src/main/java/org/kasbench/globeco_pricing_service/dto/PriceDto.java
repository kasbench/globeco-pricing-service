package org.kasbench.globeco_pricing_service.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class PriceDto {
    private Long id;
    private String ticker;
    private LocalDate date;
    private BigDecimal open;
    private BigDecimal close;
    private BigDecimal high;
    private BigDecimal low;
    private Long volume;

    public PriceDto() {}

    public PriceDto(Long id, String ticker, LocalDate date, BigDecimal open, BigDecimal close, BigDecimal high, BigDecimal low, Long volume) {
        this.id = id;
        this.ticker = ticker;
        this.date = date;
        this.open = open;
        this.close = close;
        this.high = high;
        this.low = low;
        this.volume = volume;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTicker() { return ticker; }
    public void setTicker(String ticker) { this.ticker = ticker; }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public BigDecimal getOpen() { return open; }
    public void setOpen(BigDecimal open) { this.open = open; }

    public BigDecimal getClose() { return close; }
    public void setClose(BigDecimal close) { this.close = close; }

    public BigDecimal getHigh() { return high; }
    public void setHigh(BigDecimal high) { this.high = high; }

    public BigDecimal getLow() { return low; }
    public void setLow(BigDecimal low) { this.low = low; }

    public Long getVolume() { return volume; }
    public void setVolume(Long volume) { this.volume = volume; }

    @Override
    public String toString() {
        return "PriceDto{" +
                "id=" + id +
                ", ticker='" + ticker + '\'' +
                ", date=" + date +
                ", open=" + open +
                ", close=" + close +
                ", high=" + high +
                ", low=" + low +
                ", volume=" + volume +
                '}';
    }
} 