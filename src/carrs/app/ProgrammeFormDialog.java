package carrs.app;

import carrs.model.ProgrammeDetails;

import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import javax.swing.*;

/** The form for both "New Programme" and "Edit Programme". */
public class ProgrammeFormDialog extends JDialog {
    private final ProgramManagementService service;
    private final ProgrammeDetails existing;      // null when creating
    private boolean saved = false;

    private final JTextField nameField = new JTextField(25);
    private final JTextField startField = new JTextField(25);
    private final JTextField finishField = new JTextField(25);
    private final JTextField capacityField = new JTextField(25);

    public ProgrammeFormDialog(Window owner, ProgramManagementService service, ProgrammeDetails existing) {
        super(owner, existing == null ? "New Programme" : "Edit Programme", ModalityType.APPLICATION_MODAL);
        this.service = service;
        this.existing = existing;

        if (existing != null) {
            nameField.setText(existing.getName());
            if (existing.getStartDate() != null) startField.setText(existing.getStartDate().toString());
            if (existing.getFinishDate() != null) finishField.setText(existing.getFinishDate().toString());
            capacityField.setText(String.valueOf(existing.getCapacity()));
        }

        JPanel fields = new JPanel(new GridLayout(0, 2, 5, 5));
        fields.setBorder(BorderFactory.createTitledBorder("Programme Information"));
        fields.add(new JLabel("Programme Name:")); fields.add(nameField);
        fields.add(new JLabel("Start Date (YYYY-MM-DD):")); fields.add(startField);
        fields.add(new JLabel("Finish Date (YYYY-MM-DD):")); fields.add(finishField);
        fields.add(new JLabel("Capacity:")); fields.add(capacityField);

        JButton save = new JButton("Save");
        JButton cancel = new JButton("Cancel");
        save.addActionListener(e -> onSave());
        cancel.addActionListener(e -> dispose());
        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        south.add(save); south.add(cancel);

        JPanel root = new JPanel(new BorderLayout(0, 10));
        root.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        root.add(fields, BorderLayout.CENTER);
        root.add(south, BorderLayout.SOUTH);
        setContentPane(root);
        getRootPane().setDefaultButton(save);
        pack();
        setLocationRelativeTo(owner);
    }

    private void onSave() {
        try {
            LocalDate start = LocalDate.parse(startField.getText().trim());
            LocalDate finish = LocalDate.parse(finishField.getText().trim());
            int capacity = Integer.parseInt(capacityField.getText().trim());
            if (existing == null) service.create(nameField.getText(), start, finish, capacity);
            else service.update(existing.getId(), nameField.getText(), start, finish, capacity);
            saved = true;
            dispose();
        } catch (DateTimeParseException ex) {
            JOptionPane.showMessageDialog(this, "Dates must look like 2027-01-31.");
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Capacity must be a whole number.");
        } catch (IllegalArgumentException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage());
        } catch (RuntimeException ex) {
            JOptionPane.showMessageDialog(this, "Could not save: " + ex.getMessage());
        }
    }

    public boolean wasSaved() { return saved; }
}
