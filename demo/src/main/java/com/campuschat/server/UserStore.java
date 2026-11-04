package com.campuschat.server;

import java.util.*;

/**
 * Person 3's UserStore (in-memory version).
 * Same method names as the stub, so other code does not change.
 * Later this will be rewritten with Hibernate.
 */
public class UserStore {

    private static final Set<String> registeredUsers =
            Collections.synchronizedSet(new LinkedHashSet<>());
    private static volatile boolean simulateDatabaseError = false;

    static {
        for (String name : new String[]{"alice", "bob", "charlie", "aryan"}) {
            registeredUsers.add(name);
        }
    }

    private UserStore() { }

    // "Alice " and "alice" are the same user
    private static String clean(String name) {
        return name == null ? "" : name.trim().toLowerCase();
    }

    public static boolean exists(String username) {
        checkDatabaseHealth();
        return registeredUsers.contains(clean(username));
    }

    // returns a copy so other code cannot change the set by accident
    public static List<String> allUsernames() {
        checkDatabaseHealth();
        synchronized (registeredUsers) {
            return new ArrayList<>(registeredUsers);
        }
    }

    public static void addUser(String username) {
        String name = clean(username);
        if (!name.isEmpty()) {
            registeredUsers.add(name);          // a Set never keeps duplicates
        }
    }

    public static void removeUser(String username) {   // used by tests
        registeredUsers.remove(clean(username));
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

