package com.workshop.proxy.proxy;

import com.workshop.proxy.model.LogEntry;
import com.workshop.proxy.model.LogStatus;
import com.workshop.proxy.repository.LogRepository;
import com.workshop.proxy.service.UnknownOperationException;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.stream.Collectors;

// 3. Proxy class that controls access
@Slf4j
public class LoggingProxy<T> implements MicroserviceProxy<T> {

    private static final long CACHE_TTL_MS = 5000;
    private static final int STACK_FRAMES = 4;

    private final String serviceId;
    private final Supplier<MicroserviceProxy<T>> supplier;
    private final Set<String> supportedOperations;
    private final Set<String> readOnlyOperations;
    private final LogRepository logRepository;
    private final Map<String, CachedResponse<T>> cache = new ConcurrentHashMap<>();
    private final Map<String, PendingCall> pendingCalls = new ConcurrentHashMap<>();
    private MicroserviceProxy<T> realService = null;

    public LoggingProxy(String serviceId, Supplier<MicroserviceProxy<T>> supplier, Set<String> supportedOperations,
                        Set<String> readOnlyOperations, LogRepository logRepository) {
        this.serviceId = serviceId;
        this.supplier = supplier;
        this.supportedOperations = supportedOperations;
        this.readOnlyOperations = readOnlyOperations;
        this.logRepository = logRepository;
    }

    // Proxy pattern: same method as the real service, with logging, validation, cache and lazy loading around it
    @Override
    public T execute(String operation, Object... params) {
        PendingCall call = registerStart(operation, params == null ? new Object[0] : params);
        try {
            validateAccess(operation);
            T cachedResponse = readFromCache(call);
            boolean cacheHit = cachedResponse != null;
            T response = cacheHit ? cachedResponse : callRealService(call);
            registerSuccess(call, response, cacheHit);
            return response;
        } catch (RuntimeException e) {
            registerError(call, e);
            throw e;
        }
    }

    public int pendingCallCount() {
        return pendingCalls.size();
    }

    private PendingCall registerStart(String operation, Object[] params) {
        PendingCall call = new PendingCall(UUID.randomUUID().toString(), operation, params, Instant.now(), System.nanoTime());
        pendingCalls.put(call.requestId(), call);
        log.info("START requestId={} serviceId={} operation={}", call.requestId(), serviceId, operation);
        return call;
    }

    // Proxy pattern (access control): invalid calls never reach the real service
    private void validateAccess(String operation) {
        if (operation == null || operation.isBlank()) {
            throw new UnknownOperationException("Operation is required");
        }
        if (!supportedOperations.contains(operation)) {
            throw new UnknownOperationException("Operation not supported by " + serviceId + ": " + operation);
        }
    }

    // Proxy pattern (cache): reuses a recent result of a read-only operation
    private T readFromCache(PendingCall call) {
        CachedResponse<T> cached = cache.get(call.cacheKey());
        if (cached == null || cached.isExpired()) {
            return null;
        }
        return cached.value();
    }

    private T callRealService(PendingCall call) {
        boolean readOnly = readOnlyOperations.contains(call.operation());
        MicroserviceProxy<T> service = loadRealService();
        try {
            T response = service.execute(call.operation(), call.params());
            if (readOnly) {
                cache.put(call.cacheKey(), new CachedResponse<>(response, System.currentTimeMillis() + CACHE_TTL_MS));
            }
            return response;
        } finally {
            if (!readOnly) {
                cache.clear(); // Any write operation invalidates the cache
            }
        }
    }

    // Proxy pattern (lazy loading): synchronized so concurrent first calls create only one real service
    private synchronized MicroserviceProxy<T> loadRealService() {
        if (realService == null) { // Load the real service only when needed
            realService = supplier.get();
            log.info("First call: real service loaded for serviceId={}", serviceId);
        }
        return realService;
    }

    private void registerSuccess(PendingCall call, T response, boolean cacheHit) {
        long durationMs = finish(call);
        logRepository.add(new LogEntry(call.requestId(), serviceId, String.valueOf(call.operation()), durationMs,
                LogStatus.SUCCESS, call.startedAt(), paramsOf(call), response, null, null, cacheHit));
        log.info("SUCCESS requestId={} serviceId={} operation={} durationMs={} cacheHit={}",
                call.requestId(), serviceId, call.operation(), durationMs, cacheHit);
    }

    private void registerError(PendingCall call, RuntimeException error) {
        long durationMs = finish(call);
        logRepository.add(new LogEntry(call.requestId(), serviceId, String.valueOf(call.operation()), durationMs,
                LogStatus.ERROR, call.startedAt(), paramsOf(call), null, error.getMessage(), summarizeStackTrace(error), false));
        log.error("ERROR requestId={} serviceId={} operation={} durationMs={} error={}",
                call.requestId(), serviceId, call.operation(), durationMs, error.getMessage());
    }

    private long finish(PendingCall call) {
        pendingCalls.remove(call.requestId());
        return (System.nanoTime() - call.startedNanos()) / 1_000_000;
    }

    private List<Object> paramsOf(PendingCall call) {
        return new ArrayList<>(Arrays.asList(call.params()));
    }

    private String summarizeStackTrace(RuntimeException error) {
        String frames = Arrays.stream(error.getStackTrace())
                .limit(STACK_FRAMES)
                .map(frame -> "  at " + frame)
                .collect(Collectors.joining("\n"));
        return error.getClass().getName() + ": " + error.getMessage() + "\n" + frames;
    }

    private record PendingCall(String requestId, String operation, Object[] params, Instant startedAt, long startedNanos) {

        String cacheKey() {
            return operation + Arrays.deepToString(params);
        }
    }

    private record CachedResponse<T>(T value, long expiresAtMillis) {

        boolean isExpired() {
            return System.currentTimeMillis() > expiresAtMillis;
        }
    }
}
