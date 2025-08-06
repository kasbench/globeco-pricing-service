package org.kasbench.globeco_pricing_service;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Configuration class for HTTP metrics infrastructure.
 * Sets up the in-flight request counter, gauge registration, and filter configuration.
 */
@Configuration
public class HttpMetricsConfiguration {

    /**
     * Creates an AtomicInteger bean for tracking in-flight HTTP requests.
     * This counter is thread-safe and used by both the filter and the gauge.
     *
     * @return AtomicInteger initialized to 0 for tracking in-flight requests
     */
    @Bean
    public AtomicInteger inFlightRequestsCounter() {
        return new AtomicInteger(0);
    }

    /**
     * Registers the http_requests_in_flight gauge with the MeterRegistry.
     * This gauge tracks the current number of HTTP requests being processed.
     *
     * @param meterRegistry the Spring Boot auto-configured MeterRegistry
     * @param inFlightCounter the AtomicInteger bean for tracking in-flight requests
     * @return the registered Gauge instance
     */
    @Bean
    public Gauge inFlightRequestsGauge(MeterRegistry meterRegistry, AtomicInteger inFlightCounter) {
        return Gauge.builder("http_requests_in_flight", inFlightCounter, AtomicInteger::get)
                .description("Number of HTTP requests currently being processed")
                .register(meterRegistry);
    }

    /**
     * Configures the HttpMetricsFilter with high priority and broad URL pattern.
     * The filter is registered with order=1 to ensure it captures all requests
     * before other filters and applies to all URL patterns.
     *
     * @param httpMetricsFilter the HttpMetricsFilter to register
     * @return configured FilterRegistrationBean
     */
    @Bean
    public FilterRegistrationBean<HttpMetricsFilter> httpMetricsFilterRegistration(HttpMetricsFilter httpMetricsFilter) {
        FilterRegistrationBean<HttpMetricsFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(httpMetricsFilter);
        registration.addUrlPatterns("/*");
        registration.setOrder(1); // High priority to capture all requests
        registration.setName("httpMetricsFilter");
        return registration;
    }
}