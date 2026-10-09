package com.smartsplit.ui;

import com.smartsplit.exception.AuthenticationException;
import com.smartsplit.exception.DatabaseException;
import com.smartsplit.model.Group;
import com.smartsplit.model.User;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

/**
 * Main window after login.
 * Shows the user's active groups; lets them create a new group or open one.
 */
public class DashboardFrame extends JFrame {

    private final AppContext ctx;

    private final DefaultListModel<Group> listModel = new DefaultListModel<>();
    private final JList<Group>            groupList = new JList<>(listModel);
    private final JLabel                  welcomeLabel;
    private final JButton                 newGroupBtn = UITheme.primaryButton("+ New Group");
    private final JButton                 logoutBtn   = UITheme.linkButton("Log out");

    public DashboardFrame(AppContext ctx) {
        super("SmartSplit — My Groups");
        this.ctx = ctx;
        User user = ctx.auth.getCurrentUser().orElseThrow();
        welcomeLabel = UITheme.muted("Welcome, " + user.getName());
        buildUI();
        wire();
        setSize(480, 560);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        loadGroups();
    }

    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(UITheme.BG);

        // ── Top bar ───────────────────────────────────────────────────────
        JPanel topBar = new JPanel(new BorderLayout(UITheme.PAD, 0));
        topBar.setBackground(UITheme.PRIMARY);
        topBar.setBorder(new EmptyBorder(12, UITheme.PAD, 12, UITheme.PAD));

        JLabel title = new JLabel("SmartSplit");
        title.setFont(UITheme.FONT_HEADING);
        title.setForeground(Color.WHITE);

        JPanel rightBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        rightBar.setOpaque(false);
        welcomeLabel.setForeground(new Color(0xC5CAE9));
        rightBar.add(welcomeLabel);
        rightBar.add(logoutBtn);
        logoutBtn.setForeground(new Color(0xC5CAE9));

        topBar.add(title, BorderLayout.WEST);
        topBar.add(rightBar, BorderLayout.EAST);
        root.add(topBar, BorderLayout.NORTH);

        // ── Group list ────────────────────────────────────────────────────
        groupList.setFont(UITheme.FONT_BODY);
        groupList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        groupList.setCellRenderer(new GroupCellRenderer());
        groupList.setBackground(UITheme.SURFACE);
        groupList.setFixedCellHeight(52);

        JScrollPane scroll = new JScrollPane(groupList);
        scroll.setBorder(BorderFactory.createMatteBorder(1, 0, 1, 0, UITheme.BORDER_COLOR));

        JPanel centre = new JPanel(new BorderLayout());
        centre.setOpaque(false);
        centre.setBorder(new EmptyBorder(UITheme.PAD, UITheme.PAD, UITheme.PAD, UITheme.PAD));

        JLabel sectionLabel = UITheme.heading("My Groups");
        centre.add(sectionLabel, BorderLayout.NORTH);
        centre.add(scroll, BorderLayout.CENTER);
        root.add(centre, BorderLayout.CENTER);

        // ── Bottom bar ────────────────────────────────────────────────────
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, UITheme.PAD, UITheme.PAD_S));
        bottom.setBackground(UITheme.BG);
        bottom.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UITheme.BORDER_COLOR));
        bottom.add(newGroupBtn);
        root.add(bottom, BorderLayout.SOUTH);

        setContentPane(root);
    }

    private void wire() {
        newGroupBtn.addActionListener(e -> onNewGroup());
        logoutBtn.addActionListener(e -> onLogout());

        // Double-click to open a group
        groupList.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) openSelected();
            }
        });
        groupList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && groupList.getSelectedValue() != null) openSelected();
        });
    }

    void loadGroups() {
        listModel.clear();
        SwingWorker<List<Group>, Void> w = new SwingWorker<>() {
            @Override protected List<Group> doInBackground() throws Exception {
                return ctx.groups.getUserGroups();
            }
            @Override protected void done() {
                try {
                    get().forEach(listModel::addElement);
                    // empty state handled by renderer checking isEmpty
                } catch (Exception ex) {
                    UITheme.showError(DashboardFrame.this, "Could not load groups: " + ex.getMessage());
                }
            }
        };
        w.execute();
    }

    private void openSelected() {
        Group g = groupList.getSelectedValue();
        if (g == null) return;
        new GroupFrame(this, ctx, g).setVisible(true);
    }

    private void onNewGroup() {
        String name = JOptionPane.showInputDialog(this,
                "Group name:", "New Group", JOptionPane.PLAIN_MESSAGE);
        if (name == null || name.isBlank()) return;
        try {
            ctx.groups.createGroup(name);
            loadGroups();
        } catch (AuthenticationException | DatabaseException | IllegalArgumentException ex) {
            UITheme.showError(this, ex.getMessage());
        }
    }

    private void onLogout() {
        ctx.auth.logout();
        dispose();
        LoginFrame.show(ctx);
    }

    // ── Cell renderer ─────────────────────────────────────────────────────
    private static class GroupCellRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value,
                int index, boolean isSelected, boolean cellHasFocus) {
            JPanel p = new JPanel(new BorderLayout(UITheme.PAD_S, 0));
            p.setBorder(new EmptyBorder(10, UITheme.PAD, 10, UITheme.PAD));
            p.setBackground(isSelected ? new Color(0xE8EAF6) : UITheme.SURFACE);

            Group g = (Group) value;
            JLabel name = new JLabel(g.getGroupName());
            name.setFont(UITheme.FONT_HEADING);
            name.setForeground(UITheme.TEXT_PRIMARY);

            JLabel arrow = new JLabel("›");
            arrow.setFont(new Font("SansSerif", Font.PLAIN, 20));
            arrow.setForeground(UITheme.TEXT_MUTED);

            p.add(name, BorderLayout.CENTER);
            p.add(arrow, BorderLayout.EAST);

            if (index < list.getModel().getSize() - 1)
                p.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 1, 0, UITheme.BORDER_COLOR),
                        new EmptyBorder(10, UITheme.PAD, 10, UITheme.PAD)));
            return p;
        }
    }
}

