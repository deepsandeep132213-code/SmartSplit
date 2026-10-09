package com.smartsplit.ui;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/** Factory methods for styled Swing components used throughout SmartSplit. */
public final class UIHelper {

    private UIHelper() {}

    // ── Buttons ───────────────────────────────────────────────────────────

    /** Filled primary-colour button. */
    public static JButton primaryButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(AppFonts.BUTTON);
        btn.setForeground(AppColors.TEXT_ON_DARK);
        btn.setBackground(AppColors.PRIMARY);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setOpaque(true);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setBorder(new EmptyBorder(8, 20, 8, 20));
        btn.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { btn.setBackground(AppColors.PRIMARY_DARK); }
            @Override public void mouseExited(MouseEvent e)  { btn.setBackground(AppColors.PRIMARY); }
        });
        return btn;
    }

    /** Ghost / outline button. */
    public static JButton secondaryButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(AppFonts.BUTTON);
        btn.setForeground(AppColors.PRIMARY);
        btn.setBackground(AppColors.BG_CARD);
        btn.setFocusPainted(false);
        btn.setBorder(new CompoundBorder(new LineBorder(AppColors.PRIMARY, 1, true), new EmptyBorder(7, 19, 7, 19)));
        btn.setOpaque(true);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { btn.setBackground(AppColors.PRIMARY_LIGHT); }
            @Override public void mouseExited(MouseEvent e)  { btn.setBackground(AppColors.BG_CARD); }
        });
        return btn;
    }

    /** Danger / destructive action button. */
    public static JButton dangerButton(String text) {
        JButton btn = primaryButton(text);
        btn.setBackground(AppColors.ERROR);
        btn.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) { btn.setBackground(new Color(0xC62828)); }
            @Override public void mouseExited(MouseEvent e)  { btn.setBackground(AppColors.ERROR); }
        });
        return btn;
    }

    // ── Text fields ───────────────────────────────────────────────────────

    public static JTextField styledTextField(int columns) {
        JTextField tf = new JTextField(columns);
        tf.setFont(AppFonts.BODY);
        tf.setBorder(new CompoundBorder(new LineBorder(AppColors.BORDER, 1, true), new EmptyBorder(6, 10, 6, 10)));
        tf.setBackground(AppColors.INPUT_BG);
        return tf;
    }

    public static JPasswordField styledPasswordField(int columns) {
        JPasswordField pf = new JPasswordField(columns);
        pf.setFont(AppFonts.BODY);
        pf.setBorder(new CompoundBorder(new LineBorder(AppColors.BORDER, 1, true), new EmptyBorder(6, 10, 6, 10)));
        pf.setBackground(AppColors.INPUT_BG);
        return pf;
    }

    // ── Labels ────────────────────────────────────────────────────────────

    public static JLabel heading(String text) {
        JLabel l = new JLabel(text);
        l.setFont(AppFonts.HEADING_LARGE);
        l.setForeground(AppColors.TEXT_PRIMARY);
        return l;
    }

    public static JLabel subheading(String text) {
        JLabel l = new JLabel(text);
        l.setFont(AppFonts.HEADING_MEDIUM);
        l.setForeground(AppColors.TEXT_PRIMARY);
        return l;
    }

    public static JLabel label(String text) {
        JLabel l = new JLabel(text);
        l.setFont(AppFonts.LABEL);
        l.setForeground(AppColors.TEXT_SECONDARY);
        return l;
    }

    public static JLabel errorLabel() {
        JLabel l = new JLabel(" ");
        l.setFont(AppFonts.BODY_SMALL);
        l.setForeground(AppColors.ERROR);
        return l;
    }

    // ── Panels ────────────────────────────────────────────────────────────

    /** White card with rounded-looking border and padding. */
    public static JPanel card() {
        JPanel p = new JPanel();
        p.setBackground(AppColors.BG_CARD);
        p.setBorder(new CompoundBorder(new LineBorder(AppColors.BORDER, 1), new EmptyBorder(16, 20, 16, 20)));
        return p;
    }

    public static JPanel appBackground() {
        JPanel p = new JPanel();
        p.setBackground(AppColors.BG_APP);
        return p;
    }

    // ── Separators ────────────────────────────────────────────────────────

    public static JSeparator separator() {
        JSeparator sep = new JSeparator();
        sep.setForeground(AppColors.BORDER);
        return sep;
    }

    // ── Dialogs ───────────────────────────────────────────────────────────

    public static void showError(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    public static void showInfo(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "SmartSplit", JOptionPane.INFORMATION_MESSAGE);
    }

    public static boolean confirm(Component parent, String message) {
        return JOptionPane.showConfirmDialog(parent, message, "Confirm", JOptionPane.YES_NO_OPTION)
                == JOptionPane.YES_OPTION;
    }

    // ── Amount formatter ─────────────────────────────────────────────────

    public static String formatRupee(java.math.BigDecimal amount) {
        return "₹" + amount.toPlainString();
    }
}
