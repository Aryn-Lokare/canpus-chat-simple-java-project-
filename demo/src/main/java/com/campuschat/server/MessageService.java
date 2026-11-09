package com.campuschat.server;

import com.campuschat.model.OfflineMessage;
import org.hibernate.Session;
import org.hibernate.Transaction;

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

        
        for (String user : UserStore.allUsernames()) {
            if (!user.equalsIgnoreCase(sender) && !OnlineUsers.isOnline(user)) {
                queueOfflineMessage(user, sender, text);
            }
        }
    }

   
    public static void deliverMissed(String username, ClientHandler handler) {
        checkDatabaseHealth();
        handler.send("SYNC_START");
        if (username != null) {
            String key = username.trim().toLowerCase();

           
            if (HibernateUtil.isInitialized()) {
                try (Session session = HibernateUtil.openSession()) {
                    if (session != null) {
                        Transaction tx = session.beginTransaction();
                        List<OfflineMessage> dbMsgs = session.createQuery(
                                "FROM OfflineMessage WHERE LOWER(recipient) = :r ORDER BY createdAt ASC", OfflineMessage.class)
                                .setParameter("r", key)
                                .getResultList();

                        if (dbMsgs != null && !dbMsgs.isEmpty()) {
                            for (OfflineMessage om : dbMsgs) {
                                handler.send("MSG|" + om.getSender() + "|" + om.getContent());
                                session.remove(om);
                            }
                        }
                        tx.commit();
                      
                        offlineQueue.remove(key);
                        offlineQueue.remove(username);
                    }
                } catch (Exception e) {
                    System.err.println("[MessageService] Database error delivering missed messages: " + e.getMessage());
                }
            }

           
            List<String> missed = offlineQueue.remove(key);
            if (missed == null) {
                missed = offlineQueue.remove(username);
            }
            if (missed != null) {
                for (String msg : missed) {
                    handler.send(msg);
                }
            }
        }
        handler.send("SYNC_END");
    }

   
    public static void queueOfflineMessage(String username, String sender, String text) {
        if (username == null) return;
        String key = username.trim().toLowerCase();

       
        offlineQueue.computeIfAbsent(key, k -> new CopyOnWriteArrayList<>())
                    .add("MSG|" + sender + "|" + text);

        
        if (HibernateUtil.isInitialized()) {
            try (Session session = HibernateUtil.openSession()) {
                if (session != null) {
                    Transaction tx = session.beginTransaction();
                    session.persist(new OfflineMessage(key, sender, text));
                    tx.commit();
                }
            } catch (Exception e) {
                System.err.println("[MessageService] Database error persisting offline message: " + e.getMessage());
            }
        }
    }

 
    public static void clearQueue() {
        offlineQueue.clear();
        if (HibernateUtil.isInitialized()) {
            try (Session session = HibernateUtil.openSession()) {
                if (session != null) {
                    Transaction tx = session.beginTransaction();
                    session.createMutationQuery("DELETE FROM OfflineMessage").executeUpdate();
                    tx.commit();
                }
            } catch (Exception ignored) {}
        }
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
