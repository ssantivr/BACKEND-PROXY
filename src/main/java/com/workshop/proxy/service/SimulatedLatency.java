package com.workshop.proxy.service;

import java.util.concurrent.ThreadLocalRandom;

// In-memory operations take 0 ms, so a short pause gives the dashboard realistic response times
final class SimulatedLatency {

    private static final int MIN_MS = 10;
    private static final int MAX_MS = 80;

    private SimulatedLatency() {
    }

    static void pause() {
        try {
            Thread.sleep(ThreadLocalRandom.current().nextInt(MIN_MS, MAX_MS));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
