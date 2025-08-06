package org.kasbench.globeco_pricing_service;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.servlet.FilterRegistrationBean;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class HttpMetricsConfigurationTest {

    @Test
    void testInFlightRequestsCounterBean() {
        HttpMetricsConfiguration config = new HttpMetricsConfiguration();
        AtomicInteger counter = config.inFlightRequestsCounter();
        
        assertNotNull(counter);
        assertEquals(0, counter.get());
    }

    @Test
    void testInFlightRequestsGaugeRegistration() {
        HttpMetricsConfiguration config = new HttpMetricsConfiguration();
        MeterRegistry meterRegistry = new SimpleMeterRegistry();
        AtomicInteger counter = new AtomicInteger(0);
        
        Gauge gauge = config.inFlightRequestsGauge(meterRegistry, counter);
        
        assertNotNull(gauge);
        assertEquals("http_requests_in_flight", gauge.getId().getName());
        assertEquals("Number of HTTP requests currently being processed", gauge.getId().getDescription());
        assertEquals(0.0, gauge.value());
        
        // Test that gauge reflects counter changes
        counter.set(5);
        assertEquals(5.0, gauge.value());
    }

    @Test
    void testHttpMetricsFilterRegistration() {
        HttpMetricsConfiguration config = new HttpMetricsConfiguration();
        HttpMetricsFilter filter = new HttpMetricsFilter();
        
        FilterRegistrationBean<HttpMetricsFilter> registration = 
            config.httpMetricsFilterRegistration(filter);
        
        assertNotNull(registration);
        assertEquals(filter, registration.getFilter());
        assertEquals(1, registration.getOrder());
        assertTrue(registration.getUrlPatterns().contains("/*"));
    }
}