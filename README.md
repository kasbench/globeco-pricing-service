# pricing-service
The GlobeCo Pricing Service. This is part of the KASBench suite of applications for benchmarking Kubernetes autoscaling.

## Overview

The Pricing Service generates synthetic prices for S&P 500 tickers as part of a portfolio management benchmark. It exposes REST APIs for retrieving sampled prices, supports in-memory caching, and is designed for scalable, cloud-native deployment on Kubernetes.

## Technology Stack
- Java 21
- Spring Boot 3.4.5
- PostgreSQL 17
- Flyway (database migrations)
- Caffeine (caching)
- Testcontainers (integration testing)
- Apache Commons Math (sampling)
- Springdoc OpenAPI (API docs)

## Database
- Host: `globeco-pricing-service-postgresql`
- Port: `5435`
- Database: `postgres`
- Schema: `public`
- Table: `price`

| Column      | Type         | Description                |
| ----------- | ------------ | -------------------------- |
| id          | serial (PK)  | Unique row ID              |
| price_date  | date         | Date of the price          |
| ticker      | varchar(20)  | Ticker symbol              |
| price       | decimal(18,8)| Mean price                 |
| price_std   | float        | Std deviation of price     |
| version     | integer      | Optimistic concurrency     |

## API Endpoints

Base path: `/api/v1`

### Get all prices (sampled)
- **GET** `/api/v1/prices`
- **Response:**
```json
[
  {
    "id": 1,
    "ticker": "AAPL",
    "date": "2024-06-10",
    "open": 101.23,
    "close": 101.23,
    "high": 101.23,
    "low": 101.23,
    "volume": 2000
  },
  ...
]
```

### Get sampled price for a ticker
- **GET** `/api/v1/price/{ticker}`
- **Response:**
```json
{
  "id": 1,
  "ticker": "AAPL",
  "date": "2024-06-10",
  "open": 101.23,
  "close": 101.23,
  "high": 101.23,
  "low": 101.23,
  "volume": 2000
}
```
- **404** if ticker not found

## Caching
- Uses Caffeine with a 5-minute TTL for price lookups.

## Database Migrations
- Managed by Flyway.
- Initial schema and stochastic data load via Java migration.

## Testing
- Unit and integration tests use JUnit, Spring Boot Test, and Testcontainers.
- API endpoints are tested with MockMvc.

## Health Checks

The service exposes the following health check endpoints for Kubernetes:

- **Liveness Probe:** `GET /actuator/health/liveness`  
  Used by Kubernetes to determine if the application is running.
- **Readiness Probe:** `GET /actuator/health/readiness`  
  Used by Kubernetes to determine if the application is ready to receive traffic.
- **Startup Probe:** `GET /actuator/health/startup`  
  Used by Kubernetes to determine if the application has started successfully.

All health endpoints are available on port 8083.

## Deployment Notes
- Designed for Kubernetes with health probes and resource limits.
- See `Dockerfile` and Kubernetes manifests for deployment details.

## Docker

To build the Docker image:

```sh
docker build -t globeco-pricing-service .
```

To run the service:

```sh
docker run -p 8083:8083 globeco-pricing-service
```

The container exposes port 8083 and includes a healthcheck for `/actuator/health/liveness`.

## Kubernetes

To deploy the service to Kubernetes:

1. Ensure the `globeco` namespace exists (or create it):
   ```sh
   kubectl create namespace globeco
   ```
2. Apply the deployment, service, and autoscaler manifests:
   ```sh
   kubectl apply -f k8s/globeco-pricing-service-deployment.yaml
   kubectl apply -f k8s/globeco-pricing-service-hpa.yaml
   ```

- The deployment starts with 1 replica and can scale up to 100 based on CPU usage.
- Resource limits: 100 millicores CPU, 200 MiB memory per pod.
- Liveness, readiness, and startup probes are configured with a 240s timeout.
- The service is available on port 8083 within the cluster.

## OpenAPI & Swagger UI

- **OpenAPI schema:** [http://localhost:8083/v3/api-docs](http://localhost:8083/v3/api-docs)
- **Swagger UI:** [http://localhost:8083/swagger-ui.html](http://localhost:8083/swagger-ui.html)

These endpoints provide interactive API documentation and allow you to test the service endpoints directly from your browser.

## Telemetry Instrumentation (Metrics & Traces)

The GlobeCo Pricing Service is instrumented for both metrics and distributed tracing using OpenTelemetry and Micrometer. This enables observability for performance, health, and distributed request flows.

### Metrics
- **Exported via:** [Micrometer](https://micrometer.io/) with OTLP exporter
- **Collector endpoint:**
  - `http://otel-collector-collector.monitoring.svc.cluster.local:4318/v1/metrics`
- **Forwarded to:** Prometheus (via OpenTelemetry Collector remote write)
- **Configuration (see `application.properties`):**
  ```properties
  management.otlp.metrics.export.url=http://otel-collector-collector.monitoring.svc.cluster.local:4318/v1/metrics
  management.otlp.metrics.export.resource-attributes.service.name=globeco-pricing-service
  management.otlp.metrics.export.resource-attributes.service.version=1.0.0
  management.otlp.metrics.export.resource-attributes.service.namespace=globeco
  management.otlp.metrics.export.resource-attributes.service.instance.version=1.0.0
  management.otlp.metrics.export.resource-attributes.service.instance.namespace=globeco
  ```
- **What is instrumented:**
  - JVM, system, and Spring Boot metrics (memory, CPU, HTTP requests, cache, etc.)
  - Custom metrics can be added via Micrometer if needed

### Distributed Tracing
- **Exported via:** Micrometer Tracing Bridge (OpenTelemetry)
- **Collector endpoint:**
  - `http://otel-collector-collector.monitoring.svc.cluster.local:4318/v1/traces`
- **Forwarded to:** Jaeger (via OpenTelemetry Collector)
- **Configuration (see `application.properties`):**
  ```properties
  management.otlp.tracing.endpoint=http://otel-collector-collector.monitoring.svc.cluster.local:4318/v1/traces
  management.otlp.tracing.resource-attributes.service.name=globeco-pricing-service
  management.otlp.tracing.resource-attributes.service.version=1.0.0
  management.otlp.tracing.resource-attributes.service.namespace=globeco
  management.otlp.tracing.resource-attributes.service.instance.namespace=globeco
  management.otlp.tracing.sampling.probability=1.0
  ```
- **What is instrumented:**
  - All HTTP requests (controllers/endpoints) are traced automatically
  - Spans are created for incoming requests and propagated downstream
  - Custom spans can be added in business logic if needed

### Viewing Telemetry Data
- **Metrics:**
  - View in Prometheus or Grafana dashboards (via OpenTelemetry Collector remote write)
- **Traces:**
  - View in Jaeger UI (e.g., `http://jaeger.orchestration.svc.cluster.local:16686`)

### References
- See `documentation/OTEL_CONFIGURATION_GUIDE.md` for full integration details and troubleshooting.
- [OpenTelemetry Collector](https://opentelemetry.io/docs/collector/)
- [Jaeger](https://www.jaegertracing.io/)
- [Prometheus](https://prometheus.io/)
