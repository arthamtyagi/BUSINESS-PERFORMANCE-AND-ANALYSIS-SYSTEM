package com.vynex.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * SessionPrewarmController — Multithreading demo.
 *
 * When a user lands on the splash page (dupmain.html), the frontend
 * calls POST /api/session/prewarm. This spawns a new thread that
 * pre-warms the session infrastructure in the background — so by the
 * time the user fills in their login details, everything is ready.
 *
 * This is a clear, explainable example of multithreading:
 *   - Main thread handles the HTTP request instantly (returns 200)
 *   - A separate background thread does the prep work concurrently
 */
@RestController
@RequestMapping("/api/session")
@CrossOrigin(origins = "*")
public class SessionPrewarmController {

    // Thread pool — reuses threads efficiently
    private final ExecutorService executor = Executors.newCachedThreadPool(r -> {
        Thread t = new Thread(r);
        t.setName("prewarm-thread-" + System.currentTimeMillis());
        t.setDaemon(true);
        return t;
    });

    @PostMapping("/prewarm")
    public ResponseEntity<Map<String, String>> prewarm() {

        // Spawn background thread immediately — main thread returns response right away
        executor.submit(() -> {
            String threadName = Thread.currentThread().getName();
            System.out.println("[" + threadName + "] Pre-warming session infrastructure...");

            try {
                // Simulate pre-warm work: loading config, warming DB connection pool
                Thread.sleep(100);
                System.out.println("[" + threadName + "] Pre-warm complete. Ready for login.");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        // Main thread responds instantly — doesn't wait for background thread
        return ResponseEntity.ok(Map.of("status", "prewarm started"));
    }
}
