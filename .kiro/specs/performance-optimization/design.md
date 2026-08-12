# Design Document

## Introduction

This design document describes the architecture and implementation approach for three performance optimizations to the Globeco Pricing Service: replacing the Java-based database migration with a static SQL migration, bounding the Caffeine cache size, and removing an unused dependency. The changes are localized to three files and require no new interfaces or services.

## Architecture Overview

The optimizations target three independent subsystems of the application:

1. **Database Migration Layer** — Flyway migrations executed at application startup
2. **Caching Layer** — Caffeine in-memory cache configuration
3. **Build System** — Gradle dependency declarations

Each change is self-contained and can be implemented and verified independently. No changes to controllers, services, repositories, or entity classes are required.

```
┌─────────────────────────────────────────────────────────────────┐
│                    Globeco Pricing Service                        │
├─────────────────────────────────────────────────────────────────┤
│  Startup                                                         │
│  ┌─────────────────────────────────────────┐                    │
│  │  Flyway Migrations                       │                    │
│  │  ┌─────────────┐   ┌──────────────────┐ │                    │
│  │  │ V1__init    │──▶│ V2__LoadPricing  │ │                    │
│  │  │ (schema)    │   │ (SQL: batch INS) │ │  ← Change 1       │
│  │  └─────────────┘   └──────────────────┘ │                    │
│  └─────────────────────────────────────────┘                    │
│                                                                  │
│  Runtime                                                         │
│  ┌───────────────┐    ┌───────────────────┐                     │
│  │  CacheConfig  │───▶│  Caffeine Cache   │                     │
│  │  maxSize=500  │    │  prices           │  ← Change 2        │
│  │  TTL=5min     │    │  pricesByTicker   │                     │
│  └───────────────┘    └───────────────────┘                     │
│                                                                  │
│  Build                                                           │
│  ┌───────────────────────────────────────────┐                  │
│  │  build.gradle                             │                   │
│  │  - Remove commons-math3                    │  ← Change 3     │
│  └───────────────────────────────────────────┘                  │
└─────────────────────────────────────────────────────────────────┘
```

## Component Design

### Component 1: Static SQL Migration (`V2__LoadPricingData.sql`)

**Location:** `src/main/resources/db/migration/V2__LoadPricingData.sql`

**Purpose:** Replace the Java-based migration `V2__LoadPricingData.java` with a static SQL file that loads all 471 pricing rows using batch INSERT statements.

**Design Decisions:**

- Use multi-row INSERT syntax: `INSERT INTO price (price_date, ticker, price, price_std) VALUES (...), (...), ...;`
- Group INSERT statements into batches of approximately 50 rows each to balance between statement size and parse efficiency
- All rows use the fixed date `2017-05-18` matching the `selected_prices.csv` dataset
- The `id` column auto-increments via the `serial` type, and `version` defaults to `1` — neither is specified in the INSERT
- Standard SQL syntax compatible with PostgreSQL 12+ and Testcontainers

**SQL Structure:**

```sql
-- V2__LoadPricingData.sql
-- Loads 471 pricing rows for date 2017-05-18 from selected_prices.csv

INSERT INTO price (price_date, ticker, price, price_std) VALUES
('2017-05-18', 'A', 55.85000000, 0.388),
('2017-05-18', 'AAL', 44.65000000, 1.5461),
-- ... additional rows in groups of ~50 ...
('2017-05-18', 'ZTS', 57.14000000, 0.5372);
```

**File Removal:**

The Java migration file at `src/main/java/org/kasbench/globeco_pricing_service/db/migration/V2__LoadPricingData.java` shall be deleted. Since the Flyway configuration in `build.gradle` includes `classpath:db/migration` as a migration location, the new SQL file in `src/main/resources/db/migration/` will be discovered automatically.

**Resource Cleanup:**

The static files `static/prices.csv.gz` and `static/dates.csv` are no longer referenced by any migration and may be removed in a future cleanup. However, they are not in scope for this change since `selected_prices.csv` remains the source of truth for the data.

### Component 2: Bounded Cache Configuration (`CacheConfig.java`)

**Location:** `src/main/java/org/kasbench/globeco_pricing_service/CacheConfig.java`

**Purpose:** Add a maximum size limit of 500 entries to prevent unbounded memory growth.

**Current Implementation:**

```java
@Bean
public Caffeine<Object, Object> caffeineConfig() {
    return Caffeine.newBuilder().expireAfterWrite(5, TimeUnit.MINUTES);
}
```

**New Implementation:**

```java
@Bean
public Caffeine<Object, Object> caffeineConfig() {
    return Caffeine.newBuilder()
            .maximumSize(500)
            .expireAfterWrite(5, TimeUnit.MINUTES);
}
```

**Behavior:**

