package jdbcgui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.WindowConstants;

/**
 * UpdateDeleteGateAssignment: form used to browse (Next / Previous), update
 * and delete rows of the GATE_ASSIGNMENT table. The flight cannot be changed
 * (it is what the assignment is about), but the flight can be moved to
 * another gate of its departure or arrival airport and the employee can be
 * changed. The primary key is (flight_number, airport_code, gate_no, emp_id),
 * so the row is always found using its old key. The employee must work at the
 * airport of the gate, and the assignment of a cancelled flight cannot be
 * changed (it can still be deleted). Update and delete ask the user to
 * confirm first.
 */
public class UpdateDeleteGateAssignment extends JFrame implements ActionListener {

    myDBCon dbCon;
    ResultSet rs; // all gate assignments, scrollable
    ArrayList<String> gateKeys = new ArrayList<String>(); // "airport_code|gate_no" of each item in the gate combo box
    ArrayList<String> empIds = new ArrayList<String>();   // emp_id of each item in the employee combo box

    JTextField txtFlight = new JTextField(20);
    JComboBox<String> cmbGate = new JComboBox<String>();
    JComboBox<String> cmbEmployee = new JComboBox<String>();
    JLabel lblGateError = Util.makeErrorLabel();
    JLabel lblEmpError = Util.makeErrorLabel();
    JButton btnPrevious = new JButton("< Previous");
    JButton btnNext = new JButton("Next >");
    JButton btnUpdate = new JButton("Update");
    JButton btnDelete = new JButton("Delete");

    /** Builds the form, connects to the database and shows the first gate assignment. */
    public UpdateDeleteGateAssignment() {
        setTitle("Update/Delete Gate Assignment");
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        buildGui();
        dbCon = new myDBCon();
        loadData(1);
        pack();
        setResizable(false);
        setLocationRelativeTo(null); // center form in screen
    }

    /** Creates and lays out all the components of the form. */
    private void buildGui() {
        txtFlight.setEditable(false); // the flight cannot be changed

        JPanel form = Util.makeFormPanel();
        Util.addRow(form, 0, "Flight:", txtFlight, null);
        Util.addRow(form, 1, "Gate:", cmbGate, lblGateError);
        Util.addRow(form, 2, "Employee:", cmbEmployee, lblEmpError);

        JPanel buttons = new JPanel(new FlowLayout());
        JButton[] all = {btnPrevious, btnNext, btnUpdate, btnDelete};
        for (int i = 0; i < all.length; i++) {
            all[i].setFont(Util.LABEL_FONT);
            all[i].addActionListener(this);
            buttons.add(all[i]);
        }

        JPanel top = new JPanel(new FlowLayout());
        top.add(Util.makeTitle("Update/Delete Gate Assignment"));

        getContentPane().setLayout(new BorderLayout());
        form.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        getContentPane().add(top, BorderLayout.NORTH);
        getContentPane().add(form, BorderLayout.CENTER);
        getContentPane().add(buttons, BorderLayout.SOUTH);
    }

    /**
     * Reads all gate assignments from the database and shows the one at the
     * given row number (or the last row if there are fewer rows).
     */
    private void loadData(int targetRow) {
        try {
            rs = dbCon.executeStatement("SELECT ga.flight_number, f.dept_airport, f.arr_airport, "
                    + "ga.airport_code, ga.gate_no, ga.emp_id "
                    + "FROM gate_assignment ga, flight f WHERE ga.flight_number = f.flight_number "
                    + "ORDER BY ga.flight_number ASC, ga.airport_code ASC, ga.gate_no ASC, ga.emp_id ASC");
            if (rs.last()) {
                int total = rs.getRow();
                rs.absolute(Math.max(1, Math.min(targetRow, total)));
                populateFields();
            } else {
                clearFields(); // table is empty
            }
        } catch (SQLException e) {
            Util.sqlError(this, "loading gate assignments.", e);
        }
        enableDisableButtons();
    }

