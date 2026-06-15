package com.demo.repository;

import java.sql.*;

/**
 * File 4/6 - Repository (SQL sink)
 * Taint arrives here after: Controller → Service → QueryHelper → Repository
 * 
 * Also contains: Resource leak on exception path
 * - Connection opened but not closed if ResultSet processing throws
 */
public class UserRepository {

    private static final String DB_URL = "jdbc:mysql://localhost:3306/app";

    /**
     * SQL INJECTION SINK
     * Taint path (4 files): Controller.getUser() → Service.findUser() 
     *   → QueryHelper.buildCondition() → here
     */
    public String executeQuery(String table, String condition) {
        Connection conn = null;
        Statement stmt = null;
        try {
            conn = DriverManager.getConnection(DB_URL, "root", "pass");
            stmt = conn.createStatement();

            // SINK: tainted condition interpolated into SQL
            String sql = "SELECT * FROM " + table + " WHERE " + condition;
            ResultSet rs = stmt.executeQuery(sql);

            StringBuilder result = new StringBuilder();
            while (rs.next()) {
                result.append(rs.getString(1)).append(",");
            }
            // BUG: if rs.getString() throws, conn and stmt are leaked
            // No finally block to ensure cleanup
            conn.close();
            return result.toString();

        } catch (SQLException e) {
            // RESOURCE LEAK: conn and stmt not closed in error path
            return "error: " + e.getMessage();
        }
    }
}
