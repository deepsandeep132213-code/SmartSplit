package com.smartsplit.ui;

import com.smartsplit.exception.AuthenticationException;
import com.smartsplit.exception.DatabaseException;
import com.smartsplit.exception.InvalidExpenseException;
import com.smartsplit.exception.InvalidSplitException;
import com.smartsplit.exception.MemberNotFoundException;
import com.smartsplit.model.ExpenseCategory;
import com.smartsplit.model.Group;
import com.smartsplit.model.User;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Dialog for adding a new expense.
 * Supports equal split (all members or selected subset) and custom amounts per member.
 */
public class ExpenseFrame extends JDialog {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final GroupFrame  groupFrame;
    private final AppContext   ctx;
    private final Group        group;
    private final List<User>   members;

    // ── Form fields ───────────────────────────────────────────────────────
    private final JTextField     descField    = UITheme.textField();
    private final JTextField     amountField  = UITheme.textField();
    private final JComboBox<User>            payerCombo;
    private final JComboBox<ExpenseCategory> categoryCombo =
            new JComboBox<>(ExpenseCategory.values());
    private final JTextField     dateField    = UITheme.textField();
    private final JTextField     notesField   = UITheme.textField();
    private final JRadioButton   equalAll     = new JRadioButton("All members equally", true);
    private final JRadioButton   equalSubset  = new JRadioButton("Selected members equally");
    private final JRadioButton   customSplit  = new JRadioButton("Custom amounts");

    // ── Dynamic area ──────────────────────────────────────────────────────
    private final JPanel         dynamicPanel = new JPanel(new CardLayout());
    private final JList<String>  subsetList;
    private final DefaultListModel<String> subsetModel = new DefaultListModel<>();
    private final DefaultTableModel        customModel;
    private final JTable                   customTable;

    private final JButton  saveBtn  = UITheme.primaryButton("Save Expense");
    private final JLabel   errorLbl = UITheme.muted(" ");

