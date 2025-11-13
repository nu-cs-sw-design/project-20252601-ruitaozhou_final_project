package datasource;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

/**
 * SQLiteDataSource - Singleton pattern
 * 管理 SQLite 数据库连接和 schema 初始化
 */
public class SQLiteDataSource {

    private static final String DB_URL = "jdbc:sqlite:wordminer.db";

    // Singleton instance
    private static SQLiteDataSource instance;

    // Private constructor to prevent instantiation
    private SQLiteDataSource() {
        initSchema();
    }

    /**
     * Get singleton instance (thread-safe)
     */
    public static synchronized SQLiteDataSource getInstance() {
        if (instance == null) {
            instance = new SQLiteDataSource();
        }
        return instance;
    }

    /**
     * Get a database connection
     */
    public Connection getConnection() {
        try {
            return DriverManager.getConnection(DB_URL);
        } catch (Exception e) {
            throw new RuntimeException("Unable to connect to database", e);
        }
    }

    /**
     * Initialize database schema
     */
    private void initSchema() {
        try (Connection conn = getConnection();
             Statement st = conn.createStatement()) {

            // Create articles table
            st.execute("""
                    CREATE TABLE IF NOT EXISTS articles (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        title TEXT NOT NULL,
                        content TEXT NOT NULL,
                        import_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    );
                    """);

        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize database schema", e);
        }
    }
}
