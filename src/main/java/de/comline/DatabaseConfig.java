package de.comline;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Logger;

@Configuration
public class DatabaseConfig {

    @Value("${db.url}")
    private String dbUrl;

    @Value("${db.user}")
    private String user;

    @Value("${db.pass}")
    private String pass;

    public String getDbUrl() {
        return dbUrl;
    }

    public String getUser() {
        return user;
    }

    public String getPass() {
        return pass;
    }
    private static final Logger logger = AppLogger.getLogger(DatabaseConfig.class.getName());

    @Bean
    public Connection databaseConnection() {
        try {
            return DriverManager.getConnection(dbUrl, user, pass);
        } catch (SQLException e) {
            logger.severe("Datenbankverbindung fehlgeschlagen,Logdaten überprüfen: " + e.getMessage());
            throw new RuntimeException("Datenbankverbindung fehlgeschlagen", e);
        }
    }
}

