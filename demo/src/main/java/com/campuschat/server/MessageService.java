package com.campuschat.server;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;


public class MessageService {

    private static final ConcurrentHashMap<String, List<String>> offlineQueue = new ConcurrentHashMap<>();
    private static volatile boolean simulateDatabaseError = false;

    private MessageService() {}

    
    public static void sendChat(String sender, String text) {
        checkDatabaseHealth();
        String message = "MSG|" + sender + "|" + text;
        for (ClientHandler handler : OnlineUsers.getAll()) {
            handler.send(message);
        }
    }

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

   
    public static void queueOfflineMessage(String username, String sender, String text) {
        offlineQueue.computeIfAbsent(username, k -> new CopyOnWriteArrayList<>())
                    .add("MSG|" + sender + "|" + text);
    }

    
    public static void clearQueue() {
        offlineQueue.clear();
    }

    public static void setSimulateDatabaseError(boolean throwError) {
        simulateDatabaseError = throwError;
    }

    private static void checkDatabaseHealth() {
        if (simulateDatabaseError) {
            throw new RuntimeException("Simulated database failure in MessageService");
        }
    }
}
