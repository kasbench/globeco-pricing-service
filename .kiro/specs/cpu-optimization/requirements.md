# Requirements Document

## Introduction

This document outlines the requirements for optimizing the GlobeCo Pricing Service to reduce CPU consumption from 1000m (1 core) to 100-150m, achieving an 85-90% reduction. The service currently consumes 10x more CPU than expected for a simple database retrieval service with less than 500 records. The optimization will address 7 identified inefficiencies without requiring architectural changes.

## Glossary

- **Pricing Service**: The GlobeCo microservice that retrieves and returns pricing data for securities
- **NormalDistribution**: Apache Commons Math class used to generate random samples from a normal distribution
- **HttpMetricsFilter**: Custom servlet filter that collects HTTP request metrics for observability
- **JPA**: Java Persistence API used for database access
- **HikariCP**: Connection pool library used by Spring Boot
- **OTLP**: OpenTelemetry Protocol used for metrics export
- **DTO**: Data Transfer Object returned to API clients

## Requirements

### Requirement 1: Optimize Random Sampling Performance

**User Story:** As a system operator, I want the pricing service to efficiently generate random price samples, so that CPU consumption is minimized during request processing

#### Acceptance Criteria

1. WHEN the Pricing Service generates a random sample for a price, THE Pricing Service SHALL use a cached random number generator instead of creating new NormalDistribution objects
2. THE Pricing Service SHALL implement a thread-safe caching mechanism for random sampling that eliminates per-request object instantiation
3. THE Pricing Service SHALL reduce CPU consumption from random sampling operations by at least 60%
4. WHERE a simpler random generation method provides equivalent statistical properties, THE Pricing Service SHALL use the simpler method over complex mathematical operations

### Requirement 2: Reduce Metrics Collection Overhead

**User Story:** As a system operator, I want HTTP metrics collection to have minimal performance impact, so that observability does not degrade service performance

#### Acceptance Criteria

1. WHEN the HttpMetricsFilter normalizes request paths, THE HttpMetricsFilter SHALL use simple string operations instead of regular expression pattern matching
2. THE HttpMetricsFilter SHALL reduce the number of string operations performed per request by at least 50%
3. THE HttpMetricsFilter SHALL eliminate unnecessary logging operations in the request processing path
4. THE HttpMetricsFilter SHALL reduce CPU consumption from metrics collection by at least 15%

### Requirement 3: Optimize Logging Configuration

**User Story:** As a system operator, I want production logging to be configured appropriately, so that unnecessary log processing does not consume CPU resources

#### Acceptance Criteria

1. THE Pricing Service SHALL set Micrometer OTLP registry logging level to INFO or higher in production
2. THE Pricing Service SHALL set OpenTelemetry OTLP exporter logging level to INFO or higher in production
3. THE Pricing Service SHALL reduce CPU consumption from logging operations by at least 5%
4. WHERE DEBUG logging is needed for troubleshooting, THE Pricing Service SHALL provide a mechanism to enable it temporarily without redeployment

### Requirement 4: Configure JPA and Hibernate Optimally

**User Story:** As a system operator, I want JPA and Hibernate to be configured for optimal performance, so that database operations consume minimal resources

#### Acceptance Criteria

1. THE Pricing Service SHALL configure JPA batch size to optimize bulk operations
2. THE Pricing Service SHALL configure Hibernate to order inserts and updates for better performance
3. THE Pricing Service SHALL configure JDBC fetch size appropriate for the workload
4. THE Pricing Service SHALL disable SQL logging in production environments

### Requirement 5: Configure Database Connection Pool

**User Story:** As a system operator, I want the database connection pool sized appropriately for the workload, so that resources are not wasted on unnecessary connections while supporting horizontal scaling

#### Acceptance Criteria

1. THE Pricing Service SHALL configure HikariCP maximum pool size appropriate for a service with less than 500 database records that may scale to up to 20 replicas
2. THE Pricing Service SHALL configure HikariCP minimum idle connections to balance startup time and resource usage per replica
3. THE Pricing Service SHALL configure connection timeout values appropriate for the expected query response times
4. THE Pricing Service SHALL configure idle timeout to release unused connections
5. WHEN the Pricing Service scales to 20 replicas, THE Pricing Service SHALL ensure total database connections across all replicas remain within acceptable limits for the database server

### Requirement 6: Implement Efficient Data Retrieval

**User Story:** As an API consumer, I want to retrieve pricing data efficiently, so that I can paginate through results without loading all records

#### Acceptance Criteria

1. THE Pricing Service SHALL support pagination parameters for the getAllPrices endpoint
2. WHEN a client requests prices without pagination parameters, THE Pricing Service SHALL return a default page size that balances usability and performance
3. THE Pricing Service SHALL process price records efficiently to minimize CPU consumption per record
4. WHERE parallel processing improves performance without increasing resource consumption, THE Pricing Service SHALL use parallel streams

### Requirement 7: Remove Unnecessary Production Code

**User Story:** As a system operator, I want development and debugging code removed from production, so that unnecessary operations do not consume resources

#### Acceptance Criteria

1. THE Pricing Service SHALL not create test metrics counters in production environments
2. WHERE debugging or development configurations exist, THE Pricing Service SHALL activate them only in non-production profiles
3. THE Pricing Service SHALL minimize startup operations that do not contribute to production functionality

### Requirement 8: Validate Performance Improvements

**User Story:** As a system operator, I want to verify that optimizations achieve the expected performance improvements, so that I can confidently reduce resource allocations

#### Acceptance Criteria

1. WHEN all optimizations are implemented, THE Pricing Service SHALL consume no more than 150m CPU under normal load
2. THE Pricing Service SHALL demonstrate at least 85% reduction in CPU consumption compared to the baseline
3. THE Pricing Service SHALL maintain or improve response times after optimizations
4. THE Pricing Service SHALL maintain functional correctness of all API endpoints after optimizations
