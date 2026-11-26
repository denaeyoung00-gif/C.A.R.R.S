package carrs.repository;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/** Reads the connection settings from db.properties so no password is stored in the code. */
public class Database {
    private static final Properties SETTINGS = load();

    private static Properties load() {
        Properties p = new Properties();
        try (FileInputStream in = new FileInputStream("db.properties")) {
            p.load(in);
        } catch (IOException e) {
            throw new RuntimeException("db.properties not found in the project folder. "
                    + "Copy db.properties.example to db.properties and fill in your password.", e);
        }
        return p;
    }

    public static Connection connect() throws SQLException {
        return DriverManager.getConnection(SETTINGS.getProperty("url"),
                SETTINGS.getProperty("user"), SETTINGS.getProperty("password"));
    }
}