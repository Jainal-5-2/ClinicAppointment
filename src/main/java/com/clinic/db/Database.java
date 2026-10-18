package com.clinic.db;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public final class Database {
    private static final Properties PROPS = new Properties();

    static {
        try (InputStream in = Database.class.getResourceAsStream("/db.properties")) {
            if (in != null) PROPS.load(in);
        } catch (IOException ignored) { }
    }

    private Database() { }

    public static Connection get() throws SQLException {
        return DriverManager.getConnection(
                PROPS.getProperty("db.url"),
                PROPS.getProperty("db.user", "root"),
                PROPS.getProperty("db.password", ""));
    }
}