    public ExpenseFrame(GroupFrame groupFrame, AppContext ctx, Group group) {
        super(groupFrame, "Add Expense — " + group.getGroupName(), true);
        this.groupFrame = groupFrame;
        this.ctx        = ctx;
        this.group      = group;
        this.members    = groupFrame.getCachedMembers();

        payerCombo = new JComboBox<>(members.toArray(new User[0]));
        // Pre-select current user as payer
        ctx.auth.getCurrentUser().ifPresent(u ->
            members.stream().filter(m -> m.getUserId() == u.getUserId())
                    .findFirst().ifPresent(payerCombo::setSelectedItem));

        // Subset list
        members.forEach(u -> subsetModel.addElement(u.getName()));
        subsetList = new JList<>(subsetModel);
        subsetList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        subsetList.setFont(UITheme.FONT_BODY);
        subsetList.setSelectionInterval(0, subsetModel.getSize() - 1);

        // Custom table
        String[] cols = {"Member", "Amount (₹)"};
        customModel = new DefaultTableModel(cols, 0);
        members.forEach(u -> customModel.addRow(new Object[]{u.getName(), ""}));
        customTable = new JTable(customModel);
        customTable.setFont(UITheme.FONT_BODY);
        customTable.setRowHeight(26);

        buildUI();
        wire();
        pack();
        setLocationRelativeTo(groupFrame);
        setResizable(false);
    }

    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout(0, UITheme.PAD_S));
        root.setBackground(UITheme.BG);
        root.setBorder(new EmptyBorder(UITheme.PAD, UITheme.PAD, UITheme.PAD, UITheme.PAD));

        // ── Main form ─────────────────────────────────────────────────────
        JPanel form = UITheme.card();
        form.setLayout(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(5, 4, 5, 4);
        c.fill = GridBagConstraints.HORIZONTAL;

        dateField.setText(LocalDate.now().format(DATE_FMT));
        dateField.setPreferredSize(new Dimension(140, 32));

        int row = 0;
        addRow(form, c, row++, "Description",  descField);
        addRow(form, c, row++, "Amount (₹)",   amountField);
        addRow(form, c, row++, "Paid by",       payerCombo);
        addRow(form, c, row++, "Category",      categoryCombo);
        addRow(form, c, row++, "Date (yyyy-MM-dd)", dateField);
        addRow(form, c, row++, "Notes",         notesField);

        // ── Split type radios ─────────────────────────────────────────────
        c.gridx = 0; c.gridy = row; c.gridwidth = 2; c.weightx = 1;
        JPanel radioPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        radioPanel.setOpaque(false);
        ButtonGroup bg = new ButtonGroup();
        bg.add(equalAll); bg.add(equalSubset); bg.add(customSplit);
        radioPanel.add(equalAll); radioPanel.add(equalSubset); radioPanel.add(customSplit);
        form.add(radioPanel, c);

        row++;
        // ── Dynamic panel (switches based on radio) ───────────────────────
        JPanel subsetCard = new JPanel(new BorderLayout());
        subsetCard.setOpaque(false);
        subsetCard.add(UITheme.muted("Ctrl+click to deselect members:"), BorderLayout.NORTH);
        subsetCard.add(new JScrollPane(subsetList), BorderLayout.CENTER);

        JPanel customCard = new JPanel(new BorderLayout());
        customCard.setOpaque(false);
        customCard.add(UITheme.muted("Enter each member's amount:"), BorderLayout.NORTH);
        customCard.add(new JScrollPane(customTable), BorderLayout.CENTER);

        JPanel blankCard = new JPanel();
        blankCard.setOpaque(false);

        dynamicPanel.add(blankCard,  "all");
        dynamicPanel.add(subsetCard, "subset");
        dynamicPanel.add(customCard, "custom");
        dynamicPanel.setPreferredSize(new Dimension(360, 120));
        ((CardLayout) dynamicPanel.getLayout()).show(dynamicPanel, "all");

        c.gridy = row++;
        form.add(dynamicPanel, c);

        // ── Error + save ──────────────────────────────────────────────────
        errorLbl.setForeground(UITheme.DANGER);
        c.gridy = row++;
        form.add(errorLbl, c);
        c.gridy = row;
        form.add(saveBtn, c);

        root.add(form, BorderLayout.CENTER);
        setContentPane(root);
    }

    private void addRow(JPanel p, GridBagConstraints c, int row, String label, JComponent field) {
        c.gridx = 0; c.gridy = row; c.gridwidth = 1; c.weightx = 0;
        p.add(new JLabel(label + ":"), c);
        c.gridx = 1; c.weightx = 1;
        p.add(field, c);
    }

    private void wire() {
        CardLayout cl = (CardLayout) dynamicPanel.getLayout();
        equalAll   .addActionListener(e -> cl.show(dynamicPanel, "all"));
        equalSubset.addActionListener(e -> cl.show(dynamicPanel, "subset"));
        customSplit.addActionListener(e -> cl.show(dynamicPanel, "custom"));
        saveBtn.addActionListener(e -> onSave());
    }

    private void onSave() {
        errorLbl.setText(" ");
        String desc   = descField.getText().trim();
        String amtStr = amountField.getText().trim();
        User   payer  = (User) payerCombo.getSelectedItem();
        ExpenseCategory cat = (ExpenseCategory) categoryCombo.getSelectedItem();
        String dateStr = dateField.getText().trim();
        String notes   = notesField.getText().trim();

        BigDecimal amount;
        try { amount = new BigDecimal(amtStr); }
        catch (NumberFormatException ex) { errorLbl.setText("Amount must be a number."); return; }

        LocalDate date = null;
        if (!dateStr.isBlank()) {
            try { date = LocalDate.parse(dateStr, DATE_FMT); }
            catch (DateTimeParseException ex) { errorLbl.setText("Date must be yyyy-MM-dd."); return; }
        }

        final LocalDate finalDate = date;
        saveBtn.setEnabled(false);

        if (equalAll.isSelected()) {
            saveEqual(desc, amount, payer, cat, finalDate, notes, null);
        } else if (equalSubset.isSelected()) {
            List<Integer> selected = subsetList.getSelectedValuesList().stream()
                    .map(name -> members.stream().filter(u -> u.getName().equals(name))
                            .mapToInt(User::getUserId).findFirst().orElse(-1))
                    .filter(id -> id >= 0).toList();
            if (selected.isEmpty()) {
                errorLbl.setText("Select at least one member.");
                saveBtn.setEnabled(true);
                return;
            }
            saveEqual(desc, amount, payer, cat, finalDate, notes, selected);
        } else {
            // Custom
            Map<Integer, BigDecimal> amounts = new LinkedHashMap<>();
            for (int i = 0; i < members.size(); i++) {
                String cell = (String) customModel.getValueAt(i, 1);
                if (cell == null || cell.isBlank()) {
                    errorLbl.setText("Enter an amount for " + members.get(i).getName() + ".");
                    saveBtn.setEnabled(true);
                    return;
                }
                try { amounts.put(members.get(i).getUserId(), new BigDecimal(cell)); }
                catch (NumberFormatException ex) {
                    errorLbl.setText("Invalid amount for " + members.get(i).getName() + ".");
                    saveBtn.setEnabled(true);
                    return;
                }
            }
            saveCustom(desc, amount, payer, cat, finalDate, notes, amounts);
        }
    }

    private void saveEqual(String desc, BigDecimal amount, User payer, ExpenseCategory cat,
                           LocalDate date, String notes, List<Integer> participants) {
        SwingWorker<Void, Void> w = new SwingWorker<>() {
            String err = null;
            @Override protected Void doInBackground() {
                try {
                    if (participants == null)
                        ctx.expenses.addExpense(group.getGroupId(), desc, amount,
                                payer.getUserId(), cat, date, notes);
                    else
                        ctx.expenses.addExpense(group.getGroupId(), desc, amount,
                                payer.getUserId(), cat, date, notes, participants);
                } catch (AuthenticationException | DatabaseException | InvalidExpenseException |
                         InvalidSplitException | MemberNotFoundException ex) { err = ex.getMessage(); }
                return null;
            }
            @Override protected void done() {
                saveBtn.setEnabled(true);
                if (err != null) errorLbl.setText(err);
                else dispose();
            }
        };
        w.execute();
    }

    private void saveCustom(String desc, BigDecimal amount, User payer, ExpenseCategory cat,
                            LocalDate date, String notes, Map<Integer, BigDecimal> amounts) {
        SwingWorker<Void, Void> w = new SwingWorker<>() {
            String err = null;
            @Override protected Void doInBackground() {
                try {
                    ctx.expenses.addExpenseCustomSplit(group.getGroupId(), desc, amount,
                            payer.getUserId(), cat, date, notes, amounts);
                } catch (AuthenticationException | DatabaseException | InvalidExpenseException |
                         InvalidSplitException | MemberNotFoundException ex) { err = ex.getMessage(); }
                return null;
            }
            @Override protected void done() {
                saveBtn.setEnabled(true);
                if (err != null) errorLbl.setText(err);
                else dispose();
            }
        };
        w.execute();
    }
}
