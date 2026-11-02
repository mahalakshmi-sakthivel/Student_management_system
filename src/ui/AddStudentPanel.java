package ui;

import dao.DepartmentDAO;
import dao.StudentDAO;
import model.Department;
import model.Student;
import util.Validator;

import java.awt.*;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

/**
 * Panel for registering a new student into the database.
 * Form layout: Charcoal panel with 2-column input fields, Wine "Save" and grey "Clear" buttons.
 */
public class AddStudentPanel extends JPanel {

    private final StudentDAO studentDAO = new StudentDAO();
    private final DepartmentDAO departmentDAO = new DepartmentDAO();

    private JTextField idField;
    private JTextField nameField;
    private JTextField dobField;
    private JComboBox<String> genderBox;
    private JComboBox<Department> deptBox;
    private JTextField emailField;
    private JTextField phoneField;
    private JLabel statusLabel;

    public AddStudentPanel() {
        setLayout(new BorderLayout(16, 16));
        setBackground(Theme.BLACK);
        setBorder(new EmptyBorder(24, 28, 24, 28));
        initUI();
    }

    private void initUI() {
        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Theme.BLACK);

        JLabel title = Theme.titleLabel("➕ Add New Student");
        JLabel desc = Theme.mutedLabel("Fill in the student details below to register a new record into the database.");
        header.add(title, BorderLayout.NORTH);
        header.add(desc, BorderLayout.SOUTH);
        add(header, BorderLayout.NORTH);

