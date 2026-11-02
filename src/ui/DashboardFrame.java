package ui;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Map;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

/**
 * Main Application Dashboard Frame.
 * Layout:
 * - Header: Wine Red banner with Rose Gold branding, user badge & DB settings.
 * - Sidebar: Deep Burgundy (#4A0E1A) navigation with active item indicators.
 * - Center: Jet Black (#0D0D0D) CardLayout hosting all modules.
 */
public class DashboardFrame extends JFrame {

    private final String currentUsername;

    private JPanel centerCards;
    private CardLayout cardLayout;

    // Sub-panels
    private OverviewPanel overviewPanel;
    private AddStudentPanel addStudentPanel;
    private ViewStudentPanel viewStudentPanel;
    private UpdateStudentPanel updateStudentPanel;
    private SearchDeletePanel searchDeletePanel;

    // Sidebar navigation tracking
    private final Map<String, JButton> navButtons = new HashMap<>();
    private String currentActiveKey = "OVERVIEW";

    public DashboardFrame(String username) {
        this.currentUsername = (username != null && !username.isEmpty()) ? username : "Administrator";
        setTitle("🎓 Student Management System - Dashboard [" + this.currentUsername + "]");
        setSize(1180, 760);
        setMinimumSize(new Dimension(980, 640));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        initUI();
    }

