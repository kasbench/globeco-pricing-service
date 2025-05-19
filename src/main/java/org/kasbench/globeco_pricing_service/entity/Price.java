package org.kasbench.globeco_pricing_service.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.Date;
import java.util.Objects;

@Entity
@Table(name = "price")
public class Price {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "price_date", nullable = false)
    @Temporal(TemporalType.DATE)
    private Date priceDate;

    @Column(name = "ticker", nullable = false, length = 20)
    private String ticker;

    @Column(name = "price", nullable = false, precision = 18, scale = 8)
    private BigDecimal price;

    @Column(name = "price_std", nullable = false)
    private Double priceStd;

    @Version
    @Column(name = "version", nullable = false)
    private Integer version = 1;

    public Price() {}

    public Price(Integer id, Date priceDate, String ticker, BigDecimal price, Double priceStd, Integer version) {
        this.id = id;
        this.priceDate = priceDate;
        this.ticker = ticker;
        this.price = price;
        this.priceStd = priceStd;
        this.version = version;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Date getPriceDate() { return priceDate; }
    public void setPriceDate(Date priceDate) { this.priceDate = priceDate; }
    public String getTicker() { return ticker; }
    public void setTicker(String ticker) { this.ticker = ticker; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public Double getPriceStd() { return priceStd; }
    public void setPriceStd(Double priceStd) { this.priceStd = priceStd; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Price price1 = (Price) o;
        return Objects.equals(id, price1.id) &&
                Objects.equals(priceDate, price1.priceDate) &&
                Objects.equals(ticker, price1.ticker) &&
                Objects.equals(price, price1.price) &&
                Objects.equals(priceStd, price1.priceStd) &&
                Objects.equals(version, price1.version);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, priceDate, ticker, price, priceStd, version);
    }
} 