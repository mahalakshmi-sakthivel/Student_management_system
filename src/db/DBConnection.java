package db;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.Properties;

/**
 * Handles database connectivity and initialization for the Student Management System.
 */
public class DBConnection {

    private static final String CONFIG_FILE = "db.properties";

    private static String host = "localhost";
    private static int port = 3306;
    private static String dbName = "student_db";
    private static String user = "root";
    private static String password = "root";
    private static boolean useSSL = false;
    private static boolean allowPublicKeyRetrieval = true;
    private static String serverTimezone = "UTC";

    private static String lastError = null;

    static {
        loadConfig();
    }

    public static synchronized void loadConfig() {
        File file = new File(CONFIG_FILE);
        if (file.exists()) {
            Properties props = new Properties();
            try (FileInputStream fis = new FileInputStream(file)) {
                props.load(fis);
                host = props.getProperty("db.host", host).trim();
                try {
                    port = Integer.parseInt(props.getProperty("db.port", String.valueOf(port)).trim());
                } catch (NumberFormatException ignored) {}
                dbName = props.getProperty("db.name", dbName).trim();
                user = props.getProperty("db.user", user).trim();
                password = props.getProperty("db.password", password).trim();
                useSSL = Boolean.parseBoolean(props.getProperty("db.useSSL", String.valueOf(useSSL)).trim());
                allowPublicKeyRetrieval = Boolean.parseBoolean(
                        props.getProperty("db.allowPublicKeyRetrieval", String.valueOf(allowPublicKeyRetrieval)).trim());
                serverTimezone = props.getProperty("db.serverTimezone", serverTimezone).trim();
            } catch (IOException e) {
                System.err.println("Could not load db.properties: " + e.getMessage());
            }
        }
    }

    public static synchronized void saveConfig(String newHost, int newPort, String newDbName, String newUser, String newPass) {
        host = newHost;
        port = newPort;
        dbName = newDbName;
        user = newUser;
        password = newPass;

        Properties props = new Properties();
        props.setProperty("db.host", host);
        props.setProperty("db.port", String.valueOf(port));
        props.setProperty("db.name", dbName);
        props.setProperty("db.user", user);
        props.setProperty("db.password", password);
        props.setProperty("db.useSSL", String.valueOf(useSSL));
        props.setProperty("db.allowPublicKeyRetrieval", String.valueOf(allowPublicKeyRetrieval));
        props.setProperty("db.serverTimezone", serverTimezone);

        try (FileOutputStream fos = new FileOutputStream(CONFIG_FILE)) {
            props.store(fos, "Student Management System - DB Configuration");
        } catch (IOException e) {
            System.err.println("Could not save db.properties: " + e.getMessage());
        }
    }

    private static String buildUrl(boolean includeDb) {
        StringBuilder sb = new StringBuilder("jdbc:mysql://");
        sb.append(host).append(":").append(port);
        if (includeDb && dbName != null && !dbName.isEmpty()) {
            sb.append("/").append(dbName);
        }
        sb.append("?useSSL=").append(useSSL);
        sb.append("&allowPublicKeyRetrieval=").append(allowPublicKeyRetrieval);
        sb.append("&serverTimezone=").append(serverTimezone);
        sb.append("&connectTimeout=3000&socketTimeout=60000");
        return sb.toString();
    }

