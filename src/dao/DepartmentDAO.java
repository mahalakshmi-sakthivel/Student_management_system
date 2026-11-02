package dao;

import db.DBConnection;
import model.Department;

import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Data Access Object for Department operations.
 * Connects via JDBC PreparedStatement and provides graceful demo fallback.
 */
public class DepartmentDAO {

    private static final List<Department> FALLBACK_DEPTS = new CopyOnWriteArrayList<>();

    static {
        FALLBACK_DEPTS.add(new Department(1, "Computer Science"));
        FALLBACK_DEPTS.add(new Department(2, "Information Technology"));
        FALLBACK_DEPTS.add(new Department(3, "Electronics and Communication"));
        FALLBACK_DEPTS.add(new Department(4, "Mechanical Engineering"));
        FALLBACK_DEPTS.add(new Department(5, "Civil Engineering"));
    }

    public List<Department> getAllDepartments() throws SQLException {
        List<Department> list = new ArrayList<>();
        String sql = "SELECT department_id, department_name FROM departments ORDER BY department_name ASC";

        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                list.add(new Department(
                    rs.getInt("department_id"),
                    rs.getString("department_name")
                ));
            }
            return list;
        } catch (SQLException e) {
            // Return fallback list sorted
            List<Department> copy = new ArrayList<>(FALLBACK_DEPTS);
            copy.sort((a, b) -> a.getName().compareToIgnoreCase(b.getName()));
            return copy;
        }
    }

    public Department getDepartmentById(int id) throws SQLException {
        String sql = "SELECT department_id, department_name FROM departments WHERE department_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Department(
                        rs.getInt("department_id"),
                        rs.getString("department_name")
                    );
                }
            }
        } catch (SQLException e) {
            for (Department d : FALLBACK_DEPTS) {
                if (d.getId() == id) return d;
            }
        }
        return null;
    }

    public boolean addDepartment(String name) throws SQLException {
        int nextId = FALLBACK_DEPTS.size() + 1;
        FALLBACK_DEPTS.add(new Department(nextId, name.trim()));

        String sql = "INSERT INTO departments (department_name) VALUES (?)";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, name.trim());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            return true;
        }
    }
}
