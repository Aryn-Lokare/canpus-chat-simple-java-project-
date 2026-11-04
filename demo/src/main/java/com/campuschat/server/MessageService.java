package com.campuschat.server;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Stub class representing Person 2's MessageService.
 * Handles broadcast of chat messages and delivery of offline messages.
 */
public class MessageService {

    private static final ConcurrentHashMap<String, List<String>> offlineQueue = new ConcurrentHashMap<>();
    private static volatile boolean simulateDatabaseError = false;

    private MessageService() {}

    /**
     * Broadcasts a chat message to all online clients in the format MSG|sender|text.
     */
    public static void sendChat(String sender, String text) {
        checkDatabaseHealth();
        String message = "MSG|" + sender + "|" + text;
        for (ClientHandler handler : OnlineUsers.getAll()) {
            handler.send(message);
        }
    }

    /**
     * Delivers queued offline messages to a user who just logged in.
     * Wrapped between SYNC_START and SYNC_END.
     */
    public static void deliverMissed(String username, ClientHandler handler) {
        checkDatabaseHealth();
        handler.send("SYNC_START");
        List<String> missed = offlineQueue.remove(username);
        if (missed != null) {
            for (String msg : missed) {
                handler.send(msg);
            }
        }
        handler.send("SYNC_END");
    }

    /**
     * Helper to enqueue an offline message for testing deliverMissed.
     */
    public static void queueOfflineMessage(String username, String sender, String text) {
        offlineQueue.computeIfAbsent(username, k -> new CopyOnWriteArrayList<>())
                    .add("MSG|" + sender + "|" + text);
    }

    /**
     * Clears any queued offline messages.
     */
    public static void clearQueue() {
        offlineQueue.clear();
    }

    /**
     * Simulates database failure to verify exception handling (Section 11.3).
     */
    public static void setSimulateDatabaseError(boolean throwError) {
        simulateDatabaseError = throwError;
    }

    private static void checkDatabaseHealth() {
        if (simulateDatabaseError) {
            throw new RuntimeException("Simulated database failure in MessageService");
        }
    }
}

