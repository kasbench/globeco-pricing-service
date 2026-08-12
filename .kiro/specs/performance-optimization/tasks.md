# Implementation Plan: Performance Optimization

## Overview

This implementation plan covers three independent performance optimizations for the Globeco Pricing Service: replacing the Java-based database migration with a static SQL migration using batch INSERT statements, bounding the Caffeine cache size to 500 entries, and removing the unused `commons-math3` dependency. Each change is self-contained and targets a specific file.

## Tasks

- [x] 1. Replace Java migration with static SQL migration
  - [x] 1.1 Generate `V2__LoadPricingData.sql` from `selected_prices.csv`
    - Read `src/main/resources/static/selected_prices.csv` (471 rows, no header, format: `date,ticker,price,price_std`)
    - Create `src/main/resources/db/migration/V2__LoadPricingData.sql` with batch INSERT statements
    - Use multi-row INSERT syntax: `INSERT INTO price (price_date, ticker, price, price_std) VALUES (...), (...), ...;`
    - Group rows into batches of approximately 50 rows per INSERT statement
    - Ensure `price` values use 8 decimal places (e.g., `55.85000000`) to match `decimal(18,8)` column type
    - Ensure `price_std` values are inserted as-is (float precision)
    - Do not specify `id` (serial auto-increment) or `version` (defaults to 1)
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5_

  - [x] 1.2 Delete the Java-based migration file
    - Delete `src/main/java/org/kasbench/globeco_pricing_service/db/migration/V2__LoadPricingData.java`
    - Verify no other source files import or reference this class
    - _Requirements: 1.6_

  - [x]* 1.3 Write integration test verifying migration correctness
    - **Property 1: Migration data fidelity**
    - **Validates: Requirements 1.3**
    - Create a test that starts the application with Testcontainers PostgreSQL
    - Verify the `price` table contains exactly 471 rows where `price_date = '2017-05-18'`
    - For a sample of rows, verify `ticker`, `price`, and `price_std` match the values in `selected_prices.csv`

- [x] 2. Checkpoint - Verify migration changes
  - Ensure all tests pass, ask the user if questions arise.

- [x] 3. Bound Caffeine cache size
  - [x] 3.1 Add `maximumSize(500)` to `CacheConfig.java`
    - Modify `src/main/java/org/kasbench/globeco_pricing_service/CacheConfig.java`
    - Add `.maximumSize(500)` to the `Caffeine.newBuilder()` chain in the `caffeineConfig()` bean method
    - Retain the existing `.expireAfterWrite(5, TimeUnit.MINUTES)` policy
    - _Requirements: 2.1, 2.2, 2.3, 2.4_

  - [x]* 3.2 Write unit test verifying cache size bound
    - **Property 2: Cache size bound invariant**
    - **Validates: Requirements 2.3**
    - Create a test that inserts more than 500 distinct entries into the cache
    - Assert the cache size never exceeds 500 entries after eviction processing

- [x] 4. Remove unused `commons-math3` dependency
  - [x] 4.1 Remove the dependency from `build.gradle`
    - Remove the line `implementation group: 'org.apache.commons', name: 'commons-math3', version: '3.6.1'` from the `dependencies` block in `build.gradle`
    - _Requirements: 3.1, 3.2, 3.3_

- [x] 5. Final checkpoint - Verify all changes
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Checkpoints ensure incremental validation
- Property tests validate universal correctness properties from the design document
- The three optimizations are independent and can be implemented in parallel (see dependency graph below)
- The programming language is Java 21 with Spring Boot 3.4.5, matching the existing codebase

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1", "3.1", "4.1"] },
    { "id": 1, "tasks": ["1.2", "3.2"] },
    { "id": 2, "tasks": ["1.3"] }
  ]
}
```
