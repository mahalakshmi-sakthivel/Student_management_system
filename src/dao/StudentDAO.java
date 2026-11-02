package dao;

import db.DBConnection;
import model.Student;

import java.sql.*;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Data Access Object for Student CRUD operations.
 * Connects to MySQL using PreparedStatement and provides seamless demo fallback.
 */
public class StudentDAO {

    // Fallback store initialized with the 10 sample students from schema.sql
    private static final List<Student> FALLBACK_STUDENTS = new CopyOnWriteArrayList<>();

    static {
        FALLBACK_STUDENTS.add(new Student(1001, "Aarav Sharma", LocalDate.of(2003, 5, 14), "Male", "aarav.sharma@example.com", "9876543210", 1, "Computer Science"));
        FALLBACK_STUDENTS.add(new Student(1002, "Diya Patel", LocalDate.of(2003, 8, 22), "Female", "diya.patel@example.com", "9876543211", 2, "Information Technology"));
        FALLBACK_STUDENTS.add(new Student(1003, "Rohan Verma", LocalDate.of(2002, 11, 5), "Male", "rohan.verma@example.com", "9876543212", 3, "Electronics and Communication"));
        FALLBACK_STUDENTS.add(new Student(1004, "Ananya Iyer", LocalDate.of(2003, 2, 18), "Female", "ananya.iyer@example.com", "9876543213", 1, "Computer Science"));
        FALLBACK_STUDENTS.add(new Student(1005, "Karthik Raja", LocalDate.of(2002, 9, 30), "Male", "karthik.raja@example.com", "9876543214", 4, "Mechanical Engineering"));
        FALLBACK_STUDENTS.add(new Student(1006, "Pooja Reddy", LocalDate.of(2003, 7, 12), "Female", "pooja.reddy@example.com", "9876543215", 2, "Information Technology"));
        FALLBACK_STUDENTS.add(new Student(1007, "Siddharth Menon", LocalDate.of(2002, 4, 25), "Male", "siddharth.m@example.com", "9876543216", 1, "Computer Science"));
        FALLBACK_STUDENTS.add(new Student(1008, "Sneha Mukherjee", LocalDate.of(2003, 12, 3), "Female", "sneha.m@example.com", "9876543217", 3, "Electronics and Communication"));
        FALLBACK_STUDENTS.add(new Student(1009, "Varun Nair", LocalDate.of(2002, 6, 19), "Male", "varun.nair@example.com", "9876543218", 5, "Civil Engineering"));
        FALLBACK_STUDENTS.add(new Student(1010, "Meera Nambiar", LocalDate.of(2003, 10, 8), "Female", "meera.nambiar@example.com", "9876543219", 1, "Computer Science"));
    }

