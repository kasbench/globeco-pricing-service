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