    /** Clears the fields when there is no gate assignment to show. */
    private void clearFields() {
        txtFlight.setText("");
        cmbGate.removeAllItems();
        gateKeys.clear();
        cmbEmployee.removeAllItems();
        empIds.clear();
        lblGateError.setVisible(false);
        lblEmpError.setVisible(false);
    }

    /**
     * Shows the current row in the fields. The gate combo box lists the gates
     * of the flight's departure and arrival airports; the current gate and
     * employee are selected using indexOf() on the key lists.
     */
    private void populateFields() throws SQLException {
        lblGateError.setVisible(false);
        lblEmpError.setVisible(false);

        String flightNo = rs.getString("flight_number");
        String dept = rs.getString("dept_airport");
        String arr = rs.getString("arr_airport");
        txtFlight.setText(flightNo + " - " + dept + " to " + arr);

        Util.fillCombo(dbCon, cmbGate, gateKeys,
                "SELECT airport_code || '|' || gate_no, "
                + "airport_code || ' gate ' || gate_no || ' (Terminal ' || terminal || ')' "
                + "FROM gate WHERE airport_code IN ('" + dept + "', '" + arr + "') "
                + "ORDER BY airport_code, gate_no", null);
        int gateIdx = gateKeys.indexOf(rs.getString("airport_code") + "|" + rs.getString("gate_no"));
        cmbGate.setSelectedIndex(gateIdx < 0 ? 0 : gateIdx);

        Util.fillCombo(dbCon, cmbEmployee, empIds,
                "SELECT emp_id, first_name || ' ' || last_name || ' (' || airport_code || ')' "
                + "FROM a_employee ORDER BY first_name, last_name", null);
        int empIdx = empIds.indexOf(rs.getString("emp_id"));
        cmbEmployee.setSelectedIndex(empIdx < 0 ? 0 : empIdx);

        pack(); // shrink the window back after the error labels were shown
    }

    /** Enables / disables Next, Previous, Update and Delete depending on the current row. */
    private void enableDisableButtons() {
        try {
            boolean hasRows = rs != null && rs.getRow() > 0;
            btnPrevious.setEnabled(hasRows && !rs.isFirst());
            btnNext.setEnabled(hasRows && !rs.isLast());
            btnUpdate.setEnabled(hasRows);
            btnDelete.setEnabled(hasRows);
        } catch (SQLException e) {
            Util.sqlError(this, "updating buttons.", e);
        }
    }

    /** Moves to the next gate assignment. */
    private void moveNext() {
        try {
            if (!rs.isLast()) {
                rs.next();
                populateFields();
            }
        } catch (SQLException e) {
            Util.sqlError(this, "moving to next gate assignment.", e);
        }
        enableDisableButtons();
    }

    /** Moves to the previous gate assignment. */
    private void movePrevious() {
        try {
            if (!rs.isFirst()) {
                rs.previous();
                populateFields();
            }
        } catch (SQLException e) {
            Util.sqlError(this, "moving to previous gate assignment.", e);
        }
        enableDisableButtons();
    }

