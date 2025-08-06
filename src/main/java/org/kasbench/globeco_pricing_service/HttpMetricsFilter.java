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
import org.springframework.web.servlet.HandlerMapping;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;

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

    // Patterns for sophisticated path normalization to prevent cardinality explosion
    private static final Pattern UUID_PATTERN = Pattern.compile(
            "/[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}(?=/|$)");
    private static final Pattern NUMERIC_ID_PATTERN = Pattern.compile("/\\d{2,}(?=/|$)");
    private static final Pattern SINGLE_DIGIT_ID_PATTERN = Pattern.compile("/\\d(?=/|$)");

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
            // Extract and normalize labels with sophisticated path normalization
            String method = normalizeMethod(request.getMethod());
            String path = normalizePath(request);
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
     * Sophisticated path normalization with multiple regex patterns for UUID and numeric ID replacement.
     * Implements Spring MVC HandlerMapping integration to prefer BEST_MATCHING_PATTERN_ATTRIBUTE.
     * Includes context-aware single digit ID replacement for API endpoints only.
     * Falls back to "/unknown" for normalization failures as documented in implementation guide.
     */
    private String normalizePath(HttpServletRequest request) {
        try {
            // First try to get the best match pattern from Spring MVC handler mapping
            String bestMatchingPattern = extractSpringMvcPattern(request);
            if (bestMatchingPattern != null && !bestMatchingPattern.isEmpty()) {
                return bestMatchingPattern;
            }

            // Fallback to URI normalization with sophisticated path parameter replacement
            String path = request.getRequestURI();
            if (path == null) {
                return "/unknown";
            }

            // Remove context path if present
            String contextPath = request.getContextPath();
            if (contextPath != null && !contextPath.isEmpty() && path.startsWith(contextPath)) {
                path = path.substring(contextPath.length());
            }

            // Apply sophisticated path parameter replacement
            path = normalizePathParameters(path);

            // Ensure path starts with /
            if (!path.startsWith("/")) {
                path = "/" + path;
            }

            return path;

        } catch (Exception e) {
            logger.warn("Failed to normalize path for request {}, using fallback",
                    request.getRequestURI(), e);
            return "/unknown";
        }
    }

    /**
     * Extracts Spring MVC handler mapping pattern attributes for accurate route patterns.
     * Prefers BEST_MATCHING_PATTERN_ATTRIBUTE when available.
     */
    private String extractSpringMvcPattern(HttpServletRequest request) {
        try {
            // Try to get the best matching pattern from Spring MVC
            Object bestMatchingPattern = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
            if (bestMatchingPattern instanceof String) {
                return (String) bestMatchingPattern;
            }

            // Fallback to path within handler mapping
            Object pathWithinHandlerMapping = request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
            if (pathWithinHandlerMapping instanceof String) {
                return (String) pathWithinHandlerMapping;
            }

            return null;
        } catch (Exception e) {
            logger.debug("Failed to extract Spring MVC pattern from request attributes", e);
            return null;
        }
    }

    /**
     * Applies multiple regex patterns for robust ID/UUID replacement to prevent cardinality explosion.
     * Uses context-aware single digit ID replacement for API endpoints only.
     */
    private String normalizePathParameters(String path) {
        if (path == null) {
            return "/unknown";
        }

        // Replace UUIDs first (more specific pattern)
        path = UUID_PATTERN.matcher(path).replaceAll("/{uuid}");

        // Replace multi-digit numeric IDs with {id} placeholder
        path = NUMERIC_ID_PATTERN.matcher(path).replaceAll("/{id}");

        // Context-aware single digit ID replacement for API endpoints only
        if (isApiEndpoint(path)) {
            path = SINGLE_DIGIT_ID_PATTERN.matcher(path).replaceAll("/{id}");
        }

        return path;
    }

    /**
     * Determines if the path represents an API endpoint where single digit ID replacement should occur.
     * Only replaces single digits in API contexts to avoid false positives with version numbers.
     */
    private boolean isApiEndpoint(String path) {
        return path.contains("/api/") || 
               path.contains("/v1/") || 
               path.contains("/v2/") ||
               path.contains("/prices/") ||
               path.contains("/users/") || 
               path.contains("/orders/") ||
               path.contains("/executions/");
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