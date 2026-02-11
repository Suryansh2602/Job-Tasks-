package com.info.util;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class GetConnection {
    private Connection connection;

    private static final String url = "jdbc:postgresql://localhost:5432/users";
    private static final String user = "postgres";
    private static final String password = "root";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }
}