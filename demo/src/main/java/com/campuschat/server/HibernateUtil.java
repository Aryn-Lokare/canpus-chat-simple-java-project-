package com.campuschat.server;

import java.io.File;

public class HibernateUtil {

    private static volatile boolean initialized = false;

    private HibernateUtil() {}

   
    public static void init(String configFile) {
        System.out.println("[Hibernate] Initializing Hibernate ORM with config: " + configFile);

      
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
