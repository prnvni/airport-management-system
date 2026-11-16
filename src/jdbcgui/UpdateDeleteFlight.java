package jdbcgui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
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
 * UpdateDeleteFlight: form used to browse (Next / Previous), update and
 * delete rows of the FLIGHT table. The flight number is the primary key so
 * it cannot be edited. A flight that still has crew, gate assignments or
 * bookings is not deleted. Both update and delete ask the user to confirm first.
 */
public class UpdateDeleteFlight extends JFrame implements ActionListener {

    myDBCon dbCon;
    ResultSet rs; // all flights, scrollable
    ArrayList<String> aircraftIds = new ArrayList<String>(); // aircraft_id of each item in the aircraft combo box
    ArrayList<String> fromCodes = new ArrayList<String>();   // airport_code of each item in the From combo box
    ArrayList<String> toCodes = new ArrayList<String>();     // airport_code of each item in the To combo box

    JTextField txtNumber = new JTextField(20);
    JTextField txtDept = new JTextField(20);
    JTextField txtArr = new JTextField(20);
    JComboBox<String> cmbStatus = new JComboBox<String>(AddFlight.STATUSES);
    JComboBox<String> cmbAircraft = new JComboBox<String>();
    JComboBox<String> cmbFrom = new JComboBox<String>();
    JComboBox<String> cmbTo = new JComboBox<String>();
    JLabel lblDeptError = Util.makeErrorLabel();
    JLabel lblArrError = Util.makeErrorLabel();
    JLabel lblAircraftError = Util.makeErrorLabel();
    JLabel lblToError = Util.makeErrorLabel();
    JButton btnPrevious = new JButton("< Previous");
    JButton btnNext = new JButton("Next >");
    JButton btnUpdate = new JButton("Update");
    JButton btnDelete = new JButton("Delete");

    /** Builds the form, connects to the database and shows the first flight. */
    public UpdateDeleteFlight() {
        setTitle("Update/Delete Flight");
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        buildGui();
        dbCon = new myDBCon();
        loadChoices();
        loadData(1);
        pack();
        setResizable(false);
        setLocationRelativeTo(null); // center form in screen
    }

    /** Creates and lays out all the components of the form. */
    private void buildGui() {
        txtNumber.setEditable(false); // primary key: cannot be edited

        JPanel form = Util.makeFormPanel();
        Util.addRow(form, 0, "Flight number:", txtNumber, null);
        Util.addRow(form, 1, "Departure (yyyy-MM-dd HH:mm):", txtDept, lblDeptError);
        Util.addRow(form, 2, "Arrival (yyyy-MM-dd HH:mm):", txtArr, lblArrError);
        Util.addRow(form, 3, "Status:", cmbStatus, null);
        Util.addRow(form, 4, "Aircraft:", cmbAircraft, lblAircraftError);
        Util.addRow(form, 5, "From airport:", cmbFrom, null);
        Util.addRow(form, 6, "To airport:", cmbTo, lblToError);

        JPanel buttons = new JPanel(new FlowLayout());
        JButton[] all = {btnPrevious, btnNext, btnUpdate, btnDelete};
        for (int i = 0; i < all.length; i++) {
            all[i].setFont(Util.LABEL_FONT);
            all[i].addActionListener(this);
            buttons.add(all[i]);
        }

        JPanel top = new JPanel(new FlowLayout());
        top.add(Util.makeTitle("Update/Delete Flight"));

        getContentPane().setLayout(new BorderLayout());
        form.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        getContentPane().add(top, BorderLayout.NORTH);
        getContentPane().add(form, BorderLayout.CENTER);
        getContentPane().add(buttons, BorderLayout.SOUTH);
    }

    /** Fills the aircraft and the two airport combo boxes with names (done once). */
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

    /**
     * Reads all flights from the database and shows the flight at the
     * given row number (or the last row if there are fewer rows).
     */
    private void loadData(int targetRow) {
        try {
            rs = dbCon.executeStatement("SELECT flight_number, "
                    + "TO_CHAR(dept_time, 'YYYY-MM-DD HH24:MI') AS dept_text, "
                    + "TO_CHAR(arr_time, 'YYYY-MM-DD HH24:MI') AS arr_text, "
                    + "status, aircraft_id, dept_airport, arr_airport "
                    + "FROM flight ORDER BY flight_number ASC");
            if (rs.last()) {
                int total = rs.getRow();
                rs.absolute(Math.max(1, Math.min(targetRow, total)));
                populateFields();
            } else {
                clearFields(); // table is empty
            }
        } catch (SQLException e) {
            Util.sqlError(this, "loading flights.", e);
        }
        enableDisableButtons();
    }

    /** Clears the fields when there is no flight to show. */
    private void clearFields() {
        txtNumber.setText("");
        txtDept.setText("");
        txtArr.setText("");
    }

