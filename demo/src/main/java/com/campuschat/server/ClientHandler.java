package com.campuschat.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;


public class ClientHandler implements Runnable {

    private final Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;
    private String username; 

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    
    public String getUsername() {
        return username;
    }

    
    public synchronized void send(String line) {
        if (writer != null) {
            writer.println(line);
        }
    }

    @Override
    public void run() {
        try {
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);

            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }

                String[] parts = line.split("\\|", 2);
                String command = parts[0].trim();
                String argument = parts.length > 1 ? parts[1] : "";

                boolean shouldDisconnect = false;

                try {
                    switch (command) {
                        case "LOGIN":
                            handleLogin(argument);
                            break;

                        case "CHAT":
                            handleChat(argument);
                            break;

                        case "LOGOUT":
                            handleLogout();
                            shouldDisconnect = true;
                            break;

                        default:
                            send("ERROR|Unknown command");
                            break;
                    }
                } catch (Exception e) {
                
                    System.err.println("[Server] Database/Service error handling command '" + command + "' for "
                            + (username != null ? username : "unauthenticated client") + ": " + e.getMessage());
                    e.printStackTrace();
                    send("ERROR|Server problem, please try again");
                }

                if (shouldDisconnect) {
                    break;
                }
            }
        } catch (IOException e) {
           
            System.out.println("[Server] Client disconnected or socket error: " + e.getMessage());
        } finally {
           
            cleanup();
        }
    }

    
    private void handleLogin(String name) {
        String trimmedName = name.trim();
        if (trimmedName.isEmpty()) {
            send("ERROR|No such user");
            return;
        }

        
        if (this.username != null) {
            send("ERROR|Already logged in");
            return;
        }

      
        if (!UserStore.exists(trimmedName)) {
            send("ERROR|No such user");
            return;
        }

        
        if (OnlineUsers.isOnline(trimmedName)) {
            send("ERROR|Already logged in");
            return;
        }


        this.username = trimmedName;
        OnlineUsers.add(this.username, this);
        send("OK|Welcome " + this.username);

        
        MessageService.deliverMissed(this.username, this);
    }

   
    private void handleChat(String text) {
       
        if (this.username == null) {
            send("ERROR|Not logged in");
            return;
        }

  
        MessageService.sendChat(this.username, text);
    }

    
    private void handleLogout() {
        if (this.username != null) {
            OnlineUsers.remove(this.username);
            this.username = null;
        }
        send("OK|Logged out");
    }

    private void cleanup() {
        if (this.username != null) {
            OnlineUsers.remove(this.username);
            this.username = null;
        }

        try {
            if (reader != null) {
                reader.close();
            }
        } catch (IOException ignored) {}

        if (writer != null) {
            writer.close();
        }

        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException ignored) {}
    }
}

