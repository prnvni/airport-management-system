package jdbcgui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.math.BigDecimal;
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
 * UpdateDeleteStaff: form used to browse (Next / Previous), update and delete
 * rows of the A_EMPLOYEE table. The employee id is the primary key so it
 * cannot be edited. An employee who manages an airport or supervises other
 * employees is not deleted. Deleting an employee also deletes his / her
 * qualifications (ON DELETE CASCADE). Update and delete ask for confirmation.
 */
public class UpdateDeleteStaff extends JFrame implements ActionListener {

    myDBCon dbCon;
    ResultSet rs; // all employees, scrollable
    ArrayList<String> airportCodes = new ArrayList<String>();
    ArrayList<String> airlineCodes = new ArrayList<String>();
    ArrayList<String> supervisorIds = new ArrayList<String>();

    JTextField txtEmpId = new JTextField(20);
    JTextField txtFirst = new JTextField(20);
    JTextField txtLast = new JTextField(20);
    JTextField txtRole = new JTextField(20);
    JTextField txtSalary = new JTextField(20);
    JComboBox<String> cmbAirport = new JComboBox<String>();
    JComboBox<String> cmbAirline = new JComboBox<String>();
    JComboBox<String> cmbSupervisor = new JComboBox<String>();
    JLabel lblFirstError = Util.makeErrorLabel();
    JLabel lblLastError = Util.makeErrorLabel();
    JLabel lblRoleError = Util.makeErrorLabel();
    JLabel lblSalaryError = Util.makeErrorLabel();
    JButton btnPrevious = new JButton("< Previous");
    JButton btnNext = new JButton("Next >");
    JButton btnUpdate = new JButton("Update");
    JButton btnDelete = new JButton("Delete");

    /** Builds the form, connects to the database and shows the first employee. */
    public UpdateDeleteStaff() {
        setTitle("Update/Delete Staff Member");
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
        txtEmpId.setEditable(false); // primary key: cannot be edited

        JPanel form = Util.makeFormPanel();
        Util.addRow(form, 0, "Employee ID:", txtEmpId, null);
        Util.addRow(form, 1, "First name:", txtFirst, lblFirstError);
        Util.addRow(form, 2, "Last name:", txtLast, lblLastError);
        Util.addRow(form, 3, "Role:", txtRole, lblRoleError);
        Util.addRow(form, 4, "Salary:", txtSalary, lblSalaryError);
        Util.addRow(form, 5, "Airport:", cmbAirport, null);
        Util.addRow(form, 6, "Airline:", cmbAirline, null);
        Util.addRow(form, 7, "Supervisor:", cmbSupervisor, null);

        JPanel buttons = new JPanel(new FlowLayout());
        JButton[] all = {btnPrevious, btnNext, btnUpdate, btnDelete};
        for (int i = 0; i < all.length; i++) {
            all[i].setFont(Util.LABEL_FONT);
            all[i].addActionListener(this);
            buttons.add(all[i]);
        }

        JPanel top = new JPanel(new FlowLayout());
        top.add(Util.makeTitle("Update/Delete Staff Member"));

        getContentPane().setLayout(new BorderLayout());
        form.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        getContentPane().add(top, BorderLayout.NORTH);
        getContentPane().add(form, BorderLayout.CENTER);
        getContentPane().add(buttons, BorderLayout.SOUTH);
    }

    /**
     * Reads all employees from the database and shows the employee at the
     * given row number (or the last row if there are fewer rows).
     */
    private void loadData(int targetRow) {
        try {
            rs = dbCon.executeStatement("SELECT emp_id, first_name, last_name, role, salary, "
                    + "airport_code, airline_code, supervisor_id FROM a_employee ORDER BY emp_id ASC");
            if (rs.last()) {
                int total = rs.getRow();
                rs.absolute(Math.max(1, Math.min(targetRow, total)));
                populateFields();
            } else {
                clearFields(); // table is empty
            }
        } catch (SQLException e) {
            Util.sqlError(this, "loading employees.", e);
        }
        enableDisableButtons();
    }

    /** Clears the fields when there is no employee to show. */
    private void clearFields() {
        txtEmpId.setText("");
        txtFirst.setText("");
        txtLast.setText("");
        txtRole.setText("");
        txtSalary.setText("");
        cmbAirport.removeAllItems();
        cmbAirline.removeAllItems();
        cmbSupervisor.removeAllItems();
    }

