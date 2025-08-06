# Requirements Document

## Introduction

This feature implements standardized HTTP request metrics for the GlobeCo Pricing Service microservice to provide consistent observability across all microservices. The implementation will follow the enhanced-metrics.md specification and incorporate lessons learned from the java-microservice-http-metrics-implementation-guide.md to ensure robust, production-ready metrics collection that exports to both Prometheus (direct scraping) and OpenTelemetry Collector.

## Requirements

### Requirement 1

**User Story:** As a DevOps engineer, I want standardized HTTP metrics exported from the pricing service, so that I can monitor request patterns, performance, and errors consistently across all microservices.

#### Acceptance Criteria

1. WHEN the pricing service receives any HTTP request THEN the system SHALL increment the `http_requests_total` counter with labels for method, path, and status
2. WHEN the pricing service processes any HTTP request THEN the system SHALL record the request duration in the `http_request_duration` histogram with the same labels
3. WHEN the pricing service starts processing an HTTP request THEN the system SHALL increment the `http_requests_in_flight` gauge
4. WHEN the pricing service completes processing an HTTP request THEN the system SHALL decrement the `http_requests_in_flight` gauge
5. WHEN metrics are exported THEN the system SHALL use millisecond-based durations for consistency with OpenTelemetry Collector expectations

### Requirement 2

**User Story:** As a monitoring system, I want to scrape metrics from the pricing service via standard endpoints, so that I can collect observability data through both Prometheus and OpenTelemetry pipelines.

#### Acceptance Criteria

1. WHEN a monitoring system requests `/actuator/prometheus` THEN the system SHALL return all HTTP metrics in Prometheus text format
2. WHEN the OpenTelemetry Collector requests metrics THEN the system SHALL export metrics via OTLP protocol to the configured collector endpoint
3. WHEN metrics are exported THEN the system SHALL include proper resource attributes for service identification (name, version, namespace)
4. WHEN the service starts THEN the system SHALL register all metrics with appropriate descriptions and labels

### Requirement 3

**User Story:** As a system administrator, I want HTTP metrics to capture all request types including errors and edge cases, so that I have complete visibility into service behavior.

#### Acceptance Criteria

1. WHEN the pricing service receives requests to any endpoint (API, health checks, static files) THEN the system SHALL record metrics for all requests
2. WHEN an HTTP request results in an exception or error THEN the system SHALL still record complete metrics before propagating the exception
3. WHEN an HTTP request has path parameters or IDs THEN the system SHALL normalize the path to prevent metric cardinality explosion (e.g., `/api/prices/123` becomes `/api/prices/{id}`)
4. WHEN multiple concurrent requests are processed THEN the system SHALL accurately track in-flight requests without race conditions

### Requirement 4

**User Story:** As a developer, I want the metrics implementation to follow proven patterns and avoid common pitfalls, so that the system performs well under load and integrates seamlessly with existing infrastructure.

#### Acceptance Criteria

1. WHEN Timer instances are needed for duration metrics THEN the system SHALL cache Timer instances using ConcurrentHashMap to prevent re-registration overhead
2. WHEN path normalization is performed THEN the system SHALL use multiple regex patterns for robust ID/UUID replacement as documented in the implementation guide
3. WHEN Spring MVC handler mapping is available THEN the system SHALL leverage HandlerMapping attributes for accurate route patterns
4. WHEN recording metrics THEN the system SHALL handle exceptions gracefully and track in-flight increment state to prevent double-decrement scenarios
5. WHEN configuring histogram buckets THEN the system SHALL use Duration.ofMillis() values: [5, 10, 25, 50, 100, 250, 500, 1000, 2500, 5000, 10000] milliseconds

### Requirement 5

**User Story:** As a quality assurance engineer, I want comprehensive tests for the metrics implementation, so that I can verify correct behavior under various scenarios including error conditions.

#### Acceptance Criteria

1. WHEN tests are executed THEN the system SHALL verify that all three metric types (counter, histogram, gauge) are created and registered correctly
2. WHEN test requests are processed THEN the system SHALL verify counter increments, histogram duration recording, and gauge in-flight tracking
3. WHEN test exceptions occur during request processing THEN the system SHALL verify metrics are still recorded and in-flight counter is properly decremented
4. WHEN test requests have various path patterns THEN the system SHALL verify path normalization works correctly for IDs, UUIDs, and complex routes
5. WHEN concurrent test requests are processed THEN the system SHALL verify thread safety and accurate in-flight request tracking