- The `maximumSize(500)` directive sets a maximum of 500 entries across the cache
- When the cache is at capacity, Caffeine's Window TinyLfu eviction policy removes the least recently/frequently used entries to make room for new ones
- The existing `expireAfterWrite(5, TimeUnit.MINUTES)` policy is preserved, so entries expire after 5 minutes regardless of cache size
- Both `prices` and `pricesByTicker` caches share this configuration via the shared `Caffeine` bean

**Note on Cache Semantics:**

The `prices` cache stores a single entry (key: `SimpleKey.EMPTY` from the no-arg `getAllPrices()` call) containing the full list of prices. The `pricesByTicker` cache stores one entry per unique ticker. With 471 distinct tickers, the 500-entry limit provides headroom while preventing runaway growth.

### Component 3: Dependency Removal (`build.gradle`)

**Location:** `build.gradle`

**Purpose:** Remove the unused `commons-math3` dependency to reduce artifact size and attack surface.

**Change:**

Remove the following line from the `dependencies` block:

```gradle
implementation group: 'org.apache.commons', name: 'commons-math3', version: '3.6.1'
```

**Verification:**

A search of the entire codebase confirms no Java source file imports from `org.apache.commons.math3`. The dependency is purely a dead reference.

## Interfaces

No new interfaces or API changes are introduced. The existing REST endpoints, service interfaces, and repository contracts remain unchanged. The optimizations are purely internal implementation changes that do not affect the public contract of the service.

## Data Models

No changes to data models. The `Price` entity and the `price` database table schema remain unchanged. The migration loads data into the existing schema defined by `V1__init_schema.sql`.

**Existing Schema (unchanged):**

| Column     | Type           | Constraints        |
|------------|----------------|--------------------|
| id         | serial         | PRIMARY KEY        |
| price_date | date           | NOT NULL           |
| ticker     | varchar(20)    | NOT NULL           |
| price      | decimal(18,8)  | NOT NULL           |
| price_std  | float          | NOT NULL           |
| version    | integer        | NOT NULL DEFAULT 1 |

## Error Handling

### Migration Errors

- If the SQL migration file contains syntax errors, Flyway will fail at startup with a descriptive error and the application will not start. This is standard Flyway behavior and is preferred — it prevents the application from running with an incomplete database.
- Duplicate execution is prevented by Flyway's migration history table (`flyway_schema_history`).

### Cache Eviction

- When the cache reaches its maximum size (500 entries), Caffeine silently evicts entries based on its frequency/recency algorithm. No errors are thrown; subsequent cache misses result in database queries as expected.
- Eviction is asynchronous and does not block the calling thread.

### Build Errors

- If any source file were to reference `commons-math3`, the build would fail at compile time with a clear "cannot find symbol" error. Verification confirms no such references exist.

## Testing Strategy

### Integration Tests

- **Migration verification:** Run the application with Testcontainers and verify that the `price` table contains exactly 471 rows after startup.
- **Cache behavior:** Exercise the price service endpoints and verify cache hit/miss behavior through Micrometer metrics or direct cache inspection.
- **Build verification:** Execute `./gradlew build` and verify successful compilation and test execution.

### Manual Verification

- Inspect the built JAR/Docker image to confirm `commons-math3` is absent.
- Monitor application memory usage under load to confirm bounded growth.

## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system — essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*

### Acceptance Criteria Testing Prework

**Requirement 1: Replace Java Migration with Static SQL Migration**

1.1. WHEN the Pricing_Service starts, THE Data_Migration SHALL load pricing data from a SQL migration file named `V2__LoadPricingData.sql` located in `src/main/resources/db/migration/`.
  - Thoughts: This tests that the migration file exists in the correct location and is discovered by Flyway. This is a one-time configuration/smoke test — either the file is there and Flyway runs it, or it's not.
  - Classification: SMOKE
  - Test Strategy: Start the application with Testcontainers and verify Flyway successfully executes V2.

1.2. THE Data_Migration SHALL insert all 471 rows from the Selected_Prices_Dataset into the Price_Table using batch INSERT statements.
  - Thoughts: This is a deterministic, exact-count verification. The SQL file either inserts exactly 471 rows or it doesn't. This doesn't vary with input — the file is static. A single integration test suffices.
  - Classification: INTEGRATION
  - Test Strategy: Run the migration, count rows in the price table, assert exactly 471.

1.3. THE Data_Migration SHALL insert each row with the values: `price_date` set to `2017-05-18`, `ticker` from the dataset, `price` from the dataset, and `price_std` from the dataset.
  - Thoughts: This tests data fidelity — that the SQL file correctly reproduces the CSV data. We can verify this as a property: for any row in the source CSV, there should be a corresponding row in the database with matching values. This tests OUR code (the generated SQL), behavior varies with each row in the dataset, and it's cheap to run.
  - Classification: PROPERTY
  - Test Strategy: For each row in selected_prices.csv, verify the database contains a matching row with identical ticker, price, and price_std values.

