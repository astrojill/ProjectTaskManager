package fr.ece.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

// Utility class for database connection.

public class DatabaseConnection {

    // Make sure your server running and database exists.
    private static final String DB_URL = "jdbc:mysql://localhost:3306/task_manager_db?allowMultiQueries=true&useSSL=false&serverTimezone=UTC";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "";
    private static final String DRIVER = "com.mysql.cj.jdbc.Driver";

    public static Connection getConnection() throws SQLException {
        Connection connection = null;
        try {
            // 1. Load the MySQL JDBC driver class
            Class.forName(DRIVER);

            // 2. Establish the connection
            connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            System.out.println("Database connection established successfully.");

            // 3. Create tables if they do not exist
            createTables(connection);

            return connection;
        } catch (ClassNotFoundException e) {
            System.err.println("MySQL JDBC Driver not found. Make sure the JAR is in the classpath.");
            throw new SQLException("JDBC Driver not found: " + e.getMessage());
        } catch (SQLException e) {
            System.err.println("Database connection failed or table creation failed.");
            close(null, null, connection); // Ensure connection is closed on failure
            throw e;
        }
    }

    private static void createTables(Connection connection) {
        Statement statement = null;
        try {
            statement = connection.createStatement();

            String createCategoriesTable = """
                CREATE TABLE IF NOT EXISTS categories (
                    id INT PRIMARY KEY AUTO_INCREMENT,
                    name VARCHAR(100) NOT NULL UNIQUE,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
                );
                """;
            statement.executeUpdate(createCategoriesTable);
            System.out.println("Table 'categories' ensured to exist.");

            String createUsersTable = """
                CREATE TABLE IF NOT EXISTS users (
                    id INT PRIMARY KEY AUTO_INCREMENT,
                    username VARCHAR(50) NOT NULL UNIQUE,
                    password_hash VARCHAR(255) NOT NULL,
                    role ENUM('USER', 'ADMIN') NOT NULL DEFAULT 'USER',
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    INDEX idx_username (username)
                );
                """;
            statement.executeUpdate(createUsersTable);
            System.out.println("Table 'users' ensured to exist.");

            String createTasksTable = """
                CREATE TABLE IF NOT EXISTS tasks (
                    id INT PRIMARY KEY AUTO_INCREMENT,
                    title VARCHAR(200) NOT NULL,
                    description TEXT,
                    due_date DATE,
                    status ENUM('TODO', 'IN_PROGRESS', 'DONE', 'CANCELLED') NOT NULL DEFAULT 'TODO',
                    priority ENUM('LOW', 'MEDIUM', 'HIGH') DEFAULT 'MEDIUM',
                    category_id INT,
                    user_id INT NOT NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                
                    -- Foreign key to categories
                    CONSTRAINT fk_task_category
                        FOREIGN KEY (category_id) 
                        REFERENCES categories(id)
                        ON DELETE SET NULL, -- If a category is deleted, tasks remain but category_id is set to NULL
                
                    -- Foreign key to users
                    CONSTRAINT fk_task_user
                        FOREIGN KEY (user_id) 
                        REFERENCES users(id)
                        ON DELETE CASCADE, -- If a user is deleted, all their tasks are also deleted
                    
                    -- Indexes for better performance
                    INDEX idx_user_id (user_id),
                    INDEX idx_category_id (category_id),
                    INDEX idx_status (status),
                    INDEX idx_due_date (due_date)
                );
                """;
            statement.executeUpdate(createTasksTable);
            System.out.println("Table 'tasks' ensured to exist.");

        } catch (SQLException e) {
            System.err.println("Error during table creation: " + e.getMessage());
            // Depending on context, might want to rethrow exception.
        } finally {
            close(null, statement, null);
        }
    }


    // Helper method to safely close JDBC resources.

    public static void close(ResultSet resultSet, Statement statement, Connection connection) {
        if (resultSet != null) {
            try {
                resultSet.close();
            } catch (SQLException e) {
                System.err.println("Error closing ResultSet: " + e.getMessage());
            }
        }
        if (statement != null) {
            try {
                statement.close();
            } catch (SQLException e) {
                System.err.println("Error closing Statement: " + e.getMessage());
            }
        }
        if (connection != null) {
            try {
                if (!connection.isClosed()) {
                    connection.close();
                }
            } catch (SQLException e) {
                System.err.println("Error closing Connection: " + e.getMessage());
            }
        }
    }
}