    /** Checks the rules, asks for confirmation and updates the shown gate assignment (its gate and employee). */
    private void updateAssignment() {
        lblGateError.setVisible(false);
        lblEmpError.setVisible(false);
        if (cmbGate.getSelectedIndex() < 0) {
            Util.showFieldError(lblGateError, "Please select a gate");
            pack();
            return;
        }
        if (cmbEmployee.getSelectedIndex() < 0) {
            Util.showFieldError(lblEmpError, "Please select an employee");
            pack();
            return;
        }
        String[] newGate = gateKeys.get(cmbGate.getSelectedIndex()).split("\\|");
        String newCode = newGate[0];
        String newGateNo = newGate[1];
        Integer newEmp = Integer.valueOf(empIds.get(cmbEmployee.getSelectedIndex()));
        try {
            // the assignment as it is stored now (the key we are changing)
            Integer flightNo = Integer.valueOf(rs.getString("flight_number"));
            String oldCode = rs.getString("airport_code");
            String oldGateNo = rs.getString("gate_no");
            Integer oldEmp = Integer.valueOf(rs.getString("emp_id"));

            boolean changed = !newCode.equals(oldCode) || !newGateNo.equals(oldGateNo) || !newEmp.equals(oldEmp);
            if (!changed) {
                Util.info(this, "Nothing was changed.");
                return;
            }
            // the assignment of a cancelled flight cannot be changed
            if (Util.count(dbCon, "SELECT COUNT(*) FROM flight WHERE flight_number = ? AND status = 'Cancelled'",
                    flightNo) > 0) {
                Util.showFieldError(lblGateError, "Flight " + flightNo + " is cancelled, so this cannot be changed");
                pack();
                return;
            }
            // the employee must work at the airport of the chosen gate
            if (Util.count(dbCon, "SELECT COUNT(*) FROM a_employee WHERE emp_id = ? AND airport_code = ?",
                    newEmp, newCode) == 0) {
                Util.showFieldError(lblEmpError, "This employee does not work at " + newCode);
                pack();
                return;
            }
            // the new key must not already exist
            if (Util.count(dbCon, "SELECT COUNT(*) FROM gate_assignment "
                    + "WHERE flight_number = ? AND airport_code = ? AND gate_no = ? AND emp_id = ?",
                    flightNo, newCode, newGateNo, newEmp) > 0) {
                Util.showFieldError(lblEmpError, "This employee is already assigned to this flight at this gate");
                pack();
                return;
            }
            if (!Util.confirm(this, "Update the gate assignment of flight " + flightNo + "?", "Confirm Update")) {
                return; // user clicked No: nothing changes
            }
            Util.update(dbCon, "UPDATE gate_assignment SET airport_code = ?, gate_no = ?, emp_id = ? "
                    + "WHERE flight_number = ? AND airport_code = ? AND gate_no = ? AND emp_id = ?",
                    newCode, newGateNo, newEmp, flightNo, oldCode, oldGateNo, oldEmp);
            Util.info(this, "The gate assignment of flight " + flightNo + " was updated successfully.");
            loadData(currentRow());
        } catch (SQLException e) {
            Util.sqlError(this, "updating gate assignment.", e);
        }
    }

    /** Asks for confirmation and deletes the shown gate assignment. */
    private void deleteAssignment() {
        try {
            // delete the assignment as it is stored, not what is selected in the fields
            Integer flightNo = Integer.valueOf(rs.getString("flight_number"));
            String code = rs.getString("airport_code");
            String gateNo = rs.getString("gate_no");
            Integer emp = Integer.valueOf(rs.getString("emp_id"));
            if (!Util.confirm(this, "Delete the assignment of flight " + flightNo + " to gate "
                    + gateNo + " at " + code + " (employee " + emp + ")?", "Confirm Delete")) {
                return; // user clicked No: nothing changes
            }
            Util.update(dbCon, "DELETE FROM gate_assignment WHERE flight_number = ? "
                    + "AND airport_code = ? AND gate_no = ? AND emp_id = ?", flightNo, code, gateNo, emp);
            Util.info(this, "The gate assignment was deleted successfully.");
            loadData(currentRow());
        } catch (SQLException e) {
            Util.sqlError(this, "deleting gate assignment.", e);
        }
    }

    /** Returns the current row number of the result set (1 if none). */
    private int currentRow() {
        try {
            return Math.max(1, rs.getRow());
        } catch (SQLException e) {
            return 1;
        }
    }

    /** Handles the Next, Previous, Update and Delete buttons. */
    public void actionPerformed(ActionEvent e) {
        Object src = e.getSource();
        if (src == btnNext) {
            moveNext();
        } else if (src == btnPrevious) {
            movePrevious();
        } else if (src == btnUpdate) {
            updateAssignment();
        } else if (src == btnDelete) {
            deleteAssignment();
        }
    }
}