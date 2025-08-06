package org.kasbench.globeco_pricing_service;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * HTTP metrics filter for recording request metrics.
 * Records http_requests_total counter, http_request_duration histogram, and manages in-flight requests.
 * Uses Timer caching for performance optimization and millisecond-based durations for OTLP compatibility.
 */
@Component
public class HttpMetricsFilter implements Filter {

    private static final Logger logger = LoggerFactory.getLogger(HttpMetricsFilter.class);

    private static final String HTTP_REQUESTS_TOTAL = "http_requests_total";
    private static final String HTTP_REQUEST_DURATION = "http_request_duration";

    // Histogram buckets optimized for millisecond OTLP export as specified in task requirements
    private static final Duration[] HISTOGRAM_BUCKETS = {
            Duration.ofMillis(5), Duration.ofMillis(10), Duration.ofMillis(25),
            Duration.ofMillis(50), Duration.ofMillis(100), Duration.ofMillis(250),
            Duration.ofMillis(500), Duration.ofMillis(1000), Duration.ofMillis(2500),
            Duration.ofMillis(5000), Duration.ofMillis(10000)
    };

    // CRITICAL: Cache Timer instances to avoid re-registration overhead using composite key strategy
    private final ConcurrentHashMap<String, Timer> timerCache = new ConcurrentHashMap<>();

    private final MeterRegistry meterRegistry;
    private final AtomicInteger inFlightCounter;

    public HttpMetricsFilter(MeterRegistry meterRegistry, AtomicInteger inFlightRequestsCounter) {
        this.meterRegistry = meterRegistry;
        this.inFlightCounter = inFlightRequestsCounter;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        if (!(request instanceof HttpServletRequest) || !(response instanceof HttpServletResponse)) {
            chain.doFilter(request, response);
            return;
        }

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // Start timing and increment in-flight counter
        long startTime = System.nanoTime();
        boolean inFlightIncremented = false;

        try {
            inFlightCounter.incrementAndGet();
            inFlightIncremented = true;
            chain.doFilter(request, response);
        } catch (Exception e) {
            // Record metrics even when exceptions occur
            recordMetrics(httpRequest, httpResponse, startTime);
            throw e;
        } finally {
            try {
                recordMetrics(httpRequest, httpResponse, startTime);
                if (inFlightIncremented) {
                    inFlightCounter.decrementAndGet();
                }
            } catch (Exception e) {
                logger.error("Failed to record HTTP metrics", e);
            }
        }
    }

    /**
     * Records HTTP metrics for the completed request.
     * Uses cached Timer instances and records duration in milliseconds for OTLP compatibility.
     */
    private void recordMetrics(HttpServletRequest request, HttpServletResponse response, long startTime) {
        try {
            // Extract and normalize labels (basic implementation for task 3)
            String method = normalizeMethod(request.getMethod());
            String path = getBasicPath(request);
            String status = normalizeStatusCode(response.getStatus());

            // Calculate duration in milliseconds (KEY INSIGHT: works better than nanoseconds)
            long durationNanos = System.nanoTime() - startTime;
            long durationMillis = durationNanos / 1_000_000L;

            // Record counter metric
            meterRegistry.counter(HTTP_REQUESTS_TOTAL,
                    "method", method,
                    "path", path,
                    "status", status)
                    .increment();

            // CRITICAL: Use cached Timer instances with composite key strategy to avoid re-registration overhead
            String timerKey = HTTP_REQUEST_DURATION + ":" + method + ":" + path + ":" + status;
            Timer timer = timerCache.computeIfAbsent(timerKey, key -> Timer.builder(HTTP_REQUEST_DURATION)
                    .description("Duration of HTTP requests")
                    .serviceLevelObjectives(HISTOGRAM_BUCKETS)
                    .publishPercentileHistogram(false)
                    .tag("method", method)
                    .tag("path", path)
                    .tag("status", status)
                    .register(meterRegistry));

            // Record duration in milliseconds
            timer.record(durationMillis, TimeUnit.MILLISECONDS);

        } catch (Exception e) {
            logger.error("Failed to record HTTP request metrics for {} {}",
                    request.getMethod(), request.getRequestURI(), e);
        }
    }

    /**
     * Basic path extraction for task 3. More sophisticated normalization will be added in task 4.
     */
    private String getBasicPath(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (path == null || path.isEmpty()) {
            return "/unknown";
        }
        return path;
    }

    /**
     * Normalizes HTTP method to uppercase.
     */
    private String normalizeMethod(String method) {
        if (method == null || method.trim().isEmpty()) {
            return "UNKNOWN";
        }
        return method.trim().toUpperCase();
    }

    /**
     * Normalizes HTTP status code to string.
     */
    private String normalizeStatusCode(int statusCode) {
        return String.valueOf(statusCode);
    }
}