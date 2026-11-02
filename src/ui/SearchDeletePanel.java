package ui;

import dao.StudentDAO;
import model.Student;

import java.awt.*;
import java.sql.SQLException;
import java.util.List;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;

/**
 * Panel to search and delete student records.
 * Features: Multi-criteria search, results table, selected student preview card,
 * and a prominent red Delete button with strict confirmation dialog.
 */
public class SearchDeletePanel extends JPanel {

    private final StudentDAO studentDAO = new StudentDAO();

    private JTextField searchInput;
    private JTable resultsTable;
    private DefaultTableModel tableModel;
    private JLabel resultStatusLabel;

    // Preview Card labels
    private JLabel previewIdVal;
    private JLabel previewNameVal;
    private JLabel previewDobVal;
    private JLabel previewGenderVal;
    private JLabel previewDeptVal;
    private JLabel previewEmailVal;
    private JLabel previewPhoneVal;
    private JButton deleteBtn;

    private Student selectedStudent = null;

    private static final String[] COLUMNS = {
        "Student ID", "Full Name", "Date of Birth", "Gender", "Department", "Email", "Phone"
    };

    public SearchDeletePanel() {
        setLayout(new BorderLayout(16, 16));
        setBackground(Theme.BLACK);
        setBorder(new EmptyBorder(24, 28, 24, 28));
        initUI();
    }

    private void initUI() {
        // 1. Top Section: Search Bar Header
        JPanel topContainer = new JPanel(new BorderLayout(12, 12));
        topContainer.setBackground(Theme.BLACK);

        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setBackground(Theme.BLACK);
        JLabel title = Theme.titleLabel("🔍 Search & Delete Student");
        JLabel desc = Theme.mutedLabel("Search records by Student ID or Name, preview details, and delete with confirmation.");
        titlePanel.add(title, BorderLayout.NORTH);
        titlePanel.add(desc, BorderLayout.SOUTH);
        topContainer.add(titlePanel, BorderLayout.NORTH);

        // Search Bar Card
        JPanel searchBarCard = Theme.cardPanel();
        searchBarCard.setLayout(new FlowLayout(FlowLayout.LEFT, 14, 8));

        JLabel searchLbl = Theme.label("Search Query:");
        searchLbl.setFont(Theme.FONT_BOLD);

        searchInput = new JTextField(22);
        Theme.styleField(searchInput);
        searchInput.setToolTipText("Enter Student ID, Name, Department, or Email");

        JButton searchBtn = Theme.button("🔍 Search");
        JButton resetBtn = Theme.secondaryButton("Reset");

        searchBtn.addActionListener(e -> performSearch());
        resetBtn.addActionListener(e -> {
            searchInput.setText("");
            loadAllRecords();
        });

        // Press Enter to search
        searchInput.addActionListener(e -> performSearch());

        resultStatusLabel = Theme.mutedLabel("Enter query and click Search");

        searchBarCard.add(searchLbl);
        searchBarCard.add(searchInput);
        searchBarCard.add(searchBtn);
        searchBarCard.add(resetBtn);
        searchBarCard.add(Box.createHorizontalStrut(10));
        searchBarCard.add(resultStatusLabel);

        topContainer.add(searchBarCard, BorderLayout.SOUTH);
        add(topContainer, BorderLayout.NORTH);

        // 2. Center: Results Table
        tableModel = new DefaultTableModel(COLUMNS, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };

        resultsTable = new JTable(tableModel);
        Theme.styleTable(resultsTable);

        resultsTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && resultsTable.getSelectedRow() != -1) {
                int row = resultsTable.getSelectedRow();
                int studentId = (int) tableModel.getValueAt(row, 0);
                showStudentPreview(studentId);
            }
        });

        JScrollPane scrollPane = new JScrollPane(resultsTable);
        Theme.styleScrollPane(scrollPane);

        // 3. Bottom: Selected Record Preview Card & Delete Button
        JPanel bottomPanel = Theme.cardPanel();
        bottomPanel.setLayout(new BorderLayout(14, 14));

        JLabel previewHeading = Theme.sectionLabel("Selected Student Details Preview:");
        bottomPanel.add(previewHeading, BorderLayout.NORTH);

        JPanel detailsGrid = new JPanel(new GridLayout(2, 4, 12, 8));
        detailsGrid.setBackground(Theme.CHARCOAL);

        previewIdVal = createDetailLabel("—");
        previewNameVal = createDetailLabel("—");
        previewDobVal = createDetailLabel("—");
        previewGenderVal = createDetailLabel("—");
        previewDeptVal = createDetailLabel("—");
        previewEmailVal = createDetailLabel("—");
        previewPhoneVal = createDetailLabel("—");

        addDetailField(detailsGrid, "Student ID:", previewIdVal);
        addDetailField(detailsGrid, "Full Name:", previewNameVal);
        addDetailField(detailsGrid, "DOB:", previewDobVal);
        addDetailField(detailsGrid, "Gender:", previewGenderVal);
        addDetailField(detailsGrid, "Department:", previewDeptVal);
        addDetailField(detailsGrid, "Email:", previewEmailVal);
        addDetailField(detailsGrid, "Phone:", previewPhoneVal);

        bottomPanel.add(detailsGrid, BorderLayout.CENTER);

        // Delete button
        JPanel btnBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 6));
        btnBar.setBackground(Theme.CHARCOAL);

        deleteBtn = Theme.dangerButton("🗑 Delete Selected Student");
        deleteBtn.setEnabled(false);
        deleteBtn.addActionListener(e -> onDeleteStudent());

        btnBar.add(deleteBtn);
        bottomPanel.add(btnBar, BorderLayout.SOUTH);

        // Combine center table and bottom preview
        JPanel centerContainer = new JPanel(new BorderLayout(14, 14));
        centerContainer.setBackground(Theme.BLACK);
        centerContainer.add(scrollPane, BorderLayout.CENTER);
        centerContainer.add(bottomPanel, BorderLayout.SOUTH);

        add(centerContainer, BorderLayout.CENTER);

        // Initial load
        loadAllRecords();
    }

    private JLabel createDetailLabel(String initialText) {
        JLabel l = new JLabel(initialText);
        l.setFont(Theme.FONT_BODY);
        l.setForeground(Theme.TEXT);
        return l;
    }

    private void addDetailField(JPanel panel, String title, JLabel valueLabel) {
        JPanel container = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        container.setBackground(Theme.CHARCOAL);
        JLabel titleLbl = Theme.mutedLabel(title);
        titleLbl.setFont(Theme.FONT_BOLD);
        container.add(titleLbl);
        container.add(valueLabel);
        panel.add(container);
    }

    public void loadAllRecords() {
        tableModel.setRowCount(0);
        clearPreview();
        try {
            List<Student> list = studentDAO.getAllStudents();
            for (Student s : list) {
                tableModel.addRow(new Object[]{
                    s.getId(),
                    s.getName(),
                    s.getDob() != null ? s.getDob().toString() : "N/A",
                    s.getGender(),
                    s.getDepartmentName() != null ? s.getDepartmentName() : "Dept " + s.getDepartmentId(),
                    s.getEmail(),
                    s.getPhone()
                });
            }
            resultStatusLabel.setText("Showing all " + list.size() + " student records.");
            resultStatusLabel.setForeground(Theme.ACCENT);
        } catch (SQLException e) {
            resultStatusLabel.setText("⚠ Database Error: " + e.getMessage());
            resultStatusLabel.setForeground(Theme.ERROR);
        }
    }

    private void performSearch() {
        String keyword = searchInput.getText().trim();
        tableModel.setRowCount(0);
        clearPreview();

        try {
            List<Student> results = studentDAO.searchStudents(keyword);
            for (Student s : results) {
                tableModel.addRow(new Object[]{
                    s.getId(),
                    s.getName(),
                    s.getDob() != null ? s.getDob().toString() : "N/A",
                    s.getGender(),
                    s.getDepartmentName() != null ? s.getDepartmentName() : "Dept " + s.getDepartmentId(),
                    s.getEmail(),
                    s.getPhone()
                });
            }

            if (results.isEmpty()) {
                resultStatusLabel.setText("No students found matching '" + keyword + "'.");
                resultStatusLabel.setForeground(Theme.ERROR);
            } else {
                resultStatusLabel.setText("Found " + results.size() + " matching record(s).");
                resultStatusLabel.setForeground(Theme.SUCCESS);
            }
        } catch (SQLException e) {
            resultStatusLabel.setText("⚠ Error during search: " + e.getMessage());
            resultStatusLabel.setForeground(Theme.ERROR);
        }
    }

    private void showStudentPreview(int studentId) {
        try {
            selectedStudent = studentDAO.getStudentById(studentId);
            if (selectedStudent != null) {
                previewIdVal.setText(String.valueOf(selectedStudent.getId()));
                previewNameVal.setText(selectedStudent.getName());
                previewDobVal.setText(selectedStudent.getDob() != null ? selectedStudent.getDob().toString() : "N/A");
                previewGenderVal.setText(selectedStudent.getGender());
                previewDeptVal.setText(selectedStudent.getDepartmentName() != null ? selectedStudent.getDepartmentName() : "Dept " + selectedStudent.getDepartmentId());
                previewEmailVal.setText(selectedStudent.getEmail());
                previewPhoneVal.setText(selectedStudent.getPhone());
                deleteBtn.setEnabled(true);
            }
        } catch (SQLException e) {
            resultStatusLabel.setText("⚠ Error fetching student preview: " + e.getMessage());
            resultStatusLabel.setForeground(Theme.ERROR);
        }
    }

    private void clearPreview() {
        selectedStudent = null;
        previewIdVal.setText("—");
        previewNameVal.setText("—");
        previewDobVal.setText("—");
        previewGenderVal.setText("—");
        previewDeptVal.setText("—");
        previewEmailVal.setText("—");
        previewPhoneVal.setText("—");
        deleteBtn.setEnabled(false);
    }

    private void onDeleteStudent() {
        if (selectedStudent == null) return;

        // Confirmation Prompt (Requirement FR7 & Non-Functional: Confirm before delete)
        String confirmMessage = "Are you sure you want to permanently delete this student?\n\n" +
                                "Student ID: " + selectedStudent.getId() + "\n" +
                                "Full Name:  " + selectedStudent.getName() + "\n" +
                                "Department: " + selectedStudent.getDepartmentName() + "\n\n" +
                                "⚠ This action cannot be undone!";

        boolean confirmed = Theme.showConfirm(this, confirmMessage, "Confirm Student Deletion");
        if (!confirmed) return;

        try {
            boolean deleted = studentDAO.deleteStudent(selectedStudent.getId());
            if (deleted) {
                Theme.showInfo(this, "Student " + selectedStudent.getName() + " (ID: " + selectedStudent.getId() + ") was deleted successfully.", "Student Deleted");
                clearPreview();
                if (searchInput.getText().trim().isEmpty()) {
                    loadAllRecords();
                } else {
                    performSearch();
                }
            } else {
                Theme.showError(this, "Could not delete student. Record might not exist.", "Deletion Failed");
            }
        } catch (SQLException ex) {
            Theme.showError(this, "Database Error while deleting student:\n" + ex.getMessage(), "Database Error");
        }
    }
}

