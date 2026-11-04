package com.campuschat.server;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stub class representing Person 3's UserStore.
 * Provides user existence checks.
 */
public class UserStore {

    private static final Set<String> registeredUsers = ConcurrentHashMap.newKeySet();
    private static volatile boolean simulateDatabaseError = false;

    static {
        // Pre-populate with default mock users for testing
        registeredUsers.add("alice");
        registeredUsers.add("bob");
        registeredUsers.add("charlie");
        registeredUsers.add("aryan");
    }

    private UserStore() {}

    /**
     * Checks if a user is registered.
     */
    public static boolean exists(String username) {
        checkDatabaseHealth();
        if (username == null) {
            return false;
        }
        return registeredUsers.contains(username.trim());
    }

    /**
     * Helper method to add a user (for tests).
     */
    public static void addUser(String username) {
        if (username != null) {
            registeredUsers.add(username.trim());
        }
    }

    /**
     * Helper method to remove a user (for tests).
     */
    public static void removeUser(String username) {
        if (username != null) {
            registeredUsers.remove(username.trim());
        }
    }

    /**
     * Simulates database failure to verify exception handling (Section 11.3).
     */
    public static void setSimulateDatabaseError(boolean throwError) {
        simulateDatabaseError = throwError;
    }

    private static void checkDatabaseHealth() {
        if (simulateDatabaseError) {
            throw new RuntimeException("Simulated database failure in UserStore");
        }
    }
}

