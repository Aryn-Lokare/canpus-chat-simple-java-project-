package com.campuschat.server;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;


public class OnlineUsers {

    private static final ConcurrentHashMap<String, ClientHandler> onlineMap = new ConcurrentHashMap<>();

    
    private OnlineUsers() {}

   
    public static void add(String username, ClientHandler handler) {
        if (username != null && handler != null) {
            onlineMap.put(username, handler);
        }
    }

    
    public static void remove(String username) {
        if (username != null) {
            onlineMap.remove(username);
        }
    }

    
    public static boolean isOnline(String username) {
        return username != null && onlineMap.containsKey(username);
    }

   
    public static ClientHandler get(String username) {
        if (username == null) {
            return null;
        }
        return onlineMap.get(username);
    }

   
    public static Collection<ClientHandler> getAll() {
        return Collections.unmodifiableCollection(onlineMap.values());
    }

    public static void clear() {
        onlineMap.clear();
    }
}
