package com.smartsplit.ui;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Centralised visual constants and factory helpers.
 * All frames reference this class so the app looks consistent.
 */
public final class UITheme {

    // ── Palette ───────────────────────────────────────────────────────────
    public static final Color PRIMARY       = new Color(0x5C6BC0);   // indigo
    public static final Color PRIMARY_DARK  = new Color(0x3949AB);
    public static final Color ACCENT        = new Color(0x26A69A);   // teal
    public static final Color DANGER        = new Color(0xE53935);   // red
    public static final Color BG            = new Color(0xF5F5F5);
    public static final Color SURFACE       = Color.WHITE;
    public static final Color TEXT_PRIMARY  = new Color(0x212121);
    public static final Color TEXT_MUTED    = new Color(0x757575);
    public static final Color BORDER_COLOR  = new Color(0xE0E0E0);

    // ── Typography ────────────────────────────────────────────────────────
    public static final Font FONT_TITLE   = new Font("SansSerif", Font.BOLD,  22);
    public static final Font FONT_HEADING = new Font("SansSerif", Font.BOLD,  15);
    public static final Font FONT_BODY    = new Font("SansSerif", Font.PLAIN, 13);
    public static final Font FONT_SMALL   = new Font("SansSerif", Font.PLAIN, 11);
    public static final Font FONT_MONO    = new Font("Monospaced", Font.PLAIN, 12);

    // ── Spacing ───────────────────────────────────────────────────────────
    public static final int  PAD   = 16;
    public static final int  PAD_S = 8;

    private UITheme() {}

    // ── Buttons ───────────────────────────────────────────────────────────

    public static JButton primaryButton(String text) {
        JButton b = new JButton(text);
        b.setFont(FONT_HEADING);
        b.setBackground(PRIMARY);
        b.setForeground(Color.WHITE);
        b.setFocusPainted(false);
        b.setBorderPainted(false);
        b.setOpaque(true);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.setPreferredSize(new Dimension(b.getPreferredSize().width, 36));
        b.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent e) { b.setBackground(PRIMARY_DARK); }
            public void mouseExited (java.awt.event.MouseEvent e) { b.setBackground(PRIMARY); }
        });
        return b;
    }

    public static JButton dangerButton(String text) {
        JButton b = primaryButton(text);
        b.setBackground(DANGER);
        b.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent e) { b.setBackground(DANGER.darker()); }
            public void mouseExited (java.awt.event.MouseEvent e) { b.setBackground(DANGER); }
        });
        return b;
    }

    public static JButton linkButton(String text) {
        JButton b = new JButton("<html><u>" + text + "</u></html>");
        b.setFont(FONT_BODY);
        b.setForeground(PRIMARY);
        b.setBorderPainted(false);
        b.setContentAreaFilled(false);
        b.setFocusPainted(false);
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }

    // ── Fields ────────────────────────────────────────────────────────────

    public static JTextField textField() {
        JTextField f = new JTextField();
        f.setFont(FONT_BODY);
        f.setBorder(fieldBorder());
        f.setPreferredSize(new Dimension(240, 32));
        return f;
    }

    public static JPasswordField passwordField() {
        JPasswordField f = new JPasswordField();
        f.setFont(FONT_BODY);
        f.setBorder(fieldBorder());
        f.setPreferredSize(new Dimension(240, 32));
        return f;
    }

    private static Border fieldBorder() {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR),
                new EmptyBorder(4, 8, 4, 8));
    }

    // ── Labels ────────────────────────────────────────────────────────────

    public static JLabel heading(String text) {
        JLabel l = new JLabel(text);
        l.setFont(FONT_HEADING);
        l.setForeground(TEXT_PRIMARY);
        return l;
    }

    public static JLabel muted(String text) {
        JLabel l = new JLabel(text);
        l.setFont(FONT_SMALL);
        l.setForeground(TEXT_MUTED);
        return l;
    }

    // ── Panels ────────────────────────────────────────────────────────────

    /** White card panel with a thin border and inner padding. */
    public static JPanel card() {
        JPanel p = new JPanel();
        p.setBackground(SURFACE);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_COLOR),
                new EmptyBorder(PAD, PAD, PAD, PAD)));
        return p;
    }

    // ── Error display ─────────────────────────────────────────────────────

    public static void showError(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    public static void showInfo(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "SmartSplit", JOptionPane.INFORMATION_MESSAGE);
    }

    public static boolean confirm(Component parent, String message) {
        return JOptionPane.showConfirmDialog(parent, message, "Confirm",
                JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
    }

    // ── LookAndFeel ───────────────────────────────────────────────────────

    public static void applyLookAndFeel() {
        try { UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName()); }
        catch (Exception ignored) {}
        UIManager.put("Button.arc", 6);
        UIManager.put("Panel.background", BG);
    }
}
