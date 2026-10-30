# Defense: likely professor questions

**1. What is the difference between Proxy and Facade?**
A Proxy has the same interface as one object and controls access to it. A Facade offers a new, simpler interface over several objects.

**2. What is the difference between the real object and the proxy?**
The real object does the work (`InventoryService` manages stock). The proxy does no business work: `LoggingProxy` validates, logs, caches and then delegates.

**3. Where is the pattern applied in your project?**
`LoggingProxy<T>` wraps the 3 services and both implement `MicroserviceProxy<T>`. `ProxyConfig` builds one proxy per service, and controllers only receive those proxies.

**4. Does the proxy add overhead?**
Yes: one extra call plus validation, logging and a cache lookup on every request. It is small here, and the cache saves time on repeated reads, as the `durationMs` of cache hits shows.

**5. Why a generic interface?**
With `MicroserviceProxy<T>` and `execute(operation, params)`, a single `LoggingProxy` wraps any service. Without it, I would need one proxy class per service.

**6. What is lazy loading?**
Creating an object only when it is first needed. The proxy starts with `realService = null` and calls `supplier.get()` on the first valid call only.

**7. Where are lazy loading and cache applied in your code?**
Lazy loading is in `LoggingProxy.loadRealService()`, with the `Supplier` passed from `ProxyConfig`. Cache is in `LoggingProxy.readFromCache()` and `callRealService()`: 5-second TTL, read-only operations only, cleared on any write.
