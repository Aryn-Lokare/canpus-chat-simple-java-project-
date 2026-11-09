package com.campuschat.server;

import com.campuschat.model.User;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.*;


public class UserStore {

    private static final Set<String> registeredUsers =
            Collections.synchronizedSet(new LinkedHashSet<>());
    private static volatile boolean simulateDatabaseError = false;

    static {
        for (String name : new String[]{"alice", "bob", "charlie", "aryan"}) {
            registeredUsers.add(name);
        }
    }

    private UserStore() {}

    
    private static String clean(String name) {
        return name == null ? "" : name.trim().toLowerCase();
    }

    
    public static void seedDefaultsIfEmpty() {
        if (!HibernateUtil.isInitialized()) {
            return;
        }
        try (Session session = HibernateUtil.openSession()) {
            if (session != null) {
                Transaction tx = session.beginTransaction();
                for (String name : new String[]{"alice", "bob", "charlie", "aryan"}) {
                    Long count = session.createQuery(
                            "SELECT COUNT(u) FROM User u WHERE LOWER(u.username) = :u", Long.class)
                            .setParameter("u", name)
                            .getSingleResult();
                    if (count == 0) {
                        session.persist(new User(name));
                    }
                }
                tx.commit();
            }
        } catch (Exception e) {
            System.err.println("[UserStore] Error seeding default users in database: " + e.getMessage());
        }
    }

    
    public static boolean exists(String username) {
        checkDatabaseHealth();
        String name = clean(username);
        if (name.isEmpty()) {
            return false;
        }

        if (HibernateUtil.isInitialized()) {
            try (Session session = HibernateUtil.openSession()) {
                if (session != null) {
                    Long count = session.createQuery(
                            "SELECT COUNT(u) FROM User u WHERE LOWER(u.username) = :u", Long.class)
                            .setParameter("u", name)
                            .getSingleResult();
                    return count > 0;
                }
            } catch (Exception e) {
                System.err.println("[UserStore] Database error checking user existence: " + e.getMessage());
                throw new RuntimeException("Database error in UserStore", e);
            }
        }

        return registeredUsers.contains(name);
    }

    
    public static List<String> allUsernames() {
        checkDatabaseHealth();

        if (HibernateUtil.isInitialized()) {
            try (Session session = HibernateUtil.openSession()) {
                if (session != null) {
                    List<String> dbUsers = session.createQuery(
                            "SELECT LOWER(u.username) FROM User u", String.class)
                            .getResultList();
                    if (dbUsers != null && !dbUsers.isEmpty()) {
                        return new ArrayList<>(dbUsers);
                    }
                }
            } catch (Exception e) {
                System.err.println("[UserStore] Database error fetching all usernames: " + e.getMessage());
                throw new RuntimeException("Database error in UserStore", e);
            }
        }

        synchronized (registeredUsers) {
            return new ArrayList<>(registeredUsers);
        }
    }

    
    public static void addUser(String username) {
        String name = clean(username);
        if (name.isEmpty()) {
            return;
        }

        synchronized (registeredUsers) {
            registeredUsers.add(name);
        }

        if (HibernateUtil.isInitialized()) {
            try (Session session = HibernateUtil.openSession()) {
                if (session != null) {
                    Transaction tx = session.beginTransaction();
                    Long count = session.createQuery(
                            "SELECT COUNT(u) FROM User u WHERE LOWER(u.username) = :u", Long.class)
                            .setParameter("u", name)
                            .getSingleResult();
                    if (count == 0) {
                        session.persist(new User(name));
                    }
                    tx.commit();
                }
            } catch (Exception e) {
                System.err.println("[UserStore] Database error adding user '" + name + "': " + e.getMessage());
            }
        }
    }

    
    public static void removeUser(String username) {
        String name = clean(username);
        synchronized (registeredUsers) {
            registeredUsers.remove(name);
        }

        if (HibernateUtil.isInitialized()) {
            try (Session session = HibernateUtil.openSession()) {
                if (session != null) {
                    Transaction tx = session.beginTransaction();
                    session.createMutationQuery(
                            "DELETE FROM User u WHERE LOWER(u.username) = :u")
                            .setParameter("u", name)
                            .executeUpdate();
                    tx.commit();
                }
            } catch (Exception e) {
                System.err.println("[UserStore] Database error removing user '" + name + "': " + e.getMessage());
            }
        }
    }

    public static void setSimulateDatabaseError(boolean throwError) {
        simulateDatabaseError = throwError;
    }

    private static void checkDatabaseHealth() {
        if (simulateDatabaseError) {
            throw new RuntimeException("Simulated database failure in UserStore");
        }
    }
}
