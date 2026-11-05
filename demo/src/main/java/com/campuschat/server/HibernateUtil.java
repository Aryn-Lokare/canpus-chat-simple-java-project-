package com.campuschat.server;

import com.campuschat.model.OfflineMessage;
import com.campuschat.model.User;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;

import java.io.File;
import java.net.URL;

/**
 * Manages the Hibernate SessionFactory and database connection lifecycle.
 */
public class HibernateUtil {

    private static SessionFactory sessionFactory;
    private static StandardServiceRegistry registry;
    private static volatile boolean initialized = false;

    private HibernateUtil() {}

    /**
     * Initializes Hibernate ORM using the specified configuration file.
     * Ensures local data/ directory exists.
     */
    public static synchronized void init(String configFile) {
        System.out.println("[Hibernate] Initializing Hibernate ORM with config: " + configFile);

        // Ensure data/ directory exists for test assertions and local file storage
        File dataDir = new File("data");
        if (!dataDir.exists()) {
            boolean created = dataDir.mkdirs();
            if (created) {
                System.out.println("[Hibernate] Created 'data/' directory.");
            }
        }

        try {
            StandardServiceRegistryBuilder builder = new StandardServiceRegistryBuilder();

            String configName = (configFile != null && !configFile.trim().isEmpty())
                    ? configFile.trim()
                    : "hibernate.cfg.xml";

            // 1. Try resolving via ClassLoader (standard for classpath resources like hibernate.cfg.xml)
            ClassLoader classLoader = HibernateUtil.class.getClassLoader();
            URL configUrl = classLoader.getResource(configName);
            if (configUrl == null && !configName.startsWith("/")) {
                configUrl = classLoader.getResource("/" + configName);
            }

            // 2. Try resolving as direct file path or within standard project/source directories
            File cfgFile = null;
            File[] candidateFiles = new File[] {
                new File(configName),
                new File("src/main/resources/" + configName),
                new File("demo/src/main/resources/" + configName),
                new File("demo/" + configName)
            };
            for (File candidate : candidateFiles) {
                if (candidate.exists() && candidate.isFile()) {
                    cfgFile = candidate;
                    break;
                }
            }

            // 3. Configure the StandardServiceRegistryBuilder
            if (configUrl != null) {
                builder.configure(configUrl);
            } else if (cfgFile != null) {
                builder.configure(cfgFile);
            } else {
                builder.configure(configName);
            }

            registry = builder.build();
            sessionFactory = new MetadataSources(registry)
                    .addAnnotatedClass(User.class)
                    .addAnnotatedClass(OfflineMessage.class)
                    .buildMetadata()
                    .buildSessionFactory();

            initialized = true;
            System.out.println("[Hibernate] SessionFactory built successfully.");
            System.out.println("[Hibernate] Connected to MySQL database (campusv1).");

            // Populate initial default users in MySQL database if empty
            UserStore.seedDefaultsIfEmpty();

        } catch (Exception e) {
            System.err.println("[Hibernate] Error building SessionFactory: " + e.getMessage());
            e.printStackTrace();
            if (registry != null) {
                StandardServiceRegistryBuilder.destroy(registry);
                registry = null;
            }
            sessionFactory = null;
            initialized = false;
        }
    }

    /**
     * Closes the Hibernate SessionFactory cleanly.
     */
    public static synchronized void shutdown() {
        if (sessionFactory != null && !sessionFactory.isClosed()) {
            System.out.println("[Hibernate] Closing Hibernate SessionFactory and database connection...");
            sessionFactory.close();
            sessionFactory = null;
        }
        if (registry != null) {
            StandardServiceRegistryBuilder.destroy(registry);
            registry = null;
        }
        initialized = false;
        System.out.println("[Hibernate] Database closed cleanly.");
    }

    public static SessionFactory getSessionFactory() {
        return sessionFactory;
    }

    /**
     * Opens a new Hibernate Session if initialized, or returns null.
     */
    public static Session openSession() {
        if (sessionFactory != null && !sessionFactory.isClosed()) {
            return sessionFactory.openSession();
        }
        return null;
    }

    public static boolean isInitialized() {
        return initialized;
    }
}