    /**
     * Shows the current row in the fields. The combo boxes show names; the
     * name to select is found with indexOf() on the matching code / id list.
     * The employee himself is not listed as his own supervisor.
     */
    private void populateFields() throws SQLException {
        lblFirstError.setVisible(false);
        lblLastError.setVisible(false);
        lblRoleError.setVisible(false);
        lblSalaryError.setVisible(false);

        String id = rs.getString("emp_id");
        txtEmpId.setText(id);
        txtFirst.setText(rs.getString("first_name"));
        txtLast.setText(rs.getString("last_name"));
        txtRole.setText(rs.getString("role"));
        txtSalary.setText(rs.getBigDecimal("salary").toPlainString());

        Util.fillCombo(dbCon, cmbAirport, airportCodes,
                "SELECT airport_code, airport_name || ' (' || airport_code || ')' "
                + "FROM airport ORDER BY airport_name", null);
        Util.fillCombo(dbCon, cmbAirline, airlineCodes,
                "SELECT airline_code, airline_name FROM airline ORDER BY airline_name",
                "(No airline)");
        Util.fillCombo(dbCon, cmbSupervisor, supervisorIds,
                Util.EMPLOYEE_NAMES_SQL + "WHERE emp_id <> " + Integer.parseInt(id)
                + " ORDER BY first_name, last_name", "(No supervisor)");

        int i = airportCodes.indexOf(rs.getString("airport_code"));
        cmbAirport.setSelectedIndex(i < 0 ? 0 : i);
        i = airlineCodes.indexOf(rs.getString("airline_code")); // null finds "(No airline)"
        cmbAirline.setSelectedIndex(i < 0 ? 0 : i);
        i = supervisorIds.indexOf(rs.getString("supervisor_id")); // null finds "(No supervisor)"
        cmbSupervisor.setSelectedIndex(i < 0 ? 0 : i);
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

    /** Moves to the next employee. */
    private void moveNext() {
        try {
            if (!rs.isLast()) {
                rs.next();
                populateFields();
            }
        } catch (SQLException e) {
            Util.sqlError(this, "moving to next employee.", e);
        }
        enableDisableButtons();
    }

    /** Moves to the previous employee. */
    private void movePrevious() {
        try {
            if (!rs.isFirst()) {
                rs.previous();
                populateFields();
            }
        } catch (SQLException e) {
            Util.sqlError(this, "moving to previous employee.", e);
        }
        enableDisableButtons();
    }

    /** Validates, asks for confirmation and updates the shown employee. */
    private void updateStaff() {
        String first = txtFirst.getText().trim();
        String last = txtLast.getText().trim();
        String role = txtRole.getText().trim();
        boolean ok = true;
        lblFirstError.setVisible(false);
        lblLastError.setVisible(false);
        lblRoleError.setVisible(false);
        lblSalaryError.setVisible(false);

        if (first.length() < 1 || first.length() > 30) {
            Util.showFieldError(lblFirstError, "First name must be 1 to 30 characters");
            ok = false;
        }
        if (last.length() < 1 || last.length() > 30) {
            Util.showFieldError(lblLastError, "Last name must be 1 to 30 characters");
            ok = false;
        }
        if (role.length() < 1 || role.length() > 30) {
            Util.showFieldError(lblRoleError, "Role must be 1 to 30 characters");
            ok = false;
        }
        BigDecimal salary = AddStaff.parseSalary(txtSalary.getText());
        if (salary == null) {
            Util.showFieldError(lblSalaryError, "Salary must be a positive number (max 2 decimals)");
            ok = false;
        }
        if (!ok) {
            return;
        }
        if (!Util.confirm(this, "Update employee " + txtEmpId.getText() + "?", "Confirm Update")) {
            return; // user clicked No: nothing changes
        }
        try {
            String airport = airportCodes.get(cmbAirport.getSelectedIndex());
            String airline = airlineCodes.get(cmbAirline.getSelectedIndex());
            Integer supervisor = Util.toInt(supervisorIds.get(cmbSupervisor.getSelectedIndex()));
            Util.update(dbCon, "UPDATE a_employee SET first_name = ?, last_name = ?, role = ?, salary = ?, "
                    + "airport_code = ?, airline_code = ?, supervisor_id = ? WHERE emp_id = ?",
                    first, last, role, salary, airport, airline, supervisor,
                    Integer.parseInt(txtEmpId.getText()));
            Util.info(this, "Employee " + txtEmpId.getText() + " was updated successfully.");
            loadData(currentRow());
        } catch (SQLException e) {
            Util.sqlError(this, "updating employee.", e);
        }
    }

    /**
     * Asks for confirmation and deletes the shown employee. An employee who
     * manages an airport or supervises other employees is not deleted.
     */
    private void deleteStaff() {
        int id = Integer.parseInt(txtEmpId.getText());
        try {
            int managed = Util.count(dbCon, "SELECT COUNT(*) FROM airport WHERE manager_id = ?", id);
            if (managed > 0) {
                Util.error(this, "This employee is the manager of an airport and cannot be deleted.\n"
                        + "Change the airport's manager first.");
                return;
            }
            int supervised = Util.count(dbCon, "SELECT COUNT(*) FROM a_employee WHERE supervisor_id = ?", id);
            if (supervised > 0) {
                Util.error(this, "This employee supervises " + supervised
                        + " employee(s) and cannot be deleted.\nChange their supervisor first.");
                return;
            }
            int quals = Util.count(dbCon, "SELECT COUNT(*) FROM employee_qualification WHERE emp_id = ?", id);
            String msg = "Delete employee " + id + "?";
            if (quals > 0) {
                msg = msg + "\nHis / her " + quals + " qualification(s) will also be deleted.";
            }
            if (!Util.confirm(this, msg, "Confirm Delete")) {
                return; // user clicked No: nothing changes
            }
            Util.update(dbCon, "DELETE FROM a_employee WHERE emp_id = ?", id);
            Util.info(this, "Employee " + id + " was deleted successfully.");
            loadData(currentRow());
        } catch (SQLException e) {
            if (e.getErrorCode() == 2292) { // ORA-02292: child records found
                Util.error(this, "This employee cannot be deleted because other records still use it.");
            } else {
                Util.sqlError(this, "deleting employee.", e);
            }
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
            updateStaff();
        } else if (src == btnDelete) {
            deleteStaff();
        }
    }
}
