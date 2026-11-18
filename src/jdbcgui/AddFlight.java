package jdbcgui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
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
 * AddFlight: form used to insert a new row into the FLIGHT table.
 * The aircraft and airport combo boxes show names; the matching ids / codes
 * are kept in the aircraftIds, fromCodes and toCodes lists in the same order.
 * Times are typed as yyyy-MM-dd HH:mm. Rules checked before the insert:
 * arrival after departure, different airports, and the aircraft must not
 * be on another (non-cancelled) flight at the same time.
 */
public class AddFlight extends JFrame implements ActionListener {

    /** Allowed flight statuses (same list as the CHECK constraint); also used by UpdateDeleteFlight. */
    static final String[] STATUSES = {"Scheduled", "Boarding", "Departed", "Delayed", "Cancelled", "Landed"};
    static final DateTimeFormatter TIME_FORMAT
            = DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm").withResolverStyle(ResolverStyle.STRICT);

    myDBCon dbCon;
    ArrayList<String> aircraftIds = new ArrayList<String>(); // aircraft_id of each item in the aircraft combo box
    ArrayList<String> fromCodes = new ArrayList<String>();   // airport_code of each item in the From combo box
    ArrayList<String> toCodes = new ArrayList<String>();     // airport_code of each item in the To combo box

    JTextField txtNumber = new JTextField(20);
    JTextField txtDept = new JTextField(20);
    JTextField txtArr = new JTextField(20);
    JComboBox<String> cmbStatus = new JComboBox<String>(STATUSES);
    JComboBox<String> cmbAircraft = new JComboBox<String>();
    JComboBox<String> cmbFrom = new JComboBox<String>();
    JComboBox<String> cmbTo = new JComboBox<String>();
    JLabel lblNumberError = Util.makeErrorLabel();
    JLabel lblDeptError = Util.makeErrorLabel();
    JLabel lblArrError = Util.makeErrorLabel();
    JLabel lblAircraftError = Util.makeErrorLabel();
    JLabel lblFromError = Util.makeErrorLabel();
    JLabel lblToError = Util.makeErrorLabel();
    JButton btnAdd = new JButton("Add");
    JButton btnClear = new JButton("Clear");

    /** Builds the form, connects to the database and loads the aircraft and airport names. */
    public AddFlight() {
        setTitle("Add New Flight");
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        buildGui();
        dbCon = new myDBCon();
        loadChoices();
        pack();
        setResizable(false);
        setLocationRelativeTo(null); // center form in screen
    }

