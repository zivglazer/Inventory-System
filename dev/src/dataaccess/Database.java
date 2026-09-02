package dataaccess;

import dataaccess.cache.BoundedCache;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Owns the single JDBC connection to the SQLite database and the schema lifecycle.
 *
 * <p>The DB file is the system of record. The connection URL is resolved from the system
 * property {@code adss.db.url} (default {@code jdbc:sqlite:adss.db}); tests pass
 * {@code -Dadss.db.url=jdbc:sqlite::memory:} for an isolated in-memory database.
 *
 * <p>DAOs obtain the shared connection via {@link #connection()} and register their
 * {@link BoundedCache} so {@link #reset()} can clear them together. Multi-table operations
 * should be wrapped in {@link #runInTransaction(Runnable)} for atomicity.
 */
public final class Database {

    private static final String DEFAULT_URL = "jdbc:sqlite:adss.db";

    private static String url = System.getProperty("adss.db.url", DEFAULT_URL);
    private static Connection conn;
    private static boolean schemaReady = false;
    private static final List<BoundedCache<?, ?>> caches = new ArrayList<>();

    private Database() {}

    /** Switch the target database (closes any open connection). Used by tests. */
    public static synchronized void configure(String newUrl) {
        close();
        url = newUrl;
        schemaReady = false;
    }

    /** The shared connection, opening it and creating the schema on first use. */
    public static synchronized Connection connection() {
        try {
            Connection c = openRaw();
            if (!schemaReady) createAll();
            return c;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to open database connection: " + url, e);
        }
    }

    private static Connection openRaw() throws SQLException {
        if (conn == null || conn.isClosed()) {
            conn = DriverManager.getConnection(url);
            try (Statement st = conn.createStatement()) {
                st.execute("PRAGMA foreign_keys = ON");
            }
        }
        return conn;
    }

    public static synchronized void registerCache(BoundedCache<?, ?> cache) {
        caches.add(cache);
    }

    public static synchronized void clearAllCaches() {
        for (BoundedCache<?, ?> cache : caches) cache.clear();
    }

    public static synchronized void createAll() {
        try {
            Connection c = openRaw();
            try (Statement st = c.createStatement()) {
                for (String ddl : Schema.CREATE) st.execute(ddl);
            }
            schemaReady = true;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to create schema", e);
        }
    }

    public static synchronized void dropAll() {
        try {
            Connection c = openRaw();
            try (Statement st = c.createStatement()) {
                st.execute("PRAGMA foreign_keys = OFF");
                for (String table : Schema.TABLES_CHILD_FIRST)
                    st.execute("DROP TABLE IF EXISTS " + table);
                st.execute("PRAGMA foreign_keys = ON");
            }
            schemaReady = false;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to drop schema", e);
        }
    }

    /** Blank-slate the whole database and caches (drops every table). */
    public static synchronized void reset() {
        dropAll();
        createAll();
        clearAllCaches();
    }

    /**
     * Delete all rows from the given tables (pass them child-first so foreign keys hold).
     * Used by a single module's {@code resetAll()} so it wipes only its own data and leaves
     * sibling modules (and their seeded rows) intact.
     */
    public static synchronized void clearTables(List<String> tablesChildFirst) {
        Connection c = connection();
        try (Statement st = c.createStatement()) {
            for (String table : tablesChildFirst) st.execute("DELETE FROM " + table);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to clear tables", e);
        }
    }

    /**
     * Run a unit of work atomically. On any RuntimeException the work is rolled back and the
     * caches cleared (so no stale write survives), then the exception is rethrown.
     */
    public static synchronized void runInTransaction(Runnable action) {
        Connection c = connection();
        try {
            c.setAutoCommit(false);
            action.run();
            c.commit();
        } catch (RuntimeException | SQLException e) {
            try { c.rollback(); } catch (SQLException ignored) {}
            clearAllCaches();
            throw (e instanceof RuntimeException) ? (RuntimeException) e
                                                  : new RuntimeException(e);
        } finally {
            try { c.setAutoCommit(true); } catch (SQLException ignored) {}
        }
    }

    public static synchronized void close() {
        try {
            if (conn != null && !conn.isClosed()) conn.close();
        } catch (SQLException ignored) {
        } finally {
            conn = null;
            schemaReady = false;
        }
    }
}
