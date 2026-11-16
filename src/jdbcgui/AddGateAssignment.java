package jdbcgui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.SQLException;
import java.util.ArrayList;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.WindowConstants;

/**
 * AddGateAssignment: form used to insert a new row into the GATE_ASSIGNMENT
 * table (which flight uses which gate, and which employee handles it).
 * All three combo boxes show names; the matching keys are kept in lists in
 * the same order. The gate list only shows the gates of the selected flight's
 * departure and arrival airports. The employee is optional.
 * The primary key is (flight_number, airport_code, gate_no).
 */
public class AddGateAssignment extends JFrame implements ActionListener {

    myDBCon dbCon;
    ArrayList<String> flightNos = new ArrayList<String>(); // flight_number of each item in the flight combo box
    ArrayList<String> gateKeys = new ArrayList<String>();  // "airport_code|gate_no" of each item in the gate combo box
    ArrayList<String> empIds = new ArrayList<String>();    // emp_id of each item in the employee combo box

    JComboBox<String> cmbFlight = new JComboBox<String>();
    JComboBox<String> cmbGate = new JComboBox<String>();
    JComboBox<String> cmbEmployee = new JComboBox<String>();
    JLabel lblGateError = Util.makeErrorLabel();
    JButton btnAdd = new JButton("Add");
    JButton btnClear = new JButton("Clear");

    /** Builds the form, connects to the database and loads the three drop downs. */
    public AddGateAssignment() {
        setTitle("Add New Gate Assignment");
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        buildGui();
        dbCon = new myDBCon();
        loadFlights();
        loadGates();
        loadEmployees();
        cmbFlight.addActionListener(this); // when the flight changes, the gate list changes
        pack();
        setResizable(false);
        setLocationRelativeTo(null); // center form in screen
    }

    /** Creates and lays out all the components of the form. */
    private void buildGui() {
        JPanel form = Util.makeFormPanel();
        Util.addRow(form, 0, "Flight:", cmbFlight, null);
        Util.addRow(form, 1, "Gate:", cmbGate, lblGateError);
        Util.addRow(form, 2, "Employee (optional):", cmbEmployee, null);

        JPanel buttons = new JPanel(new FlowLayout());
        btnAdd.setFont(Util.LABEL_FONT);
        btnClear.setFont(Util.LABEL_FONT);
        btnAdd.addActionListener(this);
        btnClear.addActionListener(this);
        buttons.add(btnAdd);
        buttons.add(btnClear);

        JPanel top = new JPanel(new FlowLayout());
        top.add(Util.makeTitle("Add New Gate Assignment"));

        getContentPane().setLayout(new BorderLayout());
        form.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        getContentPane().add(top, BorderLayout.NORTH);
        getContentPane().add(form, BorderLayout.CENTER);
        getContentPane().add(buttons, BorderLayout.SOUTH);
    }

    /** Fills the flight combo box with "number - route (departure time)". */
    private void loadFlights() {
        try {
            Util.fillCombo(dbCon, cmbFlight, flightNos,
                    "SELECT flight_number, flight_number || ' - ' || dept_airport || ' to ' || arr_airport "
                    + "|| ' (' || TO_CHAR(dept_time, 'YYYY-MM-DD HH24:MI') || ')' "
                    + "FROM flight ORDER BY dept_time", null);
        } catch (SQLException e) {
            Util.sqlError(this, "loading flights.", e);
        }
    }

    /** Fills the gate combo box with the gates of the selected flight's departure and arrival airports. */
    private void loadGates() {
        int idx = cmbFlight.getSelectedIndex();
        if (idx < 0) {
            cmbGate.removeAllItems();
            gateKeys.clear();
            return;
        }
        try {
            String flightNo = flightNos.get(idx);
            Util.fillCombo(dbCon, cmbGate, gateKeys,
                    "SELECT airport_code || '|' || gate_no, "
                    + "airport_code || ' gate ' || gate_no || ' (Terminal ' || terminal || ')' "
                    + "FROM gate WHERE airport_code IN "
                    + "(SELECT dept_airport FROM flight WHERE flight_number = " + Integer.parseInt(flightNo)
                    + " UNION SELECT arr_airport FROM flight WHERE flight_number = " + Integer.parseInt(flightNo) + ") "
                    + "ORDER BY airport_code, gate_no", null);
        } catch (SQLException e) {
            Util.sqlError(this, "loading gates.", e);
        }
    }

    /** Fills the employee combo box (the first item means no employee). */
    private void loadEmployees() {
        try {
            Util.fillCombo(dbCon, cmbEmployee, empIds,
                    Util.EMPLOYEE_NAMES_SQL + "ORDER BY first_name, last_name", "(No employee)");
        } catch (SQLException e) {
            Util.sqlError(this, "loading employees.", e);
        }
    }

    /** Clears the selections and the error label. */
    private void clearFields() {
        if (cmbFlight.getItemCount() > 0) {
            cmbFlight.setSelectedIndex(0);
        }
        loadGates();
        if (cmbEmployee.getItemCount() > 0) {
            cmbEmployee.setSelectedIndex(0);
        }
        lblGateError.setVisible(false);
        pack();
    }

    /** Checks for duplicates and inserts the gate assignment. */
    private void addAssignment() {
        lblGateError.setVisible(false);
        if (cmbFlight.getSelectedIndex() < 0) {
            Util.error(this, "There are no flights to assign.");
            return;
        }
        if (cmbGate.getSelectedIndex() < 0) {
            Util.showFieldError(lblGateError, "This flight's airports have no gates");
            pack();
            return;
        }
        String flightNo = flightNos.get(cmbFlight.getSelectedIndex());
        String[] gate = gateKeys.get(cmbGate.getSelectedIndex()).split("\\|");
        String code = gate[0];
        String gateNo = gate[1];
        Integer emp = Util.toInt(empIds.get(cmbEmployee.getSelectedIndex())); // null when "No employee"
        try {
            // the primary key is (flight_number, airport_code, gate_no): check it first
            if (Util.count(dbCon, "SELECT COUNT(*) FROM gate_assignment "
                    + "WHERE flight_number = ? AND airport_code = ? AND gate_no = ?",
                    Integer.valueOf(flightNo), code, gateNo) > 0) {
                Util.showFieldError(lblGateError, "This flight is already assigned to this gate");
                pack();
                return;
            }
            Util.update(dbCon, "INSERT INTO gate_assignment (flight_number, airport_code, gate_no, emp_id) "
                    + "VALUES (?, ?, ?, ?)", Integer.valueOf(flightNo), code, gateNo, emp);
            Util.info(this, "Flight " + flightNo + " was assigned to gate " + gateNo + " at " + code + ".");
            clearFields();
        } catch (SQLException e) {
            Util.sqlError(this, "adding gate assignment.", e);
        }
    }

    /** Handles the Add and Clear buttons and the flight combo box. */
    public void actionPerformed(ActionEvent e) {
        Object src = e.getSource();
        if (src == btnAdd) {
            addAssignment();
        } else if (src == btnClear) {
            clearFields();
        } else if (src == cmbFlight) {
            loadGates();
            lblGateError.setVisible(false);
            pack();
        }
    }
}