    private void initUI() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Theme.BLACK);

        // 1. Header Bar (Wine Red)
        JPanel headerBar = createHeaderBar();
        root.add(headerBar, BorderLayout.NORTH);

        // 2. Left Sidebar (Deep Burgundy)
        JPanel sidebar = createSidebar();
        root.add(sidebar, BorderLayout.WEST);

        // 3. Center Content (Jet Black with CardLayout)
        cardLayout = new CardLayout();
        centerCards = new JPanel(cardLayout);
        centerCards.setBackground(Theme.BLACK);

        overviewPanel = new OverviewPanel(this::showCard);
        addStudentPanel = new AddStudentPanel();
        viewStudentPanel = new ViewStudentPanel(this::onEditStudentRequested);
        updateStudentPanel = new UpdateStudentPanel();
        searchDeletePanel = new SearchDeletePanel();

        centerCards.add(overviewPanel, "OVERVIEW");
        centerCards.add(addStudentPanel, "ADD");
        centerCards.add(viewStudentPanel, "VIEW");
        centerCards.add(updateStudentPanel, "UPDATE");
        centerCards.add(searchDeletePanel, "SEARCH_DELETE");

        root.add(centerCards, BorderLayout.CENTER);
        setContentPane(root);

        // Set initial view
        showCard("OVERVIEW");
    }

    private JPanel createHeaderBar() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(Theme.WINE);
        header.setPreferredSize(new Dimension(getWidth(), 56));
        header.setBorder(new EmptyBorder(8, 20, 8, 20));

        // Left Branding
        JPanel brandPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 2));
        brandPanel.setOpaque(false);

        JLabel logo = new JLabel("🎓");
        logo.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 24));

        JLabel title = new JLabel("STUDENT MANAGEMENT SYSTEM");
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(Theme.ACCENT);

        brandPanel.add(logo);
        brandPanel.add(title);
        header.add(brandPanel, BorderLayout.WEST);

        // Right User status & DB controls
        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 2));
        rightPanel.setOpaque(false);

        JLabel userBadge = new JLabel("👤 Admin: " + currentUsername);
        userBadge.setFont(Theme.FONT_BOLD);
        userBadge.setForeground(Color.WHITE);

        JButton dbBtn = new JButton("⚙ DB Config");
        dbBtn.setFont(Theme.FONT_SMALL);
        dbBtn.setBackground(Theme.WINE_DARK);
        dbBtn.setForeground(Theme.ACCENT);
        dbBtn.setFocusPainted(false);
        dbBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        dbBtn.setBorder(new LineBorder(Theme.ACCENT, 1, true));
        dbBtn.addActionListener(e -> {
            DatabaseConfigDialog dialog = new DatabaseConfigDialog(this);
            dialog.setVisible(true);
            refreshAllPanels();
        });

        JButton logoutBtn = new JButton("🚪 Logout");
        logoutBtn.setFont(Theme.FONT_SMALL);
        logoutBtn.setBackground(Theme.WINE_DARK);
        logoutBtn.setForeground(Color.WHITE);
        logoutBtn.setFocusPainted(false);
        logoutBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        logoutBtn.setBorder(new LineBorder(new Color(0x6A, 0x1E, 0x27), 1, true));
        logoutBtn.addActionListener(e -> performLogout());

        rightPanel.add(userBadge);
        rightPanel.add(dbBtn);
        rightPanel.add(logoutBtn);

        header.add(rightPanel, BorderLayout.EAST);
        return header;
    }

    private JPanel createSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(Theme.WINE_DARK);
        sidebar.setPreferredSize(new Dimension(240, getHeight()));
        sidebar.setBorder(new EmptyBorder(20, 12, 20, 12));

        JLabel navHeading = new JLabel("MAIN NAVIGATION");
        navHeading.setFont(new Font("Segoe UI", Font.BOLD, 11));
        navHeading.setForeground(Theme.ACCENT);
        navHeading.setBorder(new EmptyBorder(0, 10, 12, 0));
        sidebar.add(navHeading);

        addNavButton(sidebar, "OVERVIEW", "📊  Dashboard Overview");
        addNavButton(sidebar, "ADD",      "➕  Add Student");
        addNavButton(sidebar, "VIEW",     "📋  View All Students");
        addNavButton(sidebar, "UPDATE",   "✏️  Update Student");
        addNavButton(sidebar, "SEARCH_DELETE", "🔍  Search & Delete");

        sidebar.add(Box.createVerticalGlue());

        // Sidebar Footer
        JPanel footer = new JPanel(new GridLayout(2, 1, 4, 4));
        footer.setBackground(Theme.WINE_DARK);
        JLabel verLbl = Theme.mutedLabel("Version 1.0 (Swing + MySQL)");
        verLbl.setForeground(new Color(0xD0, 0xB0, 0xB0));
        JLabel themeLbl = Theme.mutedLabel("Wine Red & Black Theme");
        themeLbl.setForeground(new Color(0xD0, 0xB0, 0xB0));
        footer.add(verLbl);
        footer.add(themeLbl);
        sidebar.add(footer);

        return sidebar;
    }

    private void addNavButton(JPanel sidebar, String cardKey, String text) {
        JButton btn = new JButton(text);
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        btn.setFont(Theme.FONT_BOLD);
        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(10, 16, 10, 16));

        styleNavButton(btn, false);

        btn.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (!cardKey.equals(currentActiveKey)) {
                    btn.setBackground(Theme.WINE_LIGHT);
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (!cardKey.equals(currentActiveKey)) {
                    btn.setBackground(Theme.WINE_DARK);
                }
            }
        });

        btn.addActionListener(e -> showCard(cardKey));

        navButtons.put(cardKey, btn);
        sidebar.add(btn);
        sidebar.add(Box.createVerticalStrut(6));
    }

    private void styleNavButton(JButton btn, boolean isActive) {
        if (isActive) {
            btn.setBackground(Theme.WINE);
            btn.setForeground(Color.WHITE);
            btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 4, 0, 0, Theme.ACCENT),
                new EmptyBorder(10, 12, 10, 16)
            ));
        } else {
            btn.setBackground(Theme.WINE_DARK);
            btn.setForeground(Theme.TEXT);
            btn.setBorder(new EmptyBorder(10, 16, 10, 16));
        }
    }

    public void showCard(String key) {
        currentActiveKey = key;
        cardLayout.show(centerCards, key);

        // Update button active styles
        for (Map.Entry<String, JButton> entry : navButtons.entrySet()) {
            styleNavButton(entry.getValue(), entry.getKey().equals(key));
        }

        // Panel refresh on navigate
        switch (key) {
            case "OVERVIEW":
                overviewPanel.loadStatistics();
                break;
            case "ADD":
                addStudentPanel.loadDepartments();
                break;
            case "VIEW":
                viewStudentPanel.loadStudents();
                break;
            case "UPDATE":
                updateStudentPanel.loadDepartments();
                updateStudentPanel.loadStudentList();
                break;
            case "SEARCH_DELETE":
                searchDeletePanel.loadAllRecords();
                break;
        }
    }

    public void onEditStudentRequested(int studentId) {
        showCard("UPDATE");
        updateStudentPanel.loadStudentForEdit(studentId);
    }

    private void refreshAllPanels() {
        overviewPanel.loadStatistics();
        addStudentPanel.loadDepartments();
        viewStudentPanel.loadStudents();
        updateStudentPanel.loadDepartments();
        updateStudentPanel.loadStudentList();
        searchDeletePanel.loadAllRecords();
    }

    private void performLogout() {
        boolean confirm = Theme.showConfirm(
            this,
            "Are you sure you want to log out from the Administrator session?",
            "Confirm Logout"
        );
        if (confirm) {
            LoginFrame login = new LoginFrame();
            login.setVisible(true);
            dispose();
        }
    }
}

