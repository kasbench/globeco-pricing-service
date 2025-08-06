package org.kasbench.globeco_pricing_service;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * HTTP metrics filter for recording request metrics.
 * This is a basic stub - full implementation will be added in task 3.
 */
@Component
public class HttpMetricsFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        // Basic pass-through implementation
        // Full metrics recording implementation will be added in task 3
        chain.doFilter(request, response);
    }
}