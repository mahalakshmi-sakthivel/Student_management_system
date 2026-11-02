package ui;

import dao.StudentDAO;
import model.Student;

import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

/**
 * Panel to view all student records in an interactive, styled JTable.
 * Features: Refresh, live filter, record count, CSV export, and double-click to edit.
 */
public class ViewStudentPanel extends JPanel {

    private final StudentDAO studentDAO = new StudentDAO();
    private JTable table;
    private DefaultTableModel tableModel;
    private TableRowSorter<DefaultTableModel> sorter;
    private JLabel countLabel;
    private JTextField filterField;
    private Consumer<Integer> onStudentSelectCallback;

    private static final String[] COLUMNS = {
        "Student ID", "Full Name", "Date of Birth", "Gender", "Department", "Email", "Phone"
    };

    public ViewStudentPanel(Consumer<Integer> onStudentSelectCallback) {
        this.onStudentSelectCallback = onStudentSelectCallback;
        setLayout(new BorderLayout(16, 16));
        setBackground(Theme.BLACK);
        setBorder(new EmptyBorder(24, 28, 24, 28));
        initUI();
    }

    private void initUI() {
        // Header Toolbar
        JPanel topPanel = new JPanel(new BorderLayout(12, 12));
        topPanel.setBackground(Theme.BLACK);

        // Title and description
        JPanel titlePanel = new JPanel(new BorderLayout());
        titlePanel.setBackground(Theme.BLACK);
        JLabel title = Theme.titleLabel("📋 Student Directory");
        JLabel desc = Theme.mutedLabel("View, filter, sort, and export all registered student records.");
        titlePanel.add(title, BorderLayout.NORTH);
        titlePanel.add(desc, BorderLayout.SOUTH);
        topPanel.add(titlePanel, BorderLayout.WEST);

        // Right side: Quick Search + Count Badge + Action Buttons
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actions.setBackground(Theme.BLACK);

        countLabel = new JLabel("Total: 0 records");
        countLabel.setFont(Theme.FONT_BOLD);
        countLabel.setForeground(Theme.ACCENT);
        countLabel.setBorder(new EmptyBorder(6, 10, 6, 10));

        filterField = new JTextField(14);
        Theme.styleField(filterField);
        filterField.setToolTipText("Type to filter table instantly...");

        JLabel filterIcon = new JLabel("🔍 Filter: ");
        filterIcon.setFont(Theme.FONT_BODY);
        filterIcon.setForeground(Theme.TEXT_MUTED);

        JButton refreshBtn = Theme.button("↻ Refresh");
        JButton exportBtn = Theme.secondaryButton("⬇ Export CSV");

        refreshBtn.addActionListener(e -> loadStudents());
        exportBtn.addActionListener(e -> exportToCSV());

        // Live filtering
        filterField.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { filter(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { filter(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { filter(); }
            private void filter() {
                String text = filterField.getText().trim();
                if (text.isEmpty()) {
                    sorter.setRowFilter(null);
                } else {
                    sorter.setRowFilter(RowFilter.regexFilter("(?i)" + PatternQuote(text)));
                }
                updateCount();
            }
        });

        actions.add(countLabel);
        actions.add(filterIcon);
        actions.add(filterField);
        actions.add(refreshBtn);
        actions.add(exportBtn);

        topPanel.add(actions, BorderLayout.EAST);
        add(topPanel, BorderLayout.NORTH);

        // Table Setup
        tableModel = new DefaultTableModel(COLUMNS, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false; // read-only table
            }

            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 0) return Integer.class;
                return String.class;
            }
        };

        table = new JTable(tableModel);
        sorter = new TableRowSorter<>(tableModel);
        table.setRowSorter(sorter);
        Theme.styleTable(table);

        // Double-click row listener -> navigates to Update panel
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2 && table.getSelectedRow() != -1) {
                    int modelRow = table.convertRowIndexToModel(table.getSelectedRow());
                    int studentId = (int) tableModel.getValueAt(modelRow, 0);
                    if (onStudentSelectCallback != null) {
                        onStudentSelectCallback.accept(studentId);
                    }
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(table);
        Theme.styleScrollPane(scrollPane);
        add(scrollPane, BorderLayout.CENTER);

        // Footer Hint
        JLabel hintLabel = Theme.mutedLabel("💡 Tip: Double-click any row to edit that student in the Update panel. Click any column header to sort.");
        add(hintLabel, BorderLayout.SOUTH);

        // Load data on start
        loadStudents();
    }

    public void loadStudents() {
        tableModel.setRowCount(0);
        try {
            List<Student> students = studentDAO.getAllStudents();
            for (Student s : students) {
                tableModel.addRow(new Object[]{
                    s.getId(),
                    s.getName(),
                    s.getDob() != null ? s.getDob().toString() : "N/A",
                    s.getGender(),
                    s.getDepartmentName() != null ? s.getDepartmentName() : ("Dept " + s.getDepartmentId()),
                    s.getEmail(),
                    s.getPhone()
                });
            }
            updateCount();
        } catch (SQLException e) {
            Theme.showError(this, "Failed to load students from database:\n" + e.getMessage(), "Database Error");
            countLabel.setText("Database Disconnected ✗");
            countLabel.setForeground(Theme.ERROR);
        }
    }

    private void updateCount() {
        int visible = table.getRowCount();
        int total = tableModel.getRowCount();
        if (visible == total) {
            countLabel.setText("Total: " + total + " records");
        } else {
            countLabel.setText("Showing " + visible + " of " + total);
        }
    }

    private void exportToCSV() {
        if (tableModel.getRowCount() == 0) {
            Theme.showInfo(this, "No records to export.", "Export CSV");
            return;
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Save Students CSV");
        fileChooser.setSelectedFile(new File("students_export.csv"));

        int userSelection = fileChooser.showSaveDialog(this);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();
            if (!fileToSave.getName().toLowerCase().endsWith(".csv")) {
                fileToSave = new File(fileToSave.getAbsolutePath() + ".csv");
            }

            try (FileWriter fw = new FileWriter(fileToSave)) {
                // Header
                for (int i = 0; i < COLUMNS.length; i++) {
                    fw.write("\"" + COLUMNS[i] + "\"");
                    if (i < COLUMNS.length - 1) fw.write(",");
                }
                fw.write("\n");

                // Rows
                for (int r = 0; r < tableModel.getRowCount(); r++) {
                    for (int c = 0; c < tableModel.getColumnCount(); c++) {
                        Object val = tableModel.getValueAt(r, c);
                        fw.write("\"" + (val != null ? val.toString().replace("\"", "\"\"") : "") + "\"");
                        if (c < tableModel.getColumnCount() - 1) fw.write(",");
                    }
                    fw.write("\n");
                }

                Theme.showInfo(this, "Student records successfully exported to:\n" + fileToSave.getAbsolutePath(), "Export Successful");
            } catch (IOException e) {
                Theme.showError(this, "Failed to save CSV file:\n" + e.getMessage(), "Export Error");
            }
        }
    }

    private String PatternQuote(String s) {
        return java.util.regex.Pattern.quote(s);
    }
}

