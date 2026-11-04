package com.campuschat.server;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

public class CampusChatServerTest {

    private static final int TEST_PORT = 18080;
    private Server server;
    private Thread serverThread;

    @BeforeEach
    public void setUp() throws Exception {
        OnlineUsers.clear();
        MessageService.clearQueue();
        UserStore.setSimulateDatabaseError(false);
        MessageService.setSimulateDatabaseError(false);

        server = new Server(TEST_PORT);
        serverThread = new Thread(() -> {
            try {
                server.start();
            } catch (IOException e) {
                // Stopped during tearDown
            }
        });
        serverThread.start();

        // Give server socket a moment to bind and listen
        Thread.sleep(150);
    }

    @AfterEach
    public void tearDown() throws Exception {
        if (server != null) {
            server.stop();
        }
        if (serverThread != null) {
            serverThread.join(1000);
        }
        OnlineUsers.clear();
    }

    private TestClient connectClient() throws IOException {
        return new TestClient("127.0.0.1", TEST_PORT);
    }

    @Test
    public void testOnlineUsersRegistry() {
        assertFalse(OnlineUsers.isOnline("alice"));
        assertNull(OnlineUsers.get("alice"));

        ClientHandler handler = new ClientHandler(null);
        OnlineUsers.add("alice", handler);

        assertTrue(OnlineUsers.isOnline("alice"));
        assertSame(handler, OnlineUsers.get("alice"));

        OnlineUsers.remove("alice");
        assertFalse(OnlineUsers.isOnline("alice"));
        assertNull(OnlineUsers.get("alice"));
    }

    @Test
    public void testUnknownCommandGivesError() throws Exception {
        try (TestClient client = connectClient()) {
            client.send("FOOBAR");
            String reply = client.readLine();
            assertEquals("ERROR|Unknown command", reply);
        }
    }

    @Test
    public void testLoginNonExistentUserFails() throws Exception {
        try (TestClient client = connectClient()) {
            client.send("LOGIN|unknown_user_999");
            String reply = client.readLine();
            assertEquals("ERROR|No such user", reply);
            assertFalse(OnlineUsers.isOnline("unknown_user_999"));
        }
    }

    @Test
    public void testLoginSuccessAndMissedDelivery() throws Exception {
        // Enqueue an offline message for alice
        MessageService.queueOfflineMessage("alice", "bob", "Hey Alice, welcome back!");

        try (TestClient client = connectClient()) {
            client.send("LOGIN|alice");
            assertEquals("OK|Welcome alice", client.readLine());
            assertEquals("SYNC_START", client.readLine());
            assertEquals("MSG|bob|Hey Alice, welcome back!", client.readLine());
            assertEquals("SYNC_END", client.readLine());

            assertTrue(OnlineUsers.isOnline("alice"));
        }
    }

    @Test
    public void testDuplicateLoginRejected() throws Exception {
        try (TestClient client1 = connectClient();
             TestClient client2 = connectClient()) {

            client1.send("LOGIN|alice");
            assertEquals("OK|Welcome alice", client1.readLine());
            assertEquals("SYNC_START", client1.readLine());
            assertEquals("SYNC_END", client1.readLine());

            // Second client tries to log in as alice
            client2.send("LOGIN|alice");
            assertEquals("ERROR|Already logged in", client2.readLine());
        }
    }

    @Test
    public void testChatWhenNotLoggedInIsRejected() throws Exception {
        try (TestClient client = connectClient()) {
            client.send("CHAT|Hello before login");
            assertEquals("ERROR|Not logged in", client.readLine());
        }
    }

