package util;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Validates student input fields according to business rules.
 */
public class Validator {

    private static final Pattern NAME_PATTERN = Pattern.compile("^[a-zA-Z\\s]{2,100}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^[0-9]{10}$");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static class ValidationResult {
        private final List<String> errors = new ArrayList<>();

        public void addError(String error) {
            errors.add(error);
        }

        public boolean isValid() {
            return errors.isEmpty();
        }

        public List<String> getErrors() {
            return errors;
        }

        public String getFormattedErrors() {
            return String.join("\n• ", errors);
        }
    }

    /**
     * Validates student data.
     *
     * @param idStr       Student ID string
     * @param name        Student full name
     * @param dobStr      Date of birth string (yyyy-MM-dd)
     * @param gender      Gender string
     * @param email       Email address
     * @param phone       Phone number
     * @param deptSelected Whether a valid department was selected
     * @return ValidationResult containing any detected errors
     */
    public static ValidationResult validateStudent(
            String idStr,
            String name,
            String dobStr,
            String gender,
            String email,
            String phone,
            boolean deptSelected) {

        ValidationResult result = new ValidationResult();

        // 1. Student ID
        if (idStr == null || idStr.trim().isEmpty()) {
            result.addError("Student ID is required.");
        } else {
            try {
                int id = Integer.parseInt(idStr.trim());
                if (id <= 0) {
                    result.addError("Student ID must be a positive number.");
                }
            } catch (NumberFormatException e) {
                result.addError("Student ID must be a valid integer number.");
            }
        }

        // 2. Name
        if (name == null || name.trim().isEmpty()) {
            result.addError("Student Name is required.");
        } else if (!NAME_PATTERN.matcher(name.trim()).matches()) {
            result.addError("Student Name must contain only letters and spaces (2-100 characters).");
        }

        // 3. Date of Birth
        if (dobStr == null || dobStr.trim().isEmpty()) {
            result.addError("Date of Birth is required (format: YYYY-MM-DD).");
        } else {
            try {
                LocalDate dob = LocalDate.parse(dobStr.trim(), DATE_FORMATTER);
                if (dob.isAfter(LocalDate.now())) {
                    result.addError("Date of Birth cannot be in the future.");
                } else if (dob.isBefore(LocalDate.now().minusYears(100))) {
                    result.addError("Date of Birth cannot be more than 100 years ago.");
                } else if (dob.isAfter(LocalDate.now().minusYears(14))) {
                    result.addError("Student must be at least 14 years of age.");
                }
            } catch (DateTimeParseException e) {
                result.addError("Date of Birth must be in valid format (YYYY-MM-DD), e.g. 2003-05-14.");
            }
        }

        // 4. Gender
        if (gender == null || gender.trim().isEmpty() || gender.equalsIgnoreCase("Select")) {
            result.addError("Please select a Gender (Male, Female, or Other).");
        } else if (!gender.equalsIgnoreCase("Male") &&
                   !gender.equalsIgnoreCase("Female") &&
                   !gender.equalsIgnoreCase("Other")) {
            result.addError("Gender must be Male, Female, or Other.");
        }

        // 5. Email
        if (email == null || email.trim().isEmpty()) {
            result.addError("Email address is required.");
        } else if (!EMAIL_PATTERN.matcher(email.trim()).matches()) {
            result.addError("Invalid email format (e.g., student@example.com).");
        }

        // 6. Phone
        if (phone == null || phone.trim().isEmpty()) {
            result.addError("Phone number is required.");
        } else {
            String digitsOnly = phone.trim().replaceAll("[\\s\\-+]", "");
            if (!PHONE_PATTERN.matcher(digitsOnly).matches()) {
                result.addError("Phone number must contain exactly 10 digits.");
            }
        }

        // 7. Department
        if (!deptSelected) {
            result.addError("Please select a valid Department.");
        }

        return result;
    }

    public static LocalDate parseDate(String dobStr) {
        if (dobStr == null || dobStr.trim().isEmpty()) return null;
        try {
            return LocalDate.parse(dobStr.trim(), DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    public static String formatDate(LocalDate date) {
        if (date == null) return "";
        return date.format(DATE_FORMATTER);
    }
}

