package ui;

import dao.DepartmentDAO;
import dao.StudentDAO;
import db.DBConnection;

import java.awt.*;
import java.sql.SQLException;
import java.util.Map;
import java.util.function.Consumer;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

/**
 * Overview / Analytics panel displayed on Dashboard home.
 * Shows key system statistics, quick actions, and database connection status.
 */
public class OverviewPanel extends JPanel {

    private final StudentDAO studentDAO = new StudentDAO();
    private final DepartmentDAO departmentDAO = new DepartmentDAO();
    private final Consumer<String> navigationHandler;

    private JLabel totalStudentsVal;
    private JLabel totalDeptsVal;
    private JLabel maleCountVal;
    private JLabel femaleCountVal;
    private JLabel dbStatusVal;

    public OverviewPanel(Consumer<String> navigationHandler) {
        this.navigationHandler = navigationHandler;
        setLayout(new BorderLayout(20, 20));
        setBackground(Theme.BLACK);
        setBorder(new EmptyBorder(24, 28, 24, 28));
        initUI();
    }

    private void initUI() {
        // Welcome Header
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Theme.BLACK);

        JLabel title = Theme.titleLabel("📊 Dashboard Overview");
        JLabel desc = Theme.mutedLabel("Welcome to the Student Management System. Here is a summary of current records.");
        header.add(title, BorderLayout.NORTH);
        header.add(desc, BorderLayout.SOUTH);
        add(header, BorderLayout.NORTH);

        // Center: Stat Cards Grid + Quick Actions
        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setBackground(Theme.BLACK);

        // 1. Metric Cards Row
        JPanel cardsGrid = new JPanel(new GridLayout(1, 4, 16, 16));
        cardsGrid.setBackground(Theme.BLACK);
        cardsGrid.setMaximumSize(new Dimension(Integer.MAX_VALUE, 130));

        totalStudentsVal = new JLabel("0");
        totalDeptsVal = new JLabel("0");
        maleCountVal = new JLabel("0");
        femaleCountVal = new JLabel("0");

        cardsGrid.add(createMetricCard("👥 Total Students", totalStudentsVal, Theme.WINE));
        cardsGrid.add(createMetricCard("🏛️ Departments", totalDeptsVal, Theme.ACCENT));
        cardsGrid.add(createMetricCard("👨 Male Students", maleCountVal, new Color(0x3B, 0x82, 0xF6)));
        cardsGrid.add(createMetricCard("👩 Female Students", femaleCountVal, new Color(0xEC, 0x48, 0x99)));

        centerPanel.add(cardsGrid);
        centerPanel.add(Box.createVerticalStrut(24));

        // 2. Middle Row: Quick Action Buttons & System Status Card
        JPanel midRow = new JPanel(new GridLayout(1, 2, 20, 0));
        midRow.setBackground(Theme.BLACK);

        // Quick Actions Card
        JPanel quickActionsCard = Theme.cardPanel();
        quickActionsCard.setLayout(new BorderLayout(12, 12));

        JLabel qaTitle = Theme.sectionLabel("⚡ Quick Actions");
        quickActionsCard.add(qaTitle, BorderLayout.NORTH);

        JPanel btnGrid = new JPanel(new GridLayout(2, 2, 12, 12));
        btnGrid.setBackground(Theme.CHARCOAL);

        JButton addBtn = Theme.button("➕ Add Student");
        JButton viewBtn = Theme.secondaryButton("📋 View All Students");
        JButton searchBtn = Theme.secondaryButton("🔍 Search & Delete");
        JButton refreshBtn = Theme.secondaryButton("↻ Refresh Stats");

        addBtn.addActionListener(e -> navigate("ADD"));
        viewBtn.addActionListener(e -> navigate("VIEW"));
        searchBtn.addActionListener(e -> navigate("SEARCH_DELETE"));
        refreshBtn.addActionListener(e -> loadStatistics());

        btnGrid.add(addBtn);
        btnGrid.add(viewBtn);
        btnGrid.add(searchBtn);
        btnGrid.add(refreshBtn);
        quickActionsCard.add(btnGrid, BorderLayout.CENTER);
        midRow.add(quickActionsCard);

        // System Info Card
        JPanel sysCard = Theme.cardPanel();
        sysCard.setLayout(new BorderLayout(12, 12));

        JLabel sysTitle = Theme.sectionLabel("🖥 System & Database Information");
        sysCard.add(sysTitle, BorderLayout.NORTH);

        JPanel infoList = new JPanel(new GridLayout(4, 1, 6, 6));
        infoList.setBackground(Theme.CHARCOAL);

        dbStatusVal = Theme.label("Database: Checking...");
        JLabel hostLbl = Theme.mutedLabel("Host & Port: " + DBConnection.getHost() + ":" + DBConnection.getPort());
        JLabel dbNameLbl = Theme.mutedLabel("Active DB: " + DBConnection.getDbName());
        JLabel javaLbl = Theme.mutedLabel("Java Runtime: JDK " + System.getProperty("java.version") + " (" + System.getProperty("os.name") + ")");

        infoList.add(dbStatusVal);
        infoList.add(hostLbl);
        infoList.add(dbNameLbl);
        infoList.add(javaLbl);
        sysCard.add(infoList, BorderLayout.CENTER);

        midRow.add(sysCard);
        centerPanel.add(midRow);

        add(centerPanel, BorderLayout.CENTER);

        // Load stats
        loadStatistics();
    }

    private JPanel createMetricCard(String title, JLabel valueLabel, Color accentColor) {
        JPanel card = new JPanel(new BorderLayout(8, 8));
        card.setBackground(Theme.CHARCOAL);
        card.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(new Color(0x2E, 0x2E, 0x2E), 1, true),
            new EmptyBorder(16, 18, 16, 18)
        ));

        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(Theme.FONT_BOLD);
        titleLbl.setForeground(accentColor);

        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 32));
        valueLabel.setForeground(Theme.TEXT);

        card.add(titleLbl, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);
        return card;
    }

    public void loadStatistics() {
        try {
            int studentCount = studentDAO.getStudentCount();
            int deptCount = departmentDAO.getAllDepartments().size();
            Map<String, Integer> genderCounts = studentDAO.getGenderCounts();

            totalStudentsVal.setText(String.valueOf(studentCount));
            totalDeptsVal.setText(String.valueOf(deptCount));

            int males = genderCounts.getOrDefault("Male", 0);
            int females = genderCounts.getOrDefault("Female", 0);

            maleCountVal.setText(String.valueOf(males));
            femaleCountVal.setText(String.valueOf(females));

            dbStatusVal.setText("Database: Connected to MySQL (" + DBConnection.getDbName() + ") ✓");
            dbStatusVal.setForeground(Theme.SUCCESS);

        } catch (SQLException e) {
            totalStudentsVal.setText("—");
            totalDeptsVal.setText("—");
            maleCountVal.setText("—");
            femaleCountVal.setText("—");

            dbStatusVal.setText("Database: Connection Failed ✗ (" + e.getMessage() + ")");
            dbStatusVal.setForeground(Theme.ERROR);
        }
    }

    private void navigate(String cardKey) {
        if (navigationHandler != null) {
            navigationHandler.accept(cardKey);
        }
    }
}