    @Test
    public void testTwoClientsCanConnectAndChatConcurrently() throws Exception {
        try (TestClient alice = connectClient();
             TestClient bob = connectClient()) {

            alice.send("LOGIN|alice");
            assertEquals("OK|Welcome alice", alice.readLine());
            assertEquals("SYNC_START", alice.readLine());
            assertEquals("SYNC_END", alice.readLine());

            bob.send("LOGIN|bob");
            assertEquals("OK|Welcome bob", bob.readLine());
            assertEquals("SYNC_START", bob.readLine());
            assertEquals("SYNC_END", bob.readLine());

            assertTrue(OnlineUsers.isOnline("alice"));
            assertTrue(OnlineUsers.isOnline("bob"));

            // Alice sends a chat message
            alice.send("CHAT|Hello everyone!");

            // Both Alice and Bob should receive it
            assertEquals("MSG|alice|Hello everyone!", alice.readLine());
            assertEquals("MSG|alice|Hello everyone!", bob.readLine());
        }
    }

    @Test
    public void testClosingClientWindowRemovesItFromOnlineUsers() throws Exception {
        TestClient alice = connectClient();
        alice.send("LOGIN|alice");
        assertEquals("OK|Welcome alice", alice.readLine());
        assertEquals("SYNC_START", alice.readLine());
        assertEquals("SYNC_END", alice.readLine());

        assertTrue(OnlineUsers.isOnline("alice"));

        // Close alice's client connection
        alice.close();

        // Wait a short duration for server to detect EOF and execute finally block
        boolean removed = false;
        for (int i = 0; i < 20; i++) {
            Thread.sleep(50);
            if (!OnlineUsers.isOnline("alice")) {
                removed = true;
                break;
            }
        }
        assertTrue(removed, "User alice should be removed from OnlineUsers upon disconnect");
    }

    @Test
    public void testServerKeepsRunningAfterClientCrash() throws Exception {
        // Crash client 1 abruptly
        TestClient crasher = connectClient();
        crasher.send("LOGIN|alice");
        assertEquals("OK|Welcome alice", crasher.readLine());
        assertEquals("SYNC_START", crasher.readLine());
        assertEquals("SYNC_END", crasher.readLine());
        crasher.socket.close(); // Abrupt drop

        // Ensure server continues to accept new client
        try (TestClient normalClient = connectClient()) {
            normalClient.send("LOGIN|bob");
            assertEquals("OK|Welcome bob", normalClient.readLine());
            assertEquals("SYNC_START", normalClient.readLine());
            assertEquals("SYNC_END", normalClient.readLine());
            assertTrue(OnlineUsers.isOnline("bob"));
        }
    }

    @Test
    public void testDatabaseErrorGivesErrorReplyAndDoesNotCrashServer() throws Exception {
        try (TestClient client = connectClient()) {
            // Trigger simulated database error in UserStore
            UserStore.setSimulateDatabaseError(true);

            client.send("LOGIN|alice");
            assertEquals("ERROR|Server problem, please try again", client.readLine());

            // Reset error
            UserStore.setSimulateDatabaseError(false);

            // Server continues running and client can retry
            client.send("LOGIN|alice");
            assertEquals("OK|Welcome alice", client.readLine());
            assertEquals("SYNC_START", client.readLine());
            assertEquals("SYNC_END", client.readLine());
        }
    }

    @Test
    public void testHibernateStartupCreatesDataFolderAndCleanShutdown() {
        HibernateUtil.init("hibernate.cfg.xml");
        assertTrue(HibernateUtil.isInitialized());

        File dataDir = new File("data");
        assertTrue(dataDir.exists() && dataDir.isDirectory(), "data/ directory should exist");

        HibernateUtil.shutdown();
        assertFalse(HibernateUtil.isInitialized());
    }

    /**
     * Helper client class for tests.
     */
    private static class TestClient implements AutoCloseable {
        private final Socket socket;
        private final BufferedReader reader;
        private final PrintWriter writer;

        TestClient(String host, int port) throws IOException {
            this.socket = new Socket(host, port);
            this.socket.setSoTimeout(3000);
            this.reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            this.writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
        }

        void send(String message) {
            writer.println(message);
        }

        String readLine() throws IOException {
            return reader.readLine();
        }

        @Override
        public void close() {
            try {
                socket.close();
            } catch (IOException ignored) {}
        }
    }
}

