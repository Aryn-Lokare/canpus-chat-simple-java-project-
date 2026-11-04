package com.campuschat.client;

import java.io.*;
import java.net.Socket;
import java.util.Scanner;

public class ConsoleClient {

    public static void main(String[] args) {
        try (Socket socket = new Socket("localhost", 8080);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             Scanner keyboard = new Scanner(System.in)) {

            // Thread 2: print everything the server sends
            Thread reader = new Thread(() -> {
                try {
                    String line;
                    while ((line = in.readLine()) != null) {
                        System.out.println(format(line));
                    }
                } catch (IOException e) {
                    // socket closed, just stop
                }
            });
            reader.setDaemon(true);
            reader.start();

            // Main thread: read keyboard and send to server
            System.out.print("Username: ");
            out.println("LOGIN|" + keyboard.nextLine().trim());
            System.out.println("(type /login name to try another user, /quit to exit)");

            while (keyboard.hasNextLine()) {
                String text = keyboard.nextLine().trim();
                if (text.isEmpty()) continue;
                if (text.equals("/quit")) {
                    out.println("LOGOUT");
                    break;
                } else if (text.startsWith("/login ")) {
                    out.println("LOGIN|" + text.substring(7).trim());
                } else {
                    out.println("CHAT|" + text);
                }
            }
        } catch (IOException e) {
            System.out.println("Cannot connect. Is the server running on port 8080?");
        }
    }

    // Turn protocol lines into readable text
    static String format(String line) {
        if (line.startsWith("MSG|")) {
            String[] p = line.split("\\|", 3);
            if (p.length == 3) return p[1] + ": " + p[2];
        }
        if (line.equals("SYNC_START")) return "--- missed messages ---";
        if (line.equals("SYNC_END")) return "--- end ---";
        if (line.startsWith("ERROR|")) return "Error: " + line.substring(6);
        if (line.startsWith("OK|")) return line.substring(3);
        return line;
    }
}
