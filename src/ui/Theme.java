package ui;

import java.awt.*;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;

/**
 * Global Theme definition for the Student Management System.
 * Theme: Wine Red (#722F37) & Jet Black (#0D0D0D)
 */
public class Theme {

    // Palette Colors
    public static final Color WINE        = new Color(0x72, 0x2F, 0x37);
    public static final Color WINE_DARK   = new Color(0x4A, 0x0E, 0x1A);
    public static final Color WINE_LIGHT  = new Color(0x9B, 0x3A, 0x47);
    public static final Color BLACK       = new Color(0x0D, 0x0D, 0x0D);
    public static final Color CHARCOAL    = new Color(0x1A, 0x1A, 0x1A);
    public static final Color CHARCOAL_LIGHT = new Color(0x22, 0x22, 0x22);
    public static final Color FIELD_BG    = new Color(0x26, 0x26, 0x26);
    public static final Color FIELD_BORDER= new Color(0x4A, 0x4A, 0x4A);
    public static final Color TEXT        = new Color(0xF5, 0xF0, 0xF0);
    public static final Color TEXT_MUTED  = new Color(0xA8, 0xA0, 0xA0);
    public static final Color ACCENT      = new Color(0xC9, 0xA2, 0x7E); // Rose Gold
    public static final Color SUCCESS     = new Color(0x4C, 0xAF, 0x6A);
    public static final Color ERROR       = new Color(0xE0, 0x43, 0x4F);

    // Typography
    public static final Font FONT_TITLE   = new Font("Segoe UI", Font.BOLD, 26);
    public static final Font FONT_SUBTITLE= new Font("Segoe UI", Font.BOLD, 18);
    public static final Font FONT_SECTION = new Font("Segoe UI", Font.BOLD, 15);
    public static final Font FONT_BOLD    = new Font("Segoe UI", Font.BOLD, 14);
    public static final Font FONT_BODY    = new Font("Segoe UI", Font.PLAIN, 14);
    public static final Font FONT_SMALL   = new Font("Segoe UI", Font.PLAIN, 12);
    public static final Font FONT_ICON    = new Font("Segoe UI Symbol", Font.PLAIN, 14);

    /**
     * Creates a Wine Red primary action button.
     */
    public static JButton button(String text) {
        return createButton(text, WINE, WINE_LIGHT, WINE_DARK, Color.WHITE);
    }

    public static JButton primaryButton(String text) {
        return button(text);
    }

    /**
     * Creates a Bright Red danger/delete button.
     */
    public static JButton dangerButton(String text) {
        return createButton(text, ERROR, new Color(0xF0, 0x54, 0x60), new Color(0xB8, 0x28, 0x33), Color.WHITE);
    }

    /**
     * Creates a secondary Charcoal button.
     */
    public static JButton secondaryButton(String text) {
        return createButton(text, new Color(0x33, 0x33, 0x33), new Color(0x44, 0x44, 0x44), new Color(0x22, 0x22, 0x22), TEXT);
    }

    /**
     * Creates a Success Green button.
     */
    public static JButton successButton(String text) {
        return createButton(text, SUCCESS, new Color(0x5C, 0xBF, 0x7A), new Color(0x3C, 0x8F, 0x5A), Color.WHITE);
    }

