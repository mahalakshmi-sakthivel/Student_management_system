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
import javax.swing.table.DefaultTableModel;

/**
 * Panel to select and update student details.
 * Layout: Split view with quick selector table on the left, and styled edit form on the right.
 */
public class UpdateStudentPanel extends JPanel {

    private final StudentDAO studentDAO = new StudentDAO();
    private final DepartmentDAO departmentDAO = new DepartmentDAO();

    // Table Selector
    private JTable studentTable;
    private DefaultTableModel tableModel;
    private JTextField searchField;

    // Form Fields
    private JTextField idField;
    private JTextField nameField;
    private JTextField dobField;
    private JComboBox<String> genderBox;
    private JComboBox<Department> deptBox;
    private JTextField emailField;
    private JTextField phoneField;
    private JLabel statusLabel;
    private JButton updateBtn;

    public UpdateStudentPanel() {
        setLayout(new BorderLayout(16, 16));
        setBackground(Theme.BLACK);
        setBorder(new EmptyBorder(24, 28, 24, 28));
        initUI();
    }

    private void initUI() {
        // Top Header
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Theme.BLACK);

        JLabel title = Theme.titleLabel("✏️ Update Student Record");
        JLabel desc = Theme.mutedLabel("Select a student from the list on the left to edit and save updated information.");
        header.add(title, BorderLayout.NORTH);
        header.add(desc, BorderLayout.SOUTH);
        add(header, BorderLayout.NORTH);

        // Center Split: Left = Selection Table, Right = Edit Form
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(380);
        splitPane.setBackground(Theme.BLACK);
        splitPane.setBorder(null);

        // 1. Left Selection Panel
        JPanel leftPanel = Theme.cardPanel();
        leftPanel.setLayout(new BorderLayout(10, 10));

        JPanel searchBar = new JPanel(new BorderLayout(6, 6));
        searchBar.setBackground(Theme.CHARCOAL);
        JLabel searchLbl = Theme.sectionLabel("Select Student:");
        searchField = new JTextField();
        Theme.styleField(searchField);
        searchField.setToolTipText("Filter by ID or Name...");

        searchField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { filterList(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { filterList(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { filterList(); }
        });

        searchBar.add(searchLbl, BorderLayout.NORTH);
        searchBar.add(searchField, BorderLayout.CENTER);
        leftPanel.add(searchBar, BorderLayout.NORTH);

        tableModel = new DefaultTableModel(new String[]{"ID", "Name", "Department"}, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        studentTable = new JTable(tableModel);
        Theme.styleTable(studentTable);
        studentTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        studentTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && studentTable.getSelectedRow() != -1) {
                int row = studentTable.getSelectedRow();
                int studentId = (int) tableModel.getValueAt(row, 0);
                loadStudentDetails(studentId);
            }
        });

        JScrollPane tableScroll = new JScrollPane(studentTable);
        Theme.styleScrollPane(tableScroll);
        leftPanel.add(tableScroll, BorderLayout.CENTER);

        splitPane.setLeftComponent(leftPanel);