1.4. THE Data_Migration SHALL use standard SQL INSERT statements compatible with PostgreSQL and Testcontainers environments.
  - Thoughts: This is a compatibility constraint validated by the fact that Testcontainers (which uses PostgreSQL) can successfully execute the migration. It's a smoke test.
  - Classification: SMOKE
  - Test Strategy: Run the migration against a Testcontainers PostgreSQL instance — success means compatibility.

1.5. WHEN the Data_Migration completes, THE Price_Table SHALL contain exactly 471 rows with `price_date` equal to `2017-05-18`.
  - Thoughts: This is essentially the same as 1.2 with the additional date constraint. It's a deterministic count check. A single integration test verifies both the count and the date.
  - Classification: INTEGRATION
  - Test Strategy: Query `SELECT COUNT(*) FROM price WHERE price_date = '2017-05-18'` and assert 471.

1.6. THE Build_System SHALL exclude the Java-based migration file `V2__LoadPricingData.java` from the compiled application.
  - Thoughts: This is a file-system check — either the file is deleted or it isn't. A one-time verification.
  - Classification: SMOKE
  - Test Strategy: Verify the file does not exist in the source tree after implementation.

**Requirement 2: Bound Cache Size**

2.1. THE Cache_Configuration SHALL set a maximum size of 500 entries for the `prices` cache.
  - Thoughts: This is a configuration assertion — verifiable by inspecting the cache manager's configuration at runtime. It's a single check.
  - Classification: EXAMPLE
  - Test Strategy: Inject the CacheManager bean and verify the underlying Caffeine instance has maximumSize == 500.

2.2. THE Cache_Configuration SHALL set a maximum size of 500 entries for the `pricesByTicker` cache.
  - Thoughts: Same as 2.1 — configuration verification for a specific cache name.
  - Classification: EXAMPLE
  - Test Strategy: Same as 2.1 but for the pricesByTicker cache.

2.3. WHILE the cache contains 500 entries, THE Cache_Configuration SHALL evict existing entries before admitting new entries.
  - Thoughts: This tests eviction behavior. We can model this as a property: for any sequence of cache put operations exceeding 500 entries, the cache size never exceeds 500. The input varies (what keys are inserted), and 100 iterations with random keys would increase confidence. This tests OUR configuration's effect on Caffeine's behavior.
  - Classification: PROPERTY
  - Test Strategy: Generate random cache entries beyond 500, verify cache size never exceeds 500 after each insertion.

2.4. THE Cache_Configuration SHALL retain the existing time-based expiration policy of 5 minutes after write.
  - Thoughts: This is a configuration assertion — verifiable by inspecting the configured expiration duration.
  - Classification: EXAMPLE
  - Test Strategy: Inspect the Caffeine configuration and verify expireAfterWrite is 5 minutes.

**Requirement 3: Remove Unused Dependency**

3.1. THE Build_System SHALL exclude the `org.apache.commons:commons-math3` library from the project dependencies.
  - Thoughts: This is a build configuration check — either the dependency is present in build.gradle or it's not.
  - Classification: SMOKE
  - Test Strategy: Parse build.gradle and verify commons-math3 is absent.

3.2. WHEN the project is built, THE Build_System SHALL produce a deployable artifact that does not contain the `commons-math3` library.
  - Thoughts: This is an artifact inspection check — build the project and verify the JAR doesn't contain commons-math3 classes.
  - Classification: INTEGRATION
  - Test Strategy: Build the project, inspect the artifact's dependency tree or contents.

3.3. WHEN the project is built after the dependency removal, THE Build_System SHALL compile and pass all existing tests without errors related to the removed dependency.
  - Thoughts: This is a build verification — run the full build and test suite. Either it passes or it doesn't.
  - Classification: SMOKE
  - Test Strategy: Execute `./gradlew build` and verify exit code 0.

### Property Reflection

After analyzing all criteria, the testable properties are:

1. **From 1.3** — Data fidelity: each CSV row has a corresponding database row with matching values.
2. **From 2.3** — Cache eviction: cache size never exceeds 500 entries.

These are logically independent properties with no redundancy:
- Property 1 validates data transformation correctness (CSV → SQL → database rows)
- Property 2 validates runtime memory bounding behavior

Both provide unique validation value and cannot be combined or consolidated.

### Property 1: Migration data fidelity

*For any* row in the `selected_prices.csv` dataset, after the V2 migration executes, the `price` table SHALL contain a row with matching `price_date` ('2017-05-18'), `ticker`, `price`, and `price_std` values.

**Validates: Requirements 1.3**

### Property 2: Cache size bound invariant

*For any* sequence of cache insertions exceeding 500 distinct keys, the cache size SHALL never exceed 500 entries at any observation point after eviction processing completes.

**Validates: Requirements 2.3**