    private static JButton createButton(String text, Color baseColor, Color hoverColor, Color pressedColor, Color textColor) {
        JButton b = new JButton(text);
        b.setBackground(baseColor);
        b.setForeground(textColor);
        b.setFont(FONT_BOLD);
        b.setFocusPainted(false);
        b.setBorder(new CompoundBorder(new LineBorder(baseColor.brighter(), 1, true), new EmptyBorder(9, 20, 9, 20)));
        b.setCursor(new Cursor(Cursor.HAND_CURSOR));

        b.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (b.isEnabled()) b.setBackground(hoverColor);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (b.isEnabled()) b.setBackground(baseColor);
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (b.isEnabled()) b.setBackground(pressedColor);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (b.isEnabled()) {
                    b.setBackground(b.contains(e.getPoint()) ? hoverColor : baseColor);
                }
            }
        });
        return b;
    }

    /**
     * Styles standard text fields.
     */
    public static void styleField(JTextField f) {
        f.setBackground(FIELD_BG);
        f.setForeground(TEXT);
        f.setCaretColor(TEXT);
        f.setFont(FONT_BODY);

        Border normalBorder = new CompoundBorder(
            new LineBorder(FIELD_BORDER, 1, true),
            new EmptyBorder(8, 12, 8, 12)
        );
        Border focusedBorder = new CompoundBorder(
            new LineBorder(WINE, 2, true),
            new EmptyBorder(7, 11, 7, 11)
        );

        f.setBorder(normalBorder);

        f.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                f.setBorder(focusedBorder);
            }

            @Override
            public void focusLost(FocusEvent e) {
                f.setBorder(normalBorder);
            }
        });
    }

    /**
     * Styles combo boxes.
     */
    public static void styleComboBox(JComboBox<?> box) {
        box.setBackground(FIELD_BG);
        box.setForeground(TEXT);
        box.setFont(FONT_BODY);
        box.setCursor(new Cursor(Cursor.HAND_CURSOR));
        box.setBorder(new CompoundBorder(
            new LineBorder(FIELD_BORDER, 1, true),
            new EmptyBorder(4, 6, 4, 6)
        ));

        box.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel l = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                l.setFont(FONT_BODY);
                l.setBorder(new EmptyBorder(6, 10, 6, 10));
                if (isSelected) {
                    l.setBackground(WINE);
                    l.setForeground(Color.WHITE);
                } else {
                    l.setBackground(FIELD_BG);
                    l.setForeground(TEXT);
                }
                return l;
            }
        });
    }

    public static JLabel label(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(TEXT);
        l.setFont(FONT_BODY);
        return l;
    }

    public static JLabel mutedLabel(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(TEXT_MUTED);
        l.setFont(FONT_SMALL);
        return l;
    }

    public static JLabel sectionLabel(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(ACCENT);
        l.setFont(FONT_SECTION);
        return l;
    }

    public static JLabel titleLabel(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(ACCENT);
        l.setFont(FONT_TITLE);
        return l;
    }

    /**
     * Creates a styled Card Panel in Charcoal with subtle border.
     */
    public static JPanel cardPanel() {
        JPanel p = new JPanel();
        p.setBackground(CHARCOAL);
        p.setBorder(new CompoundBorder(
            new LineBorder(new Color(0x2E, 0x2E, 0x2E), 1, true),
            new EmptyBorder(16, 20, 16, 20)
        ));
        return p;
    }

    /**
     * Styles JTable according to the Wine Red & Black theme specifications.
     */
    public static void styleTable(JTable t) {
        t.setBackground(CHARCOAL);
        t.setForeground(TEXT);
        t.setFont(FONT_BODY);
        t.setRowHeight(34);
        t.setGridColor(new Color(0x2D, 0x2D, 0x2D));
        t.setShowVerticalLines(false);
        t.setShowHorizontalLines(true);
        t.setSelectionBackground(WINE);
        t.setSelectionForeground(Color.WHITE);
        t.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        t.setFillsViewportHeight(true);

        // Header Styling
        JTableHeader header = t.getTableHeader();
        header.setBackground(WINE);
        header.setForeground(Color.WHITE);
        header.setFont(FONT_BOLD);
        header.setPreferredSize(new Dimension(header.getWidth(), 38));
        header.setReorderingAllowed(false);

        // Alternating row renderer
        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                setBorder(new EmptyBorder(4, 10, 4, 10));

                if (isSelected) {
                    c.setBackground(WINE);
                    c.setForeground(Color.WHITE);
                } else {
                    if (row % 2 == 0) {
                        c.setBackground(CHARCOAL);
                    } else {
                        c.setBackground(CHARCOAL_LIGHT);
                    }
                    c.setForeground(TEXT);
                }
                return c;
            }
        };

        for (int i = 0; i < t.getColumnCount(); i++) {
            t.getColumnModel().getColumn(i).setCellRenderer(renderer);
        }
    }

    /**
     * Styles a JScrollPane to match dark theme.
     */
    public static void styleScrollPane(JScrollPane sp) {
        sp.setBackground(CHARCOAL);
        sp.getViewport().setBackground(CHARCOAL);
        sp.setBorder(new LineBorder(new Color(0x33, 0x33, 0x33), 1, true));
        sp.getVerticalScrollBar().setUnitIncrement(16);
    }

    /**
     * Shows a styled Info message dialog.
     */
    public static void showInfo(Component parent, String message, String title) {
        JOptionPane.showMessageDialog(parent, message, title, JOptionPane.INFORMATION_MESSAGE);
    }

    /**
     * Shows a styled Error message dialog.
     */
    public static void showError(Component parent, String message, String title) {
        JOptionPane.showMessageDialog(parent, message, title, JOptionPane.ERROR_MESSAGE);
    }

    /**
     * Shows a styled Confirmation dialog.
     */
    public static boolean showConfirm(Component parent, String message, String title) {
        int choice = JOptionPane.showConfirmDialog(
            parent,
            message,
            title,
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE
        );
        return choice == JOptionPane.YES_OPTION;
    }
}