    /** Parses "yyyy-MM-dd HH:mm"; returns null if the text is not a real date and time. */
    static LocalDateTime parseTime(String text) {
        try {
            return LocalDateTime.parse(text, TIME_FORMAT);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    /** Creates and lays out all the components of the form. */
    private void buildGui() {
        JPanel form = Util.makeFormPanel();
        Util.addRow(form, 0, "Flight number (1-7 digits):", txtNumber, lblNumberError);
        Util.addRow(form, 1, "Departure (yyyy-MM-dd HH:mm):", txtDept, lblDeptError);
        Util.addRow(form, 2, "Arrival (yyyy-MM-dd HH:mm):", txtArr, lblArrError);
        Util.addRow(form, 3, "Status:", cmbStatus, null);
        Util.addRow(form, 4, "Aircraft:", cmbAircraft, lblAircraftError);
        Util.addRow(form, 5, "From airport:", cmbFrom, lblFromError);
        Util.addRow(form, 6, "To airport:", cmbTo, lblToError);

        JPanel buttons = new JPanel(new FlowLayout());
        btnAdd.setFont(Util.LABEL_FONT);
        btnClear.setFont(Util.LABEL_FONT);
        btnAdd.addActionListener(this);
        btnClear.addActionListener(this);
        buttons.add(btnAdd);
        buttons.add(btnClear);

        JPanel top = new JPanel(new FlowLayout());
        top.add(Util.makeTitle("Add New Flight"));

        getContentPane().setLayout(new BorderLayout());
        form.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        getContentPane().add(top, BorderLayout.NORTH);
        getContentPane().add(form, BorderLayout.CENTER);
        getContentPane().add(buttons, BorderLayout.SOUTH);
    }

    /** Fills the aircraft and the two airport combo boxes with names. */
    private void loadChoices() {
        String airportSql = "SELECT airport_code, airport_code || ' - ' || airport_name "
                + "FROM airport ORDER BY airport_code";
        try {
            Util.fillCombo(dbCon, cmbAircraft, aircraftIds,
                    "SELECT aircraft_id, aircraft_id || ' - ' || model || ' (' || airline_code || ')' "
                    + "FROM aircraft ORDER BY aircraft_id", null);
            Util.fillCombo(dbCon, cmbFrom, fromCodes, airportSql, null);
            Util.fillCombo(dbCon, cmbTo, toCodes, airportSql, null);
        } catch (SQLException e) {
            Util.sqlError(this, "loading aircraft and airports.", e);
        }
    }

    /** Hides all the error labels. */
    private void clearErrorLabels() {
        lblNumberError.setVisible(false);
        lblDeptError.setVisible(false);
        lblArrError.setVisible(false);
        lblAircraftError.setVisible(false);
        lblFromError.setVisible(false);
        lblToError.setVisible(false);
    }

    /** Clears all the input fields and the error labels. */
    private void clearFields() {
        txtNumber.setText("");
        txtDept.setText("");
        txtArr.setText("");
        cmbStatus.setSelectedIndex(0);
        if (cmbAircraft.getItemCount() > 0) {
            cmbAircraft.setSelectedIndex(0);
        }
        if (cmbFrom.getItemCount() > 0) {
            cmbFrom.setSelectedIndex(0);
        }
        if (cmbTo.getItemCount() > 0) {
            cmbTo.setSelectedIndex(0);
        }
        clearErrorLabels();
    }

    /**
     * Checks all the fields. Shows an error label for every invalid field
     * and returns true only if everything is valid.
     */
    private boolean validateInput(String number, LocalDateTime dept, LocalDateTime arr) {
        boolean ok = true;
        clearErrorLabels();

        if (!number.matches("[0-9]{1,7}") || Integer.parseInt(number) == 0) {
            Util.showFieldError(lblNumberError, "Flight number must be a whole number up to 7 digits");
            ok = false;
        }
        if (dept == null) {
            Util.showFieldError(lblDeptError, "Use the format yyyy-MM-dd HH:mm, e.g. 2026-12-01 14:30");
            ok = false;
        }
        if (arr == null) {
            Util.showFieldError(lblArrError, "Use the format yyyy-MM-dd HH:mm, e.g. 2026-12-01 18:45");
            ok = false;
        }
        if (dept != null && arr != null && !arr.isAfter(dept)) {
            Util.showFieldError(lblArrError, "Arrival must be after departure");
            ok = false;
        }
        if (cmbAircraft.getSelectedIndex() < 0) {
            Util.showFieldError(lblAircraftError, "Add an aircraft first");
            ok = false;
        }
        if (cmbFrom.getSelectedIndex() < 0) {
            Util.showFieldError(lblFromError, "Add an airport first");
            ok = false;
        }
        if (cmbTo.getSelectedIndex() < 0) {
            Util.showFieldError(lblToError, "Add an airport first");
            ok = false;
        }
        if (cmbFrom.getSelectedIndex() >= 0 && cmbTo.getSelectedIndex() >= 0
                && fromCodes.get(cmbFrom.getSelectedIndex()).equals(toCodes.get(cmbTo.getSelectedIndex()))) {
            Util.showFieldError(lblToError, "Departure and arrival airports must be different");
            ok = false;
        }
        if (dept != null && dept.isBefore(LocalDateTime.now())) {
            Util.showFieldError(lblDeptError, "Departure can't be in the past");
            ok = false;
        }
        return ok;
    }

    /** Validates the input, checks the rules and inserts the flight. */
    private void addFlight() {
        String number = txtNumber.getText().trim();
        LocalDateTime dept = parseTime(txtDept.getText().trim());
        LocalDateTime arr = parseTime(txtArr.getText().trim());

        if (!validateInput(number, dept, arr)) {
            return;
        }
        String status = (String) cmbStatus.getSelectedItem();
        Integer aircraft = Integer.valueOf(aircraftIds.get(cmbAircraft.getSelectedIndex()));
        String from = fromCodes.get(cmbFrom.getSelectedIndex());
        String to = toCodes.get(cmbTo.getSelectedIndex());
        Timestamp deptTs = Timestamp.valueOf(dept);
        Timestamp arrTs = Timestamp.valueOf(arr);

        try {
            if (Util.count(dbCon, "SELECT COUNT(*) FROM flight WHERE flight_number = ?", Integer.valueOf(number)) > 0) {
                Util.showFieldError(lblNumberError, "This flight number already exists");
                return;
            }
            // one aircraft cannot be on two flights at the same time (cancelled flights do not count)
            if (!status.equals("Cancelled") && Util.count(dbCon,
                    "SELECT COUNT(*) FROM flight WHERE aircraft_id = ? AND status <> 'Cancelled' "
                    + "AND dept_time < ? AND arr_time > ?", aircraft, arrTs, deptTs) > 0) {
                Util.showFieldError(lblAircraftError, "This aircraft is already on another flight at that time");
                return;
            }
            Util.update(dbCon, "INSERT INTO flight (flight_number, dept_time, arr_time, status, "
                    + "aircraft_id, dept_airport, arr_airport) VALUES (?, ?, ?, ?, ?, ?, ?)",
                    Integer.valueOf(number), deptTs, arrTs, status, aircraft, from, to);
            Util.info(this, "Flight " + number + " was added successfully.");
            clearFields();
        } catch (SQLException e) {
            Util.sqlError(this, "adding flight.", e);
        }
    }

    /** Handles the Add and Clear buttons. */
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == btnAdd) {
            addFlight();
        } else if (e.getSource() == btnClear) {
            clearFields();
        }
    }
}
