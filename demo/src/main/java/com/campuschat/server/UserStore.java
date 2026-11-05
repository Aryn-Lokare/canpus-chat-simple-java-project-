package com.campuschat.server;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;


public class UserStore {

    private static final Set<String> registeredUsers = ConcurrentHashMap.newKeySet();
    private static volatile boolean simulateDatabaseError = false;

    static {
        
        registeredUsers.add("alice");
        registeredUsers.add("bob");
        registeredUsers.add("charlie");
        registeredUsers.add("aryan");
    }

    private UserStore() {}

    
    public static boolean exists(String username) {
        checkDatabaseHealth();
        if (username == null) {
            return false;
        }
        return registeredUsers.contains(username.trim());
    }

    
    public static void addUser(String username) {
        if (username != null) {
            registeredUsers.add(username.trim());
        }
    }

    
    public static void removeUser(String username) {
        if (username != null) {
            registeredUsers.remove(username.trim());
        }
    }

 
    public static void setSimulateDatabaseError(boolean throwError) {
        simulateDatabaseError = throwError;
    }

    private static void checkDatabaseHealth() {
        if (simulateDatabaseError) {
            throw new RuntimeException("Simulated database failure in UserStore");
        }
    }
}
