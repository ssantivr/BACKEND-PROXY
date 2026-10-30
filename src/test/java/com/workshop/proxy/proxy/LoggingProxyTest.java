package com.workshop.proxy.proxy;

import com.workshop.proxy.model.LogEntry;
import com.workshop.proxy.model.LogStatus;
import com.workshop.proxy.repository.LogRepository;
import com.workshop.proxy.service.UnknownOperationException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LoggingProxyTest {

    private final LogRepository logRepository = new LogRepository();
    private final AtomicInteger servicesCreated = new AtomicInteger();
    private final AtomicInteger realCalls = new AtomicInteger();

    private final LoggingProxy<String> proxy = new LoggingProxy<>("fake", this::createFakeService,
            Set.of("read", "write", "fail"), Set.of("read"), logRepository);

    private MicroserviceProxy<String> createFakeService() {
        servicesCreated.incrementAndGet();
        return (operation, params) -> {
            realCalls.incrementAndGet();
            if (operation.equals("fail")) {
                throw new IllegalStateException("Fake failure");
            }
            return "result-" + realCalls.get();
        };
    }

    private LogEntry lastLog() {
        return logRepository.findAll().get(0);
    }

    @Test
    void logsSuccessWithAllFields() {
        String response = proxy.execute("write");

        LogEntry log = lastLog();
        assertEquals("result-1", response);
        assertEquals(LogStatus.SUCCESS, log.status());
        assertEquals("fake", log.serviceId());
        assertEquals("write", log.operation());
        assertEquals(response, log.response());
        assertNotNull(log.requestId());
        assertNotNull(log.timestamp());
        assertEquals(0, proxy.pendingCallCount());
    }

    @Test
    void logsErrorWithStackSummaryAndRethrows() {
        assertThrows(IllegalStateException.class, () -> proxy.execute("fail"));

        LogEntry log = lastLog();
        assertEquals(LogStatus.ERROR, log.status());
        assertEquals("Fake failure", log.errorMessage());
        assertTrue(log.stackSummary().startsWith("java.lang.IllegalStateException: Fake failure"));
        assertEquals(5, log.stackSummary().lines().count());
        assertNull(log.response());
    }

    @Test
    void storesInputParams() {
        proxy.execute("write", "P-1001", 3);

        assertEquals(List.of("P-1001", 3), lastLog().params());
    }

    @Test
    void loadsRealServiceOnlyOnceAndOnlyAfterFirstCall() {
        assertEquals(0, servicesCreated.get());

        proxy.execute("write");
        proxy.execute("write");

        assertEquals(1, servicesCreated.get());
        assertEquals(2, realCalls.get());
    }

    @Test
    void invalidOperationNeverReachesRealService() {
        assertThrows(UnknownOperationException.class, () -> proxy.execute("delete"));
        assertThrows(UnknownOperationException.class, () -> proxy.execute(" "));
        assertThrows(UnknownOperationException.class, () -> proxy.execute(null));

        assertEquals(0, servicesCreated.get());
        assertEquals(0, realCalls.get());
        assertEquals(3, logRepository.findAll().size());
        assertEquals(LogStatus.ERROR, lastLog().status());
    }

    @Test
    void cachesReadOperationsAndStillLogsHits() {
        String first = proxy.execute("read", "A");
        String second = proxy.execute("read", "A");

        assertEquals(first, second);
        assertEquals(1, realCalls.get());
        assertTrue(lastLog().cacheHit());
        assertFalse(logRepository.findAll().get(1).cacheHit());
    }

    @Test
    void cacheDependsOnParams() {
        proxy.execute("read", "A");
        proxy.execute("read", "B");

        assertEquals(2, realCalls.get());
    }

    @Test
    void writeOperationsInvalidateCacheAndAreNeverCached() {
        proxy.execute("read");
        proxy.execute("write");
        proxy.execute("write");
        proxy.execute("read");

        assertEquals(4, realCalls.get());
        assertFalse(lastLog().cacheHit());
    }

    @Test
    void cacheExpiresAfterTtl() throws InterruptedException {
        proxy.execute("read");
        Thread.sleep(5100);
        proxy.execute("read");

        assertEquals(2, realCalls.get());
    }
}
