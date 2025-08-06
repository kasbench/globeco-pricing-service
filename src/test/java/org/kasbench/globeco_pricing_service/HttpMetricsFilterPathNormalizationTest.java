package org.kasbench.globeco_pricing_service;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.HandlerMapping;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Test class for HttpMetricsFilter path normalization functionality.
 * Tests sophisticated path normalization with multiple regex patterns for UUID and numeric ID replacement.
 */
class HttpMetricsFilterPathNormalizationTest {

    private SimpleMeterRegistry meterRegistry;
    private AtomicInteger inFlightRequestsCounter;
    private HttpMetricsFilter httpMetricsFilter;
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        inFlightRequestsCounter = new AtomicInteger(0);
        httpMetricsFilter = new HttpMetricsFilter(meterRegistry, inFlightRequestsCounter);
        filterChain = mock(FilterChain.class);
    }

    @Test
    void testPathNormalization_UUIDReplacement() throws IOException, ServletException {
        // Test UUID pattern replacement
        MockHttpServletRequest request = new MockHttpServletRequest("GET", 
            "/api/v1/prices/550e8400-e29b-41d4-a716-446655440000");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(200);

        httpMetricsFilter.doFilter(request, response, filterChain);

        // Verify metrics were recorded with normalized path
        assertTrue(meterRegistry.find("http_requests_total")
            .tag("path", "/api/v1/prices/{uuid}")
            .counter() != null);
    }

    @Test
    void testPathNormalization_NumericIDReplacement() throws IOException, ServletException {
        // Test multi-digit numeric ID replacement
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/prices/12345");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(200);

        httpMetricsFilter.doFilter(request, response, filterChain);

        // Verify metrics were recorded with normalized path
        assertTrue(meterRegistry.find("http_requests_total")
            .tag("path", "/api/v1/prices/{id}")
            .counter() != null);
    }

    @Test
    void testPathNormalization_SingleDigitIDInAPIContext() throws IOException, ServletException {
        // Test single digit ID replacement in API context
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/prices/5");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(200);

        httpMetricsFilter.doFilter(request, response, filterChain);

        // Verify metrics were recorded with normalized path (single digit should be replaced in API context)
        assertTrue(meterRegistry.find("http_requests_total")
            .tag("path", "/api/v1/prices/{id}")
            .counter() != null);
    }

    @Test
    void testPathNormalization_SingleDigitNotInAPIContext() throws IOException, ServletException {
        // Test single digit NOT replaced when not in API context
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/static/css/v1/style.css");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(200);

        httpMetricsFilter.doFilter(request, response, filterChain);

        // Verify metrics were recorded with original path (single digit should NOT be replaced)
        assertTrue(meterRegistry.find("http_requests_total")
            .tag("path", "/static/css/v1/style.css")
            .counter() != null);
    }

    @Test
    void testPathNormalization_SpringMVCHandlerMapping() throws IOException, ServletException {
        // Test Spring MVC HandlerMapping integration
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/prices/123");
        request.setAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, "/api/v1/prices/{ticker}");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(200);

        httpMetricsFilter.doFilter(request, response, filterChain);

        // Verify Spring MVC pattern is preferred over regex normalization
        assertTrue(meterRegistry.find("http_requests_total")
            .tag("path", "/api/v1/prices/{ticker}")
            .counter() != null);
    }

    @Test
    void testPathNormalization_ComplexPath() throws IOException, ServletException {
        // Test complex path with multiple IDs and UUIDs
        MockHttpServletRequest request = new MockHttpServletRequest("GET", 
            "/api/v1/users/123/orders/550e8400-e29b-41d4-a716-446655440000/items/456");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(200);

        httpMetricsFilter.doFilter(request, response, filterChain);

        // Verify all IDs and UUIDs are normalized
        assertTrue(meterRegistry.find("http_requests_total")
            .tag("path", "/api/v1/users/{id}/orders/{uuid}/items/{id}")
            .counter() != null);
    }

    @Test
    void testPathNormalization_NullPath() throws IOException, ServletException {
        // Test fallback to "/unknown" for null path
        MockHttpServletRequest request = new MockHttpServletRequest("GET", null);
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(200);

        httpMetricsFilter.doFilter(request, response, filterChain);

        // Verify fallback path is used
        assertTrue(meterRegistry.find("http_requests_total")
            .tag("path", "/unknown")
            .counter() != null);
    }

    @Test
    void testPathNormalization_WithContextPath() throws IOException, ServletException {
        // Test context path removal
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/pricing-service/api/v1/prices/123");
        request.setContextPath("/pricing-service");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(200);

        httpMetricsFilter.doFilter(request, response, filterChain);

        // Verify context path is removed and ID is normalized
        assertTrue(meterRegistry.find("http_requests_total")
            .tag("path", "/api/v1/prices/{id}")
            .counter() != null);
    }
}