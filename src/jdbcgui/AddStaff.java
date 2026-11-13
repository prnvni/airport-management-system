package jdbcgui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.math.BigDecimal;
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
 * AddStaff: form used to insert a new row into the A_EMPLOYEE table.
 * The Airport, Airline and Supervisor combo boxes show names; the matching
 * codes / ids are kept in the lists airportCodes, airlineCodes and
 * supervisorIds in the same order and are used in the INSERT statement.
 */
public class AddStaff extends JFrame implements ActionListener {

    myDBCon dbCon;
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
    JLabel lblIdError = Util.makeErrorLabel();
    JLabel lblFirstError = Util.makeErrorLabel();
    JLabel lblLastError = Util.makeErrorLabel();
    JLabel lblRoleError = Util.makeErrorLabel();
    JLabel lblSalaryError = Util.makeErrorLabel();
    JButton btnAdd = new JButton("Add");
    JButton btnClear = new JButton("Clear");

    /** Builds the form, connects to the database and loads the combo boxes. */
    public AddStaff() {
        setTitle("Add New Staff Member");
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        buildGui();
        dbCon = new myDBCon();
        loadCombos();
        suggestNextId();
        pack();
        setResizable(false);
        setLocationRelativeTo(null); // center form in screen
    }

    /** Creates and lays out all the components of the form. */
    private void buildGui() {
        JPanel form = Util.makeFormPanel();
        Util.addRow(form, 0, "Employee ID:", txtEmpId, lblIdError);
        Util.addRow(form, 1, "First name:", txtFirst, lblFirstError);
        Util.addRow(form, 2, "Last name:", txtLast, lblLastError);
        Util.addRow(form, 3, "Role:", txtRole, lblRoleError);
        Util.addRow(form, 4, "Salary:", txtSalary, lblSalaryError);
        Util.addRow(form, 5, "Airport:", cmbAirport, null);
        Util.addRow(form, 6, "Airline:", cmbAirline, null);
        Util.addRow(form, 7, "Supervisor:", cmbSupervisor, null);

        JPanel buttons = new JPanel(new FlowLayout());
        btnAdd.setFont(Util.LABEL_FONT);
        btnClear.setFont(Util.LABEL_FONT);
        btnAdd.addActionListener(this);
        btnClear.addActionListener(this);
        buttons.add(btnAdd);
        buttons.add(btnClear);

        JPanel top = new JPanel(new FlowLayout());
        top.add(Util.makeTitle("Add New Staff Member"));

        getContentPane().setLayout(new BorderLayout());
        form.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        getContentPane().add(top, BorderLayout.NORTH);
        getContentPane().add(form, BorderLayout.CENTER);
        getContentPane().add(buttons, BorderLayout.SOUTH);
    }

    /** Fills the Airport, Airline and Supervisor combo boxes with names. */
    private void loadCombos() {
        try {
            Util.fillCombo(dbCon, cmbAirport, airportCodes,
                    "SELECT airport_code, airport_name || ' (' || airport_code || ')' "
                    + "FROM airport ORDER BY airport_name", null);
            Util.fillCombo(dbCon, cmbAirline, airlineCodes,
                    "SELECT airline_code, airline_name FROM airline ORDER BY airline_name",
                    "(No airline)");
            Util.fillCombo(dbCon, cmbSupervisor, supervisorIds,
                    Util.EMPLOYEE_NAMES_SQL + "ORDER BY first_name, last_name",
                    "(No supervisor)");
        } catch (SQLException e) {
            Util.sqlError(this, "loading airports / airlines / supervisors.", e);
        }
    }

    /** Puts the next free employee id (max + 1) in the Employee ID box. */
    private void suggestNextId() {
        try {
            int next = Util.count(dbCon, "SELECT NVL(MAX(emp_id), 0) + 1 FROM a_employee");
            txtEmpId.setText(String.valueOf(next));
        } catch (SQLException e) {
            txtEmpId.setText("");
        }
    }

    /** Hides all the error labels. */
    private void clearErrorLabels() {
        lblIdError.setVisible(false);
        lblFirstError.setVisible(false);
        lblLastError.setVisible(false);
        lblRoleError.setVisible(false);
        lblSalaryError.setVisible(false);
    }

    /** Clears the input fields and error labels and suggests a new id. */
    private void clearFields() {
        txtFirst.setText("");
        txtLast.setText("");
        txtRole.setText("");
        txtSalary.setText("");
        if (cmbAirport.getItemCount() > 0) {
            cmbAirport.setSelectedIndex(0);
        }
        cmbAirline.setSelectedIndex(0);
        cmbSupervisor.setSelectedIndex(0);
        clearErrorLabels();
        suggestNextId();
    }

    /**
     * Validates the salary text. Returns the salary as BigDecimal or null
     * if it is not a positive number with at most 8 digits and 2 decimals.
     */
    static BigDecimal parseSalary(String text) {
        try {
            BigDecimal s = new BigDecimal(text.trim());
            if (s.signum() <= 0 || s.scale() > 2 || s.compareTo(new BigDecimal("99999999.99")) > 0) {
                return null;
            }
            return s;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Validates the input, checks the id is free and inserts the employee. */
    private void addStaff() {
        String idText = txtEmpId.getText().trim();
        String first = txtFirst.getText().trim();
        String last = txtLast.getText().trim();
        String role = txtRole.getText().trim();
        boolean ok = true;
        clearErrorLabels();

        if (!idText.matches("[0-9]{1,6}") || Integer.parseInt(idText) < 1) {
            Util.showFieldError(lblIdError, "ID must be a number from 1 to 999999");
            ok = false;
        }
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
        BigDecimal salary = parseSalary(txtSalary.getText());
        if (salary == null) {
            Util.showFieldError(lblSalaryError, "Salary must be a positive number (max 2 decimals)");
            ok = false;
        }
        if (cmbAirport.getItemCount() == 0) {
            Util.error(this, "There are no airports. Add an airport first.");
            ok = false;
        }
        if (!ok) {
            return;
        }
        try {
            int id = Integer.parseInt(idText);
            if (Util.count(dbCon, "SELECT COUNT(*) FROM a_employee WHERE emp_id = ?", id) > 0) {
                Util.showFieldError(lblIdError, "This employee ID already exists");
                return;
            }
            // codes / ids of the selected names
            String airport = airportCodes.get(cmbAirport.getSelectedIndex());
            String airline = airlineCodes.get(cmbAirline.getSelectedIndex());
            Integer supervisor = Util.toInt(supervisorIds.get(cmbSupervisor.getSelectedIndex()));

            Util.update(dbCon, "INSERT INTO a_employee (emp_id, first_name, last_name, role, salary, "
                    + "airport_code, airline_code, supervisor_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                    id, first, last, role, salary, airport, airline, supervisor);
            Util.info(this, "Employee " + first + " " + last + " was added successfully.");
            clearFields();
            loadCombos(); // the new employee can now be a supervisor
        } catch (SQLException e) {
            Util.sqlError(this, "adding employee.", e);
        }
    }

    /** Handles the Add and Clear buttons. */
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == btnAdd) {
            addStaff();
        } else if (e.getSource() == btnClear) {
            clearFields();
        }
    }
}