    /**
     * Obtains a connection to the student_db database.
     */
    public static Connection getConnection() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            lastError = "MySQL JDBC Driver not found in classpath: " + e.getMessage();
            throw new SQLException(lastError, e);
        }

        try {
            Connection conn = DriverManager.getConnection(buildUrl(true), user, password);
            lastError = null;
            return conn;
        } catch (SQLException e) {
            lastError = e.getMessage();
            throw e;
        }
    }

    /**
     * Obtains a connection to the MySQL server without selecting a database.
     */
    public static Connection getServerConnection() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            lastError = "MySQL JDBC Driver not found: " + e.getMessage();
            throw new SQLException(lastError, e);
        }
        return DriverManager.getConnection(buildUrl(false), user, password);
    }

    /**
     * Tests whether the database server is reachable and credentials are valid.
     */
    public static boolean testConnection() {
        try (Connection conn = getConnection()) {
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            // Also try server level connection if database doesn't exist yet
            try (Connection conn = getServerConnection()) {
                return conn != null && !conn.isClosed();
            } catch (SQLException ex) {
                lastError = ex.getMessage();
                return false;
            }
        }
    }

    /**
     * Initializes database and tables automatically if they do not exist.
     */
    public static boolean initializeDatabase() {
        try (Connection conn = getServerConnection();
             Statement stmt = conn.createStatement()) {

            stmt.executeUpdate("CREATE DATABASE IF NOT EXISTS " + dbName);
            stmt.executeUpdate("USE " + dbName);

            // Create departments
            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS departments (" +
                "    department_id   INT PRIMARY KEY AUTO_INCREMENT," +
                "    department_name VARCHAR(100) NOT NULL UNIQUE" +
                ")"
            );

            // Create courses
            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS courses (" +
                "    course_id     INT PRIMARY KEY AUTO_INCREMENT," +
                "    course_name   VARCHAR(100) NOT NULL," +
                "    department_id INT," +
                "    FOREIGN KEY (department_id) REFERENCES departments(department_id) ON DELETE SET NULL" +
                ")"
            );

            // Create students
            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS students (" +
                "    student_id    INT PRIMARY KEY," +
                "    student_name  VARCHAR(100) NOT NULL," +
                "    date_of_birth DATE NOT NULL," +
                "    gender        VARCHAR(10) NOT NULL," +
                "    email         VARCHAR(100) NOT NULL UNIQUE," +
                "    phone         VARCHAR(15) NOT NULL," +
                "    department_id INT," +
                "    FOREIGN KEY (department_id) REFERENCES departments(department_id) ON DELETE RESTRICT" +
                ")"
            );

            // Create admin_users
            stmt.executeUpdate(
                "CREATE TABLE IF NOT EXISTS admin_users (" +
                "    admin_id      INT PRIMARY KEY AUTO_INCREMENT," +
                "    username      VARCHAR(50) NOT NULL UNIQUE," +
                "    password_hash VARCHAR(255) NOT NULL" +
                ")"
            );

            // Seed default departments if empty
            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM departments")) {
                if (rs.next() && rs.getInt(1) == 0) {
                    stmt.executeUpdate(
                        "INSERT INTO departments (department_name) VALUES " +
                        "('Computer Science'), ('Information Technology'), " +
                        "('Electronics'), ('Mechanical'), ('Civil Engineering')"
                    );
                }
            }

            // Seed default admin if empty (admin / admin123)
            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM admin_users")) {
                if (rs.next() && rs.getInt(1) == 0) {
                    stmt.executeUpdate(
                        "INSERT INTO admin_users (username, password_hash) VALUES " +
                        "('admin', '240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9')"
                    );
                }
            }

            // Seed sample students if empty
            try (ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM students")) {
                if (rs.next() && rs.getInt(1) == 0) {
                    stmt.executeUpdate(
                        "INSERT INTO students (student_id, student_name, date_of_birth, gender, email, phone, department_id) VALUES " +
                        "(1001, 'Aarav Sharma',     '2003-05-14', 'Male',   'aarav.sharma@example.com',     '9876543210', 1), " +
                        "(1002, 'Diya Patel',       '2003-08-22', 'Female', 'diya.patel@example.com',       '9876543211', 2), " +
                        "(1003, 'Rohan Verma',      '2002-11-05', 'Male',   'rohan.verma@example.com',      '9876543212', 3), " +
                        "(1004, 'Ananya Iyer',      '2003-02-18', 'Female', 'ananya.iyer@example.com',      '9876543213', 1), " +
                        "(1005, 'Karthik Raja',     '2002-09-30', 'Male',   'karthik.raja@example.com',     '9876543214', 4)"
                    );
                }
            }

            lastError = null;
            return true;
        } catch (SQLException e) {
            lastError = e.getMessage();
            return false;
        }
    }

    public static String getHost() { return host; }
    public static int getPort() { return port; }
    public static String getDbName() { return dbName; }
    public static String getUser() { return user; }
    public static String getPassword() { return password; }
    public static String getLastError() { return lastError; }
}
