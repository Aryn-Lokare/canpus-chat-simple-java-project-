package com.campuschat.server;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
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
            for (String key : onlineMap.keySet()) {
                if (key.equalsIgnoreCase(username)) {
                    onlineMap.remove(key);
                }
            }
        }
    }

    public static boolean isOnline(String username) {
        if (username == null) {
            return false;
        }
        if (onlineMap.containsKey(username)) {
            return true;
        }
        for (String key : onlineMap.keySet()) {
            if (key.equalsIgnoreCase(username)) {
                return true;
            }
        }
        return false;
    }

    
    public static ClientHandler get(String username) {
        if (username == null) {
            return null;
        }
        ClientHandler handler = onlineMap.get(username);
        if (handler != null) {
            return handler;
        }
        for (Map.Entry<String, ClientHandler> entry : onlineMap.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(username)) {
                return entry.getValue();
            }
        }
        return null;
    }

  
    public static Collection<ClientHandler> getAll() {
        return Collections.unmodifiableCollection(onlineMap.values());
    }

    
    public static void clear() {
        onlineMap.clear();
    }
}
