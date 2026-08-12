# Requirements Document

## Introduction

This document specifies the requirements for optimizing the CPU and memory footprint of the Globeco Pricing Service. The optimization targets three areas: replacing the inefficient Java-based database migration with a static SQL migration using batch INSERT statements, bounding the Caffeine cache size to prevent unbounded memory growth, and removing an unused dependency to reduce artifact size.

## Glossary

- **Pricing_Service**: The Globeco Pricing Service Spring Boot application responsible for serving stock pricing data.
- **Data_Migration**: The Flyway database migration (version V2) that loads initial pricing data into the `price` table at application startup.
- **Cache_Configuration**: The Caffeine cache setup in `CacheConfig.java` that manages in-memory caching for the `prices` and `pricesByTicker` caches.
- **Build_System**: The Gradle build configuration (`build.gradle`) that defines project dependencies and build tasks.
- **Price_Table**: The PostgreSQL table `public.price` with columns: `id` (serial PK), `price_date` (date), `ticker` (varchar(20)), `price` (decimal(18,8)), `price_std` (float), `version` (integer DEFAULT 1).
- **Selected_Prices_Dataset**: The static CSV file (`selected_prices.csv`) containing 471 pricing rows for the date 2017-05-18 in the format `date,ticker,price,price_std` with no header row.
- **Batch_INSERT**: A SQL INSERT statement that inserts multiple rows in a single statement using a VALUES list.

## Requirements

### Requirement 1: Replace Java Migration with Static SQL Migration

**User Story:** As a DevOps engineer, I want the database migration to use a static SQL file with batch INSERT statements, so that the migration executes faster and consumes less CPU and memory than the current approach of decompressing and filtering a large CSV file at runtime.

#### Acceptance Criteria

1. WHEN the Pricing_Service starts, THE Data_Migration SHALL load pricing data from a SQL migration file named `V2__LoadPricingData.sql` located in `src/main/resources/db/migration/`.
2. THE Data_Migration SHALL insert all 471 rows from the Selected_Prices_Dataset into the Price_Table using batch INSERT statements.
3. THE Data_Migration SHALL insert each row with the values: `price_date` set to `2017-05-18`, `ticker` from the dataset, `price` from the dataset, and `price_std` from the dataset.
4. THE Data_Migration SHALL use standard SQL INSERT statements compatible with PostgreSQL and Testcontainers environments.
5. WHEN the Data_Migration completes, THE Price_Table SHALL contain exactly 471 rows with `price_date` equal to `2017-05-18`.
6. THE Build_System SHALL exclude the Java-based migration file `V2__LoadPricingData.java` from the compiled application (the file shall be deleted from `src/main/java/org/kasbench/globeco_pricing_service/db/migration/`).

### Requirement 2: Bound Cache Size

**User Story:** As a system administrator, I want the in-memory cache to have a maximum size limit, so that the application memory usage remains predictable and does not grow unboundedly under varying load.

#### Acceptance Criteria

1. THE Cache_Configuration SHALL set a maximum size of 500 entries for the `prices` cache.
2. THE Cache_Configuration SHALL set a maximum size of 500 entries for the `pricesByTicker` cache.
3. WHILE the cache contains 500 entries, THE Cache_Configuration SHALL evict existing entries before admitting new entries.
4. THE Cache_Configuration SHALL retain the existing time-based expiration policy of 5 minutes after write.

### Requirement 3: Remove Unused Dependency

**User Story:** As a developer, I want unused dependencies removed from the build configuration, so that the application artifact is smaller and the attack surface is reduced.

#### Acceptance Criteria

1. THE Build_System SHALL exclude the `org.apache.commons:commons-math3` library from the project dependencies.
2. WHEN the project is built, THE Build_System SHALL produce a deployable artifact that does not contain the `commons-math3` library.
3. WHEN the project is built after the dependency removal, THE Build_System SHALL compile and pass all existing tests without errors related to the removed dependency.
