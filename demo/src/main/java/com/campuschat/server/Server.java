package com.campuschat.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Main server class for Campus Chat.
 * Accepts incoming socket connections on port 8080 and spins off a ClientHandler thread for each client.
 */
public class Server {

    public static final int DEFAULT_PORT = 8080;

    private final int port;
    private ServerSocket serverSocket;
    private volatile boolean running = false;

    public Server() {
        this(DEFAULT_PORT);
    }

    public Server(int port) {
        this.port = port;
    }

    /**
     * Starts listening for client connections.
     */
    public void start() throws IOException {
        serverSocket = new ServerSocket(port);
        running = true;
        System.out.println("Campus Chat Server started and listening on port " + port + "...");

        while (running && !serverSocket.isClosed()) {
            try {
                // In a loop, call accept()
                Socket clientSocket = serverSocket.accept();
                System.out.println("[Server] Accepted connection from " + clientSocket.getRemoteSocketAddress());

                // For each client, create a ClientHandler and start it in a new Thread
                ClientHandler handler = new ClientHandler(clientSocket);
                Thread clientThread = new Thread(handler);
                clientThread.start();
            } catch (IOException e) {
                if (!running || serverSocket.isClosed()) {
                    System.out.println("[Server] Server stopped listening.");
                    break;
                }
                System.err.println("[Server] Error accepting client: " + e.getMessage());
            }
        }
    }

    /**
     * Stops the server by closing the ServerSocket.
     */
    public void stop() {
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            System.err.println("[Server] Error closing server socket: " + e.getMessage());
        }
    }

    public boolean isRunning() {
        return running;
    }

    public int getPort() {
        return port;
    }

    public static void main(String[] args) {
        int port = DEFAULT_PORT;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.out.println("Invalid port specified. Using default: " + DEFAULT_PORT);
            }
        }

        // Section 11.1: Call HibernateUtil.init("hibernate.cfg.xml") before the accept() loop.
        // Then a wrong database setting shows up at startup, not when the first user logs in.
        HibernateUtil.init("hibernate.cfg.xml");

        // Section 11.1: Register a shutdown hook so the database closes cleanly when the server stops
        Runtime.getRuntime().addShutdownHook(new Thread(HibernateUtil::shutdown));

        Server server = new Server(port);
        try {
            server.start();
        } catch (IOException e) {
            System.err.println("[Server] Fatal error: Could not start server on port " + port + ": " + e.getMessage());
        }
    }
}

