# Requirements checklist

| Item | Requirement | File / class | Status |
| --- | --- | --- | --- |
| N1 | Frontend and backend | BACKEND-PROXY, FRONTEND-PROXY- repositories | Done |
| N2 | Both deployed to PROD | Render, Vercel (URLs in `README.md`) | Pending |
| N3 | Simple code, one app, in-memory | `ProxyApplication`, `ConcurrentHashMap` in services, `LogRepository` | Done |
| B1 | Generic interface | `proxy/MicroserviceProxy` | Done |
| B2 | Real services with real operations | `service/InventoryService`, `OrderService`, `PaymentService` | Done |
| B3 | Proxy wraps any service: start, result, error with stack summary | `proxy/LoggingProxy` (`registerStart`, `registerSuccess`, `registerError`, `summarizeStackTrace`) | Done |
| B4 | Log fields, thread-safe repository, max 1000, newest first | `model/LogEntry`, `repository/LogRepository` | Done |
| B5 | POST inventory endpoint | `controller/ServiceController.callInventory` | Done |
| B6 | POST orders endpoint | `controller/ServiceController.callOrders` | Done |
| B7 | POST payments endpoint | `controller/ServiceController.callPayments` | Done |
| B8 | Metrics summary | `controller/MetricsController.summary`, `metrics/MetricsService.summary` | Done |
| B9 | Filtered logs with pagination | `controller/MetricsController.logs`, `metrics/MetricsService.findLogs` | Done |
| B10 | 10% random payment failures | `service/PaymentService.failRandomly` | Done |
| B11 | Simulate 50 random calls | `metrics/LoadSimulator`, `controller/MetricsController.simulateLoad` | Done |
| F1 | 3 service cards | `components/ServiceCard.jsx`, inside `components/InterceptMap.jsx` | Done |
| F2 | Line chart, last 20 calls per service | `components/ResponseTimeChart.jsx` | Done |
| F3 | Logs table with 3-second polling | `App.jsx` (`setInterval` with cleanup), `components/LogsTable.jsx` | Done |
| F4 | Filters and pagination | `components/Filters.jsx`, `components/LogsTable.jsx` | Done |
| F5 | Simulate load button | `App.jsx` (`handleSimulateLoad`) | Done |
| F6 | Expandable log details | `components/LogsTable.jsx` (`LogDetails`) | Done |
| F7 | Red card above 15% errors | `components/ServiceCard.jsx`, `.card-problem` in `App.css` | Done |
| D1 | Backend on Render (changed from Railway at the user's request) | `render.yaml`, `Dockerfile` | Pending |
| D2 | Frontend on Vercel | `vercel.json` (frontend repository) | Pending |
| PDF 1 | Same structure, numbered comments | `MicroserviceProxy`, services, `LoggingProxy`, clients | Done |
| PDF 2 | Mandatory lazy loading | `LoggingProxy.loadRealService`, service constructors, `ProxyConfig` | Done |
| PDF 3 | Access control | `LoggingProxy.validateAccess` | Done |
| PDF 4 | Cache for read-only operations, 5-second TTL | `LoggingProxy.readFromCache`, `callRealService` | Done |
| PDF 5 | Clients use `MicroserviceProxy<?>` | `ServiceController`, `LoadSimulator`, `DemoMain` | Done |
| PDF 6 | Console demo | `demo/DemoMain` | Done |