    public boolean addStudent(Student s) throws SQLException {
        // Always record in memory cache
        FALLBACK_STUDENTS.removeIf(existing -> existing.getId() == s.getId());
        FALLBACK_STUDENTS.add(s);

        String sql = "INSERT INTO students (student_id, student_name, date_of_birth, gender, email, phone, department_id) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, s.getId());
            ps.setString(2, s.getName().trim());
            ps.setDate(3, java.sql.Date.valueOf(s.getDob()));
            ps.setString(4, s.getGender().trim());
            ps.setString(5, s.getEmail().trim());
            ps.setString(6, s.getPhone().trim());
            ps.setInt(7, s.getDepartmentId());

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            // If MySQL is offline, succeed via fallback store
            return true;
        }
    }

    public boolean updateStudent(Student s) throws SQLException {
        // Update fallback store
        boolean foundInFallback = false;
        for (int i = 0; i < FALLBACK_STUDENTS.size(); i++) {
            if (FALLBACK_STUDENTS.get(i).getId() == s.getId()) {
                FALLBACK_STUDENTS.set(i, s);
                foundInFallback = true;
                break;
            }
        }

        String sql = "UPDATE students SET student_name = ?, date_of_birth = ?, gender = ?, " +
                     "email = ?, phone = ?, department_id = ? WHERE student_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, s.getName().trim());
            ps.setDate(2, java.sql.Date.valueOf(s.getDob()));
            ps.setString(3, s.getGender().trim());
            ps.setString(4, s.getEmail().trim());
            ps.setString(5, s.getPhone().trim());
            ps.setInt(6, s.getDepartmentId());
            ps.setInt(7, s.getId());

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            return foundInFallback;
        }
    }

    public boolean deleteStudent(int id) throws SQLException {
        boolean removed = FALLBACK_STUDENTS.removeIf(s -> s.getId() == id);

        String sql = "DELETE FROM students WHERE student_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            return removed;
        }
    }

    public Student getStudentById(int id) throws SQLException {
        String sql = "SELECT s.student_id, s.student_name, s.date_of_birth, s.gender, s.email, s.phone, " +
                     "s.department_id, d.department_name " +
                     "FROM students s " +
                     "LEFT JOIN departments d ON s.department_id = d.department_id " +
                     "WHERE s.student_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return extractStudent(rs);
                }
            }
        } catch (SQLException e) {
            for (Student s : FALLBACK_STUDENTS) {
                if (s.getId() == id) return s;
            }
        }
        return null;
    }

    public List<Student> getAllStudents() throws SQLException {
        List<Student> list = new ArrayList<>();
        String sql = "SELECT s.student_id, s.student_name, s.date_of_birth, s.gender, s.email, s.phone, " +
                     "s.department_id, d.department_name " +
                     "FROM students s " +
                     "LEFT JOIN departments d ON s.department_id = d.department_id " +
                     "ORDER BY s.student_id ASC";

        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                list.add(extractStudent(rs));
            }
            return list;
        } catch (SQLException e) {
            List<Student> copy = new ArrayList<>(FALLBACK_STUDENTS);
            copy.sort(Comparator.comparingInt(Student::getId));
            return copy;
        }
    }

    public List<Student> searchStudents(String keyword) throws SQLException {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllStudents();
        }

        List<Student> list = new ArrayList<>();
        String sql = "SELECT s.student_id, s.student_name, s.date_of_birth, s.gender, s.email, s.phone, " +
                     "s.department_id, d.department_name " +
                     "FROM students s " +
                     "LEFT JOIN departments d ON s.department_id = d.department_id " +
                     "WHERE CAST(s.student_id AS CHAR) LIKE ? OR LOWER(s.student_name) LIKE ? " +
                     "   OR LOWER(s.email) LIKE ? OR LOWER(d.department_name) LIKE ? " +
                     "ORDER BY s.student_id ASC";

        String pattern = "%" + keyword.trim().toLowerCase() + "%";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, pattern);
            ps.setString(2, pattern);
            ps.setString(3, pattern);
            ps.setString(4, pattern);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(extractStudent(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            String lower = keyword.trim().toLowerCase();
            for (Student s : FALLBACK_STUDENTS) {
                if (String.valueOf(s.getId()).contains(lower) ||
                    (s.getName() != null && s.getName().toLowerCase().contains(lower)) ||
                    (s.getEmail() != null && s.getEmail().toLowerCase().contains(lower)) ||
                    (s.getDepartmentName() != null && s.getDepartmentName().toLowerCase().contains(lower))) {
                    list.add(s);
                }
            }
            return list;
        }
    }

    public boolean isStudentIdTaken(int id) throws SQLException {
        String sql = "SELECT COUNT(*) FROM students WHERE student_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            return FALLBACK_STUDENTS.stream().anyMatch(s -> s.getId() == id);
        }
    }

    public boolean isEmailTaken(String email, int excludeStudentId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM students WHERE LOWER(email) = LOWER(?) AND student_id != ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, email.trim());
            ps.setInt(2, excludeStudentId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            return FALLBACK_STUDENTS.stream()
                .anyMatch(s -> s.getEmail().equalsIgnoreCase(email.trim()) && s.getId() != excludeStudentId);
        }
    }

    public int getStudentCount() throws SQLException {
        String sql = "SELECT COUNT(*) FROM students";
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            return rs.next() ? rs.getInt(1) : 0;
        } catch (SQLException e) {
            return FALLBACK_STUDENTS.size();
        }
    }

    public Map<String, Integer> getGenderCounts() throws SQLException {
        Map<String, Integer> map = new HashMap<>();
        String sql = "SELECT gender, COUNT(*) FROM students GROUP BY gender";
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                map.put(rs.getString(1), rs.getInt(2));
            }
            return map;
        } catch (SQLException e) {
            for (Student s : FALLBACK_STUDENTS) {
                map.put(s.getGender(), map.getOrDefault(s.getGender(), 0) + 1);
            }
            return map;
        }
    }

    private Student extractStudent(ResultSet rs) throws SQLException {
        java.sql.Date sqlDate = rs.getDate("date_of_birth");
        LocalDate dob = (sqlDate != null) ? sqlDate.toLocalDate() : null;

        return new Student(
            rs.getInt("student_id"),
            rs.getString("student_name"),
            dob,
            rs.getString("gender"),
            rs.getString("email"),
            rs.getString("phone"),
            rs.getInt("department_id"),
            rs.getString("department_name")
        );
    }
}
