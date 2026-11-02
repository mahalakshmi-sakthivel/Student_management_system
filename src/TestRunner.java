import db.DBConnection;
import util.SecurityUtil;
import util.Validator;

public class TestRunner {

    public static void main(String[] args) {
        int passed = 0;
        int failed = 0;

        System.out.println("==================================================");
        System.out.println(" Running Student Management System Verification   ");
        System.out.println("==================================================");

        // Test 1: SHA-256 Hashing test
        String hash = SecurityUtil.sha256("admin123");
        String expected = "240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a9";
        if (expected.equals(hash)) {
            System.out.println("[PASS] Test 1: Password hashing matches MySQL SHA2('admin123', 256)");
            passed++;
        } else {
            System.err.println("[FAIL] Test 1: Expected " + expected + " but got " + hash);
            failed++;
        }

        // Test 2: Validation of valid student
        Validator.ValidationResult res1 = Validator.validateStudent(
            "1001", "Aarav Sharma", "2003-05-14", "Male", "aarav@example.com", "9876543210", true
        );
        if (res1.isValid()) {
            System.out.println("[PASS] Test 2: Valid student inputs pass all validation rules");
            passed++;
        } else {
            System.err.println("[FAIL] Test 2: Valid student was rejected: " + res1.getFormattedErrors());
            failed++;
        }

        // Test 3: Validation of non-numeric Student ID
        Validator.ValidationResult res2 = Validator.validateStudent(
            "ABC", "Aarav Sharma", "2003-05-14", "Male", "aarav@example.com", "9876543210", true
        );
        if (!res2.isValid() && res2.getErrors().stream().anyMatch(e -> e.contains("integer number"))) {
            System.out.println("[PASS] Test 3: Non-numeric Student ID rejected correctly");
            passed++;
        } else {
            System.err.println("[FAIL] Test 3: Non-numeric ID was not caught");
            failed++;
        }

        // Test 4: Validation of future DOB
        Validator.ValidationResult res3 = Validator.validateStudent(
            "1002", "Diya Patel", "2099-01-01", "Female", "diya@example.com", "9876543210", true
        );
        if (!res3.isValid() && res3.getErrors().stream().anyMatch(e -> e.contains("future"))) {
            System.out.println("[PASS] Test 4: Future Date of Birth rejected correctly");
            passed++;
        } else {
            System.err.println("[FAIL] Test 4: Future DOB was not caught");
            failed++;
        }

        // Test 5: Validation of invalid email
        Validator.ValidationResult res4 = Validator.validateStudent(
            "1003", "Rohan Verma", "2002-11-05", "Male", "invalid-email-address", "9876543210", true
        );
        if (!res4.isValid() && res4.getErrors().stream().anyMatch(e -> e.contains("email"))) {
            System.out.println("[PASS] Test 5: Malformed email rejected correctly");
            passed++;
        } else {
            System.err.println("[FAIL] Test 5: Malformed email was not caught");
            failed++;
        }

        // Test 6: Validation of invalid phone number (< 10 digits)
        Validator.ValidationResult res5 = Validator.validateStudent(
            "1004", "Ananya Iyer", "2003-02-18", "Female", "ananya@example.com", "12345", true
        );
        if (!res5.isValid() && res5.getErrors().stream().anyMatch(e -> e.contains("10 digits"))) {
            System.out.println("[PASS] Test 6: Invalid phone number rejected correctly");
            passed++;
        } else {
            System.err.println("[FAIL] Test 6: Invalid phone number was not caught");
            failed++;
        }

        // Test 7: Validation of unselected department
        Validator.ValidationResult res6 = Validator.validateStudent(
            "1005", "Karthik Raja", "2002-09-30", "Male", "karthik@example.com", "9876543210", false
        );
        if (!res6.isValid() && res6.getErrors().stream().anyMatch(e -> e.contains("Department"))) {
            System.out.println("[PASS] Test 7: Missing department rejected correctly");
            passed++;
        } else {
            System.err.println("[FAIL] Test 7: Missing department was not caught");
            failed++;
        }

        // Test 8: MySQL JDBC Driver presence
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            System.out.println("[PASS] Test 8: MySQL Connector/J driver (com.mysql.cj.jdbc.Driver) loaded successfully");
            passed++;
        } catch (ClassNotFoundException e) {
            System.err.println("[FAIL] Test 8: MySQL Connector/J driver not found: " + e.getMessage());
            failed++;
        }

        System.out.println("--------------------------------------------------");
        System.out.println("Tests Passed: " + passed + " / " + (passed + failed));
        if (failed == 0) {
            System.out.println(">> ALL VERIFICATION TESTS PASSED SUCCESSFULLY! <<");
        } else {
            System.exit(1);
        }
    }
}
