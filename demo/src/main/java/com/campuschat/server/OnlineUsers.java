package com.campuschat.server;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry of currently online users and their active client handlers.
 * Backed by ConcurrentHashMap for safe access across multiple client threads.
 */
public class OnlineUsers {

    private static final ConcurrentHashMap<String, ClientHandler> onlineMap = new ConcurrentHashMap<>();

    // Prevent instantiation
    private OnlineUsers() {}

    /**
     * Adds an online user and their handler.
     */
    public static void add(String username, ClientHandler handler) {
        if (username != null && handler != null) {
            onlineMap.put(username, handler);
        }
    }

    /**
     * Removes a user from the registry.
     */
    public static void remove(String username) {
        if (username != null) {
            onlineMap.remove(username);
        }
    }

    /**
     * Checks if a user is currently online.
     */
    public static boolean isOnline(String username) {
        return username != null && onlineMap.containsKey(username);
    }

    /**
     * Returns the ClientHandler of an online user, or null if not online.
     */
    public static ClientHandler get(String username) {
        if (username == null) {
            return null;
        }
        return onlineMap.get(username);
    }

    /**
     * Returns an unmodifiable collection of all active client handlers.
     */
    public static Collection<ClientHandler> getAll() {
        return Collections.unmodifiableCollection(onlineMap.values());
    }

    /**
     * Clears all entries from the online users registry.
     */
    public static void clear() {
        onlineMap.clear();
    }
}
