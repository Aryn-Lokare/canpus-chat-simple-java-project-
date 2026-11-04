package com.campuschat.server;

import java.io.File;

/**
 * Stub class representing Hibernate ORM initialization and shutdown.
 * Owned by Person 2 in final integration.
 */
public class HibernateUtil {

    private static volatile boolean initialized = false;

    private HibernateUtil() {}

    /**
     * Initializes the Hibernate SessionFactory from the specified configuration file.
     * Ensures data/ directory exists for the H2 database.
     */
    public static void init(String configFile) {
        System.out.println("[Hibernate] Initializing Hibernate ORM with config: " + configFile);

        // Ensure data/ folder exists for H2 database storage
        File dataDir = new File("data");
        if (!dataDir.exists()) {
            boolean created = dataDir.mkdirs();
            if (created) {
                System.out.println("[Hibernate] Created 'data/' directory for database files.");
            }
        }

        initialized = true;
        System.out.println("[Hibernate] SessionFactory built successfully.");
        System.out.println("[Hibernate] Connected to H2 database in data/ directory.");
    }

    /**
     * Closes the Hibernate SessionFactory cleanly.
     */
    public static void shutdown() {
        if (initialized) {
            System.out.println("[Hibernate] Closing Hibernate SessionFactory and database connection...");
            initialized = false;
            System.out.println("[Hibernate] Database closed cleanly.");
        }
    }

    public static boolean isInitialized() {
        return initialized;
    }
}

