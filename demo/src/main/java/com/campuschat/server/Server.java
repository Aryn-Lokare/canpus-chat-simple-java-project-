package com.campuschat.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

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

    
    public void start() throws IOException {
        serverSocket = new ServerSocket(port);
        running = true;
        System.out.println("Campus Chat Server started and listening on port " + port + "...");

        while (running && !serverSocket.isClosed()) {
            try {
                
                Socket clientSocket = serverSocket.accept();
                System.out.println("[Server] Accepted connection from " + clientSocket.getRemoteSocketAddress());

                
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

        
        HibernateUtil.init("hibernate.cfg.xml");

        
        Runtime.getRuntime().addShutdownHook(new Thread(HibernateUtil::shutdown));

        Server server = new Server(port);
        try {
            server.start();
        } catch (IOException e) {
            System.err.println("[Server] Fatal error: Could not start server on port " + port + ": " + e.getMessage());
        }
    }
}