        // 2. Right Edit Form Panel
        JPanel rightCard = Theme.cardPanel();
        rightCard.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 12, 8, 12);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        idField = new JTextField(15);
        idField.setEditable(false);
        idField.setBackground(new Color(0x1F, 0x1F, 0x1F));
        idField.setToolTipText("Student ID is the Primary Key and cannot be modified.");

        nameField = new JTextField(20);
        dobField = new JTextField(15);
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

        int row = 0;
        addEditRow(rightCard, gbc, row++, "Student ID (Locked):", idField);
        addEditRow(rightCard, gbc, row++, "Full Name:", nameField);
        addEditRow(rightCard, gbc, row++, "Date of Birth (YYYY-MM-DD):", dobField);
        addEditRow(rightCard, gbc, row++, "Gender:", genderBox);
        addEditRow(rightCard, gbc, row++, "Department:", deptBox);
        addEditRow(rightCard, gbc, row++, "Email Address:", emailField);
        addEditRow(rightCard, gbc, row++, "Phone Number (10 digits):", phoneField);

        // Status banner
        statusLabel = new JLabel("Please select a student from the list to begin editing.");
        statusLabel.setFont(Theme.FONT_SMALL);
        statusLabel.setForeground(Theme.TEXT_MUTED);
        gbc.gridx = 0;
        gbc.gridy = row++;
        gbc.gridwidth = 2;
        rightCard.add(statusLabel, gbc);

        // Action Buttons
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 10));
        actionPanel.setBackground(Theme.CHARCOAL);

        JButton resetBtn = Theme.secondaryButton("Reset Fields");
        updateBtn = Theme.button("Update Record");
        updateBtn.setEnabled(false);

        resetBtn.addActionListener(e -> {
            if (studentTable.getSelectedRow() != -1) {
                int sId = (int) tableModel.getValueAt(studentTable.getSelectedRow(), 0);
                loadStudentDetails(sId);
            }
        });

        updateBtn.addActionListener(e -> onUpdate());

        actionPanel.add(resetBtn);
        actionPanel.add(updateBtn);

        gbc.gridx = 0;
        gbc.gridy = row++;
        gbc.gridwidth = 2;
        rightCard.add(actionPanel, gbc);

        splitPane.setRightComponent(rightCard);
        add(splitPane, BorderLayout.CENTER);

        // Initialize lists
        loadDepartments();
        loadStudentList();
    }

    private void addEditRow(JPanel panel, GridBagConstraints gbc, int row, String labelText, JComponent field) {
        gbc.gridwidth = 1;
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0.3;
        JLabel lbl = Theme.label(labelText);
        lbl.setFont(Theme.FONT_BOLD);
        panel.add(lbl, gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.7;
        panel.add(field, gbc);
    }

    public void loadDepartments() {
        deptBox.removeAllItems();
        try {
            List<Department> list = departmentDAO.getAllDepartments();
            for (Department d : list) {
                deptBox.addItem(d);
            }
        } catch (SQLException e) {
            statusLabel.setText("⚠ Warning: Could not load departments: " + e.getMessage());
            statusLabel.setForeground(Theme.ERROR);
        }
    }

    public void loadStudentList() {
        tableModel.setRowCount(0);
        try {
            List<Student> students = studentDAO.getAllStudents();
            for (Student s : students) {
                tableModel.addRow(new Object[]{
                    s.getId(),
                    s.getName(),
                    s.getDepartmentName() != null ? s.getDepartmentName() : "Dept " + s.getDepartmentId()
                });
            }
        } catch (SQLException e) {
            statusLabel.setText("⚠ Could not load students: " + e.getMessage());
            statusLabel.setForeground(Theme.ERROR);
        }
    }

    private void filterList() {
        String keyword = searchField.getText().trim();
        tableModel.setRowCount(0);
        try {
            List<Student> students = studentDAO.searchStudents(keyword);
            for (Student s : students) {
                tableModel.addRow(new Object[]{
                    s.getId(),
                    s.getName(),
                    s.getDepartmentName() != null ? s.getDepartmentName() : "Dept " + s.getDepartmentId()
                });
            }
        } catch (SQLException ignored) {}
    }

    public void loadStudentForEdit(int studentId) {
        loadStudentList();
        // Locate in table
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            if ((int) tableModel.getValueAt(i, 0) == studentId) {
                studentTable.setRowSelectionInterval(i, i);
                studentTable.scrollRectToVisible(studentTable.getCellRect(i, 0, true));
                break;
            }
        }
        loadStudentDetails(studentId);
    }

    private void loadStudentDetails(int studentId) {
        try {
            Student s = studentDAO.getStudentById(studentId);
            if (s != null) {
                idField.setText(String.valueOf(s.getId()));
                nameField.setText(s.getName());
                dobField.setText(Validator.formatDate(s.getDob()));
                genderBox.setSelectedItem(s.getGender());
                emailField.setText(s.getEmail());
                phoneField.setText(s.getPhone());

                // Select matching department in combobox
                for (int i = 0; i < deptBox.getItemCount(); i++) {
                    Department d = deptBox.getItemAt(i);
                    if (d.getId() == s.getDepartmentId()) {
                        deptBox.setSelectedIndex(i);
                        break;
                    }
                }

                updateBtn.setEnabled(true);
                statusLabel.setText("Editing student ID: " + s.getId() + " (" + s.getName() + ")");
                statusLabel.setForeground(Theme.ACCENT);
            }
        } catch (SQLException e) {
            statusLabel.setText("⚠ Error loading student details: " + e.getMessage());
            statusLabel.setForeground(Theme.ERROR);
        }
    }

    private void onUpdate() {
        String idStr = idField.getText().trim();
        if (idStr.isEmpty()) return;

        int id = Integer.parseInt(idStr);
        String name = nameField.getText().trim();
        String dobStr = dobField.getText().trim();
        String gender = (String) genderBox.getSelectedItem();
        Department dept = (Department) deptBox.getSelectedItem();
        String email = emailField.getText().trim();
        String phone = phoneField.getText().trim();

        boolean deptValid = (dept != null && dept.getId() > 0);

        Validator.ValidationResult val = Validator.validateStudent(idStr, name, dobStr, gender, email, phone, deptValid);
        if (!val.isValid()) {
            Theme.showError(this, "Please fix the following validation errors:\n\n• " + val.getFormattedErrors(), "Validation Error");
            return;
        }

        LocalDate dob = Validator.parseDate(dobStr);

        try {
            // Check email uniqueness excluding this student
            if (studentDAO.isEmailTaken(email, id)) {
                Theme.showError(this, "Email '" + email + "' is already registered to another student.", "Duplicate Email");
                return;
            }

            Student s = new Student(id, name, dob, gender, email, phone, dept.getId());
            boolean success = studentDAO.updateStudent(s);

            if (success) {
                Theme.showInfo(this, "Student record updated successfully in MySQL!", "Update Successful");
                statusLabel.setText("✓ Student " + name + " (ID: " + id + ") updated successfully!");
                statusLabel.setForeground(Theme.SUCCESS);

                // Refresh table selector
                int selectedRow = studentTable.getSelectedRow();
                filterList();
                if (selectedRow != -1 && selectedRow < studentTable.getRowCount()) {
                    studentTable.setRowSelectionInterval(selectedRow, selectedRow);
                }
            } else {
                Theme.showError(this, "Failed to update student record.", "Update Failed");
            }

        } catch (SQLException ex) {
            Theme.showError(this, "Database Error while updating:\n" + ex.getMessage(), "Database Error");
            statusLabel.setText("⚠ Database Error: " + ex.getMessage());
            statusLabel.setForeground(Theme.ERROR);
        }
    }
}

