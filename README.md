# Microservices monitoring system with a Logging and Auditing Proxy

**Production URLs**

- Frontend (Vercel): https://proxy-checkpoint.vercel.app
- Backend (Render): https://proxy-monitor-backend-hjad.onrender.com

## Description

A system with 3 microservices (Inventory, Orders, Payments). A Logging Proxy sits in front of each one and intercepts every call to record usage metrics, response times and errors. The React dashboard, Checkpoint, shows this data in real time and draws each call as a packet that the proxy lets through, answers from cache or blocks.

The 3 "microservices" are service classes inside one Spring Boot app (one deployment). Storage is in memory only, so data resets when the app restarts.

- Backend: Spring Boot 3, Java 17, Maven (this repository)
- Frontend: React, Vite, Recharts ([FRONTEND-PROXY-](https://github.com/ssantivr/FRONTEND-PROXY-))

## Proxy pattern theory

Proxy is a **structural** design pattern used to **control access** to an object or resource. It acts as an **intermediary** between the client and the object to be accessed.

### Structure in this project

| Role | Class in this project | Class example |
| --- | --- | --- |
| 1. Common interface | `MicroserviceProxy<T>` | `SocialMediaProfile` |
| 2. Real object | `InventoryService`, `OrderService`, `PaymentService` | `RealProfile` |
| 3. Proxy | `LoggingProxy<T>` | `ProfileProxy` |
| 4. Client | `ServiceController`, `LoadSimulator`, `DemoMain` | `Main` |

### Advantages and where they are applied

| Advantage | Where | How |
| --- | --- | --- |
| Access control | `LoggingProxy.validateAccess()` | Rejects a null, blank or unsupported operation, logs ERROR and never reaches the real service |
| Lazy loading | `LoggingProxy.loadRealService()` | The real service is created by the `Supplier` on the first call only. `ProxyConfig` passes the `Supplier`, not an instance |
| Cache | `LoggingProxy.readFromCache()` and `callRealService()` | Read-only operations are cached for 5 seconds. Any write clears the cache. Writes are never cached, and hits are logged with `cacheHit: true` |
| Common interface | `MicroserviceProxy<T>` | Clients declare `MicroserviceProxy<?>` and never know whether they use a proxy or a real service |

### Disadvantages in this project

- **Complexity:** every call goes through one more class, so following a request means reading `LoggingProxy` as well as the service.
- **Possible overhead:** the proxy adds validation, logging and cache lookups to each call. It is small here, but it exists on every request.
- **Additional design requirement:** the interface had to be designed first. The generic `execute(operation, params)` lets one proxy wrap any service, but each service must declare its supported and read-only operations.

## Run locally

Backend (http://localhost:8080):

```bash
./mvnw spring-boot:run
```

Tests:

```bash
./mvnw test
```

Console demo (lazy loading):

```bash
./mvnw -q compile exec:java -Dexec.mainClass=com.workshop.proxy.demo.DemoMain
```

Frontend (http://localhost:5173), from the frontend repository:

```bash
npm install
cp .env.example .env
npm run dev
```

## Endpoints

The body of the `/api/services` endpoints is a JSON array of params.

| Method | Path | Description |
| --- | --- | --- |
| POST | `/api/services/inventory/{operation}` | `listProducts`, `checkStock [productId]`, `addStock [productId, quantity]`, `reduceStock [productId, quantity]` |
| POST | `/api/services/orders/{operation}` | `createOrder [customer, productId, quantity]`, `getOrder [orderId]`, `listOrders`, `cancelOrder [orderId]` |
| POST | `/api/services/payments/{operation}` | `processPayment [orderId, amount]`, `getPaymentStatus [paymentId]`, `refundPayment [paymentId]` |
| GET | `/api/metrics/summary` | Total calls, success rate, error rate and average time per service |
| GET | `/api/metrics/logs` | Filtered logs. Optional: `service`, `status`, `from`, `to`, `page`, `size` (max 100) |
| POST | `/api/metrics/simulate-load` | Generates 50 random calls |
| GET | `/api/health` | Health check |

```bash
curl -X POST localhost:8080/api/services/inventory/listProducts
```

```bash
curl -X POST localhost:8080/api/services/inventory/reduceStock -H "Content-Type: application/json" -d '["P-1001", 2]'
```

```bash
curl -X POST localhost:8080/api/services/orders/createOrder -H "Content-Type: application/json" -d '["Ana", "P-1001", 1]'
```

```bash
curl -X POST localhost:8080/api/services/payments/processPayment -H "Content-Type: application/json" -d '["O-1001", 99.5]'
```

```bash
curl localhost:8080/api/metrics/summary
```

```bash
curl "localhost:8080/api/metrics/logs?service=payments&status=ERROR&from=2026-01-01T00:00:00Z&page=0&size=10"
```

```bash
curl -X POST localhost:8080/api/metrics/simulate-load
```

Notes:

- Metrics are calculated from the stored logs, so they cover the newest 1000 calls.
- `SimulatedLatency` adds a 10-80 ms pause in the real services, because in-memory operations take about 0 ms. Cache hits skip it.
- 10% of the calls that reach `PaymentService` fail on purpose with "Payment gateway timeout". Cache hits do not reach it.
- The services are independent: `createOrder` does not reduce stock or check that the product exists.

## Deployment

### Environment variables

| Variable | Where | Value |
| --- | --- | --- |
| `PORT` | Render | Set automatically by Render (default 8080 locally) |
| `ALLOWED_ORIGIN` | Render | Vercel URL of the frontend (CORS) |
| `VITE_API_URL` | Vercel | Render URL of the backend |

### Backend on Render

`render.yaml` describes the service and builds `Dockerfile`.

1. In Render, choose New > Blueprint and select this repository.
2. Leave `ALLOWED_ORIGIN` empty for now and apply the Blueprint.

The free plan sleeps after 15 minutes without traffic. The first request can then take about a minute, and in-memory logs start empty.

### Frontend on Vercel

From the frontend repository:

```bash
vercel --prod --build-env VITE_API_URL=https://your-backend.onrender.com
```

### Connect both

In Render, set `ALLOWED_ORIGIN` to the Vercel URL and save. Render redeploys the service.
