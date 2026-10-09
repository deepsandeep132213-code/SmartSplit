package com.smartsplit.database;

import com.smartsplit.exception.DatabaseException;
import com.smartsplit.util.EnvConfig;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Objects;

/** Opens JDBC connections. Credentials come from EnvConfig, never from source code. */
public class DBConnection {

    public static final String DEFAULT_URL = "jdbc:mysql://localhost:3306/smartsplit_db";

    private final String url;
    private final String username;
    private final String password;

    public DBConnection(String url, String username, String password) {
        this.url = Objects.requireNonNull(url);
        this.username = Objects.requireNonNull(username);
        this.password = Objects.requireNonNull(password);
    }

    public static DBConnection fromConfig(EnvConfig config) throws DatabaseException {
        String username = config.get("DB_USERNAME").orElseThrow(() ->
                new DatabaseException("DB_USERNAME is not configured. Set it in your environment or .env file."));
        return new DBConnection(
                config.get("DB_URL").orElse(DEFAULT_URL),
                username,
                config.get("DB_PASSWORD").orElse(""));
    }

    public Connection getConnection() throws DatabaseException {
        try {
            return DriverManager.getConnection(url, username, password);
        } catch (SQLException e) {
            throw new DatabaseException("Could not connect to the database. Check MySQL is running and credentials are correct.", e);
        }
    }
}
