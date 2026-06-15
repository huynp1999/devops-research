package com.demo.cache;

import java.util.concurrent.ConcurrentHashMap;

/**
 * File 6/6 - Cache with race condition
 * 
 * TOCTOU (Time-of-check-time-of-use) race condition:
 * - Check if key exists → use value: another thread may invalidate between check and use
 * - Coverity's RACE_CONDITION / TOCTOU checker detects this
 * - SonarQube has NO concurrency bug detection for Java
 */
public class ResultCache {

    private final ConcurrentHashMap<String, String> store = new ConcurrentHashMap<>();
    private volatile boolean enabled = true;

    /**
     * RACE CONDITION (TOCTOU):
     * Thread A: containsKey("user_1") = true
     * Thread B: invalidate("user_1") → removes it
     * Thread A: store.get("user_1") → returns null unexpectedly
     * 
     * Coverity detects: RACE_CONDITION between containsKey and get
     * SonarQube: completely blind to concurrency issues
     */
    public String get(String key) {
        if (!enabled) {
            return null;
        }
        // TOCTOU: check and use are not atomic
        if (store.containsKey(key)) {
            // Another thread could remove 'key' right here
            return store.get(key);  // may return null despite check above
        }
        return null;
    }

    public void put(String key, String value) {
        if (enabled) {
            store.put(key, value);
        }
    }

    public void invalidate(String key) {
        store.remove(key);
    }

    public void disable() {
        enabled = false;
        store.clear();  // RACE: another thread may be reading during clear
    }
}