        // Center: Charcoal Form Card
        JPanel formCard = Theme.cardPanel();
        formCard.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 14, 10, 14);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        idField = new JTextField(15);
        nameField = new JTextField(20);
        dobField = new JTextField(12);
        dobField.setToolTipText("Format: YYYY-MM-DD (e.g. 2003-05-14)");

        genderBox = new JComboBox<>(new String[]{"-- Select Gender --", "Male", "Female", "Other"});
        deptBox = new JComboBox<>();

        emailField = new JTextField(20);
        phoneField = new JTextField(15);

        Theme.styleField(idField);
        Theme.styleField(nameField);
        Theme.styleField(dobField);
        Theme.styleComboBox(genderBox);
        Theme.styleComboBox(deptBox);
        Theme.styleField(emailField);
        Theme.styleField(phoneField);

        // Row 0: Student ID & Name
        addFormField(formCard, gbc, 0, 0, "Student ID (Numeric):", idField, "e.g. 1011");
        addFormField(formCard, gbc, 0, 1, "Full Name (Letters & spaces):", nameField, "e.g. Rahul Sharma");

        // Row 1: Date of Birth & Gender
        addFormField(formCard, gbc, 1, 0, "Date of Birth (YYYY-MM-DD):", dobField, "e.g. 2003-05-14");
        addFormField(formCard, gbc, 1, 1, "Gender:", genderBox, "Select gender");

        // Row 2: Department & Phone
        addFormField(formCard, gbc, 2, 0, "Department:", deptBox, "Select department");
        addFormField(formCard, gbc, 2, 1, "Phone Number (10 digits):", phoneField, "e.g. 9876543210");

        // Row 3: Email Address (Spans 2 columns or left side)
        addFormField(formCard, gbc, 3, 0, "Email Address:", emailField, "e.g. student@example.com");

        // Helper hint row
        gbc.gridx = 2;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        JLabel noteLbl = Theme.mutedLabel("ℹ All fields are mandatory and strictly validated against MySQL constraints.");
        formCard.add(noteLbl, gbc);

        // Row 4: Status / Error Banner
        statusLabel = new JLabel(" ");
        statusLabel.setFont(Theme.FONT_BOLD);
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 4;
        formCard.add(statusLabel, gbc);

        // Row 5: Action Buttons
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 14, 10));
        actionPanel.setBackground(Theme.CHARCOAL);

        JButton clearBtn = Theme.secondaryButton("Clear Form");
        JButton saveBtn = Theme.button("Save Student Record");

        clearBtn.addActionListener(e -> clearForm());
        saveBtn.addActionListener(e -> onSave());

        actionPanel.add(clearBtn);
        actionPanel.add(saveBtn);

        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.gridwidth = 4;
        formCard.add(actionPanel, gbc);

        add(formCard, BorderLayout.CENTER);

        // Load departments
        loadDepartments();
    }

    private void addFormField(JPanel panel, GridBagConstraints gbc, int row, int col, String labelText, JComponent field, String hint) {
        gbc.gridwidth = 1;
        gbc.weightx = 0.15;
        gbc.gridx = col * 2;
        gbc.gridy = row;

        JPanel labelContainer = new JPanel(new BorderLayout());
        labelContainer.setBackground(Theme.CHARCOAL);
        JLabel lbl = Theme.label(labelText);
        lbl.setFont(Theme.FONT_BOLD);
        labelContainer.add(lbl, BorderLayout.NORTH);

        if (hint != null) {
            JLabel h = Theme.mutedLabel(hint);
            labelContainer.add(h, BorderLayout.SOUTH);
        }
        panel.add(labelContainer, gbc);

        gbc.weightx = 0.35;
        gbc.gridx = col * 2 + 1;
        panel.add(field, gbc);
    }

    public void loadDepartments() {
        deptBox.removeAllItems();
        try {
            List<Department> departments = departmentDAO.getAllDepartments();
            if (departments.isEmpty()) {
                deptBox.addItem(new Department(-1, "-- No Departments Found --"));
            } else {
                for (Department d : departments) {
                    deptBox.addItem(d);
                }
            }
        } catch (SQLException e) {
            deptBox.addItem(new Department(-1, "-- DB Error Loading Departments --"));
            statusLabel.setText("⚠ Warning: Could not load departments from MySQL: " + e.getMessage());
            statusLabel.setForeground(Theme.ERROR);
        }
    }

    private void onSave() {
        statusLabel.setText(" ");
        String idStr = idField.getText().trim();
        String name = nameField.getText().trim();
        String dobStr = dobField.getText().trim();
        String gender = (String) genderBox.getSelectedItem();
        Department dept = (Department) deptBox.getSelectedItem();
        String email = emailField.getText().trim();
        String phone = phoneField.getText().trim();

        boolean deptValid = (dept != null && dept.getId() > 0);

        // Validation Rules Check
        Validator.ValidationResult val = Validator.validateStudent(idStr, name, dobStr, gender, email, phone, deptValid);
        if (!val.isValid()) {
            Theme.showError(this, "Please fix the following validation errors:\n\n• " + val.getFormattedErrors(), "Validation Error");
            statusLabel.setText("⚠ Please correct highlighted errors.");
            statusLabel.setForeground(Theme.ERROR);
            return;
        }

        int id = Integer.parseInt(idStr);
        LocalDate dob = Validator.parseDate(dobStr);

        // Database checks and insert
        try {
            // Check duplicate Student ID
            if (studentDAO.isStudentIdTaken(id)) {
                Theme.showError(this, "Error: Student ID " + id + " already exists in the database! Please use a unique ID.", "Duplicate Student ID");
                statusLabel.setText("⚠ Student ID " + id + " already exists.");
                statusLabel.setForeground(Theme.ERROR);
                idField.requestFocus();
                return;
            }

            // Check duplicate Email
            if (studentDAO.isEmailTaken(email, -1)) {
                Theme.showError(this, "Error: Email '" + email + "' is already registered with another student.", "Duplicate Email");
                statusLabel.setText("⚠ Email is already in use.");
                statusLabel.setForeground(Theme.ERROR);
                emailField.requestFocus();
                return;
            }

            Student s = new Student(id, name, dob, gender, email, phone, dept.getId());
            boolean success = studentDAO.addStudent(s);

            if (success) {
                Theme.showInfo(this, "Student registered successfully!\n\nID: " + id + "\nName: " + name + "\nDepartment: " + dept.getName(), "Success");
                statusLabel.setText("✓ Student " + name + " (ID: " + id + ") saved successfully!");
                statusLabel.setForeground(Theme.SUCCESS);
                clearForm();
            } else {
                Theme.showError(this, "Failed to insert student record. Please try again.", "Error");
            }

        } catch (SQLException ex) {
            Theme.showError(this, "Database Error while saving student:\n" + ex.getMessage(), "Database Error");
            statusLabel.setText("⚠ Database Error: " + ex.getMessage());
            statusLabel.setForeground(Theme.ERROR);
        }
    }

    private void clearForm() {
        idField.setText("");
        nameField.setText("");
        dobField.setText("");
        genderBox.setSelectedIndex(0);
        if (deptBox.getItemCount() > 0) {
            deptBox.setSelectedIndex(0);
        }
        emailField.setText("");
        phoneField.setText("");
        idField.requestFocus();
    }
}

