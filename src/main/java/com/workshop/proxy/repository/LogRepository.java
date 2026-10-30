package com.workshop.proxy.repository;

import com.workshop.proxy.model.LogEntry;
import org.springframework.stereotype.Repository;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

@Repository
public class LogRepository {

    private static final int MAX_ENTRIES = 1000;

    private final Deque<LogEntry> entries = new ArrayDeque<>();

    public synchronized void add(LogEntry entry) {
        entries.addFirst(entry);
        if (entries.size() > MAX_ENTRIES) {
            entries.removeLast();
        }
    }

    // Newest first
    public synchronized List<LogEntry> findAll() {
        return new ArrayList<>(entries);
    }
}