    /** Shows the current row in the fields; names are selected using indexOf() on the key lists. */
    private void populateFields() throws SQLException {
        lblDeptError.setVisible(false);
        lblArrError.setVisible(false);
        lblAircraftError.setVisible(false);
        lblToError.setVisible(false);

        txtNumber.setText(rs.getString("flight_number"));
        txtDept.setText(rs.getString("dept_text"));
        txtArr.setText(rs.getString("arr_text"));
        cmbStatus.setSelectedItem(rs.getString("status"));

        int idx = aircraftIds.indexOf(rs.getString("aircraft_id"));
        if (idx >= 0) {
            cmbAircraft.setSelectedIndex(idx);
        }
        idx = fromCodes.indexOf(rs.getString("dept_airport"));
        if (idx >= 0) {
            cmbFrom.setSelectedIndex(idx);
        }
        idx = toCodes.indexOf(rs.getString("arr_airport"));
        if (idx >= 0) {
            cmbTo.setSelectedIndex(idx);
        }
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

    /** Moves to the next flight. */
    private void moveNext() {
        try {
            if (!rs.isLast()) {
                rs.next();
                populateFields();
            }
        } catch (SQLException e) {
            Util.sqlError(this, "moving to next flight.", e);
        }
        enableDisableButtons();
    }

    /** Moves to the previous flight. */
    private void movePrevious() {
        try {
            if (!rs.isFirst()) {
                rs.previous();
                populateFields();
            }
        } catch (SQLException e) {
            Util.sqlError(this, "moving to previous flight.", e);
        }
        enableDisableButtons();
    }

    /** Validates the editable fields; shows error labels and returns true if all are valid. */
    private boolean validateInput(LocalDateTime dept, LocalDateTime arr) {
        boolean ok = true;
        lblDeptError.setVisible(false);
        lblArrError.setVisible(false);
        lblAircraftError.setVisible(false);
        lblToError.setVisible(false);

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
        if (fromCodes.get(cmbFrom.getSelectedIndex()).equals(toCodes.get(cmbTo.getSelectedIndex()))) {
            Util.showFieldError(lblToError, "Departure and arrival airports must be different");
            ok = false;
        }
        return ok;
    }

    /** Validates, asks for confirmation and updates the shown flight. */
    private void updateFlight() {
        String number = txtNumber.getText();
        LocalDateTime dept = AddFlight.parseTime(txtDept.getText().trim());
        LocalDateTime arr = AddFlight.parseTime(txtArr.getText().trim());

        if (!validateInput(dept, arr)) {
            return;
        }
        String status = (String) cmbStatus.getSelectedItem();
        Integer aircraft = Integer.valueOf(aircraftIds.get(cmbAircraft.getSelectedIndex()));
        String from = fromCodes.get(cmbFrom.getSelectedIndex());
        String to = toCodes.get(cmbTo.getSelectedIndex());
        Timestamp deptTs = Timestamp.valueOf(dept);
        Timestamp arrTs = Timestamp.valueOf(arr);

        try {
            // one aircraft cannot be on two flights at the same time (this flight and cancelled flights do not count)
            if (!status.equals("Cancelled") && Util.count(dbCon,
                    "SELECT COUNT(*) FROM flight WHERE aircraft_id = ? AND status <> 'Cancelled' "
                    + "AND dept_time < ? AND arr_time > ? AND flight_number <> ?",
                    aircraft, arrTs, deptTs, Integer.valueOf(number)) > 0) {
                Util.showFieldError(lblAircraftError, "This aircraft is already on another flight at that time");
                return;
            }
            if (!Util.confirm(this, "Update flight " + number + "?", "Confirm Update")) {
                return; // user clicked No: nothing changes
            }
            Util.update(dbCon, "UPDATE flight SET dept_time = ?, arr_time = ?, status = ?, "
                    + "aircraft_id = ?, dept_airport = ?, arr_airport = ? WHERE flight_number = ?",
                    deptTs, arrTs, status, aircraft, from, to, Integer.valueOf(number));
            Util.info(this, "Flight " + number + " was updated successfully.");
            loadData(currentRow());
        } catch (SQLException e) {
            Util.sqlError(this, "updating flight.", e);
        }
    }

    /**
     * Counts the rows of a dependent table that use this flight. A table that
     * does not exist yet (ORA-00942, e.g. a teammate has not created it) counts as 0.
     */
    private int countUsing(String sql, Integer flight) throws SQLException {
        try {
            return Util.count(dbCon, sql, flight);
        } catch (SQLException e) {
            if (e.getErrorCode() == 942) {
                return 0;
            }
            throw e;
        }
    }

    /**
     * Asks for confirmation and deletes the shown flight. A flight that still
     * has crew, gate assignments or bookings is not deleted.
     */
    private void deleteFlight() {
        String number = txtNumber.getText();
        Integer flight = Integer.valueOf(number);
        try {
            int crew = countUsing("SELECT COUNT(*) FROM flight_crew WHERE flight_number = ?", flight);
            int gates = countUsing("SELECT COUNT(*) FROM gate_assignment WHERE flight_number = ?", flight);
            int bookings = countUsing("SELECT COUNT(*) FROM flight_booking WHERE flight_number = ?", flight);
            if (crew > 0 || gates > 0 || bookings > 0) {
                Util.error(this, "Flight " + number + " cannot be deleted because it has "
                        + crew + " crew member(s), " + gates + " gate assignment(s) and "
                        + bookings + " booking(s). Remove them first.");
                return;
            }
            if (!Util.confirm(this, "Delete flight " + number + "?", "Confirm Delete")) {
                return; // user clicked No: nothing changes
            }
            Util.update(dbCon, "DELETE FROM flight WHERE flight_number = ?", flight);
            Util.info(this, "Flight " + number + " was deleted successfully.");
            loadData(currentRow());
        } catch (SQLException e) {
            if (e.getErrorCode() == 2292) { // ORA-02292: child records found
                Util.error(this, "Flight " + number + " cannot be deleted because other records still use it.");
            } else {
                Util.sqlError(this, "deleting flight.", e);
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
            updateFlight();
        } else if (src == btnDelete) {
            deleteFlight();
        }
    }
}
