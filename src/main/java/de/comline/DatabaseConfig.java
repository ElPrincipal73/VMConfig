package de.comline;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

@Configuration
public class DatabaseConfig {

    @Value("${db.url}")
    private String DB_URL;

    @Value("${db.user}")
    private String USER;

    @Value("${db.pass}")
    private String PASS;

    @Bean
    public Connection databaseConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, USER, PASS);
    }
}
