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


public class HibernateUtil {

    private static SessionFactory sessionFactory;
    private static StandardServiceRegistry registry;
    private static volatile boolean initialized = false;

    private HibernateUtil() {}

    
    public static synchronized void init(String configFile) {
        System.out.println("[Hibernate] Initializing Hibernate ORM with config: " + configFile);

      
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

           
            ClassLoader classLoader = HibernateUtil.class.getClassLoader();
            URL configUrl = classLoader.getResource(configName);
            if (configUrl == null && !configName.startsWith("/")) {
                configUrl = classLoader.getResource("/" + configName);
            }

           
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
