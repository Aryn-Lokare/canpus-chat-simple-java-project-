package com.campuschat.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * Handles communication with a connected client.
 * Runs in its own thread to ensure non-blocking concurrent client handling.
 */
public class ClientHandler implements Runnable {

    private final Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;
    private String username; // null until logged in

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    /**
     * Gets the username associated with this handler, or null if not yet logged in.
     */
    public String getUsername() {
        return username;
    }

    /**
     * Writes one line to that client.
     * Synchronized to prevent thread interleaving when multiple threads
     * (e.g. broadcasting other users' chat messages) call send() concurrently.
     */
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

                // Step 2: Split each line with line.split("\\|", 2)
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
                    // Section 11.3: If a database/service call throws, reply ERROR|Server problem, please try again
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
            // Section 6: Never let one broken client crash the server. Catch IOException inside run().
            System.out.println("[Server] Client disconnected or socket error: " + e.getMessage());
        } finally {
            // Step 5: On LOGOUT or when the socket closes or throws: remove the user from OnlineUsers, close the socket.
            // Use try/finally so a crashed client never stays listed as online.
            cleanup();
        }
    }

    /**
     * Step 4: Login flow.
     */
    private void handleLogin(String name) {
        String trimmedName = name.trim();
        if (trimmedName.isEmpty()) {
            send("ERROR|No such user");
            return;
        }

        // If client on this connection is already logged in
        if (this.username != null) {
            send("ERROR|Already logged in");
            return;
        }

        // 1. Check UserStore.exists(name). If not, reply ERROR|No such user
        if (!UserStore.exists(trimmedName)) {
            send("ERROR|No such user");
            return;
        }

        // 2. If already online, reply ERROR|Already logged in
        if (OnlineUsers.isOnline(trimmedName)) {
            send("ERROR|Already logged in");
            return;
        }

        // 3. Add to OnlineUsers, reply OK|Welcome name
        this.username = trimmedName;
        OnlineUsers.add(this.username, this);
        send("OK|Welcome " + this.username);

        // 4. Call MessageService.deliverMissed(name, this) so missed messages arrive
        MessageService.deliverMissed(this.username, this);
    }

    /**
     * Step 2 & 5: Chat flow.
     */
    private void handleChat(String text) {
        // Reject CHAT if the user is not logged in
        if (this.username == null) {
            send("ERROR|Not logged in");
            return;
        }

        // On CHAT|text : call MessageService.sendChat(username, text)
        MessageService.sendChat(this.username, text);
    }

    /**
     * Step 5: Logout flow.
     */
    private void handleLogout() {
        if (this.username != null) {
            OnlineUsers.remove(this.username);
            this.username = null;
        }
        send("OK|Logged out");
    }

    /**
     * Cleans up resources and unregisters user if registered.
     */
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
