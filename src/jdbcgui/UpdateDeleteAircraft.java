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
 * UpdateDeleteAircraft: form used to browse (Next / Previous), update and
 * delete rows of the AIRCRAFT table. The aircraft ID is the primary key so
 * it cannot be edited. An aircraft that is still used by flights is not
 * deleted. Both update and delete ask the user to confirm first.
 */
public class UpdateDeleteAircraft extends JFrame implements ActionListener {

    myDBCon dbCon;
    ResultSet rs; // all aircraft, scrollable
    ArrayList<String> airlineCodes = new ArrayList<String>(); // airline_code of each name in the combo box

    JTextField txtId = new JTextField(20);
    JTextField txtModel = new JTextField(20);
    JTextField txtCapacity = new JTextField(20);
    JTextField txtYear = new JTextField(20);
    JComboBox<String> cmbAirline = new JComboBox<String>();
    JLabel lblModelError = Util.makeErrorLabel();
    JLabel lblCapacityError = Util.makeErrorLabel();
    JLabel lblYearError = Util.makeErrorLabel();
    JButton btnPrevious = new JButton("< Previous");
    JButton btnNext = new JButton("Next >");
    JButton btnUpdate = new JButton("Update");
    JButton btnDelete = new JButton("Delete");

    /** Builds the form, connects to the database and shows the first aircraft. */
    public UpdateDeleteAircraft() {
        setTitle("Update/Delete Aircraft");
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        buildGui();
        dbCon = new myDBCon();
        loadAirlines();
        loadData(1);
        pack();
        setResizable(false);
        setLocationRelativeTo(null); // center form in screen
    }

    /** Creates and lays out all the components of the form. */
    private void buildGui() {
        txtId.setEditable(false); // primary key: cannot be edited

        JPanel form = Util.makeFormPanel();
        Util.addRow(form, 0, "Aircraft ID:", txtId, null);
        Util.addRow(form, 1, "Model:", txtModel, lblModelError);
        Util.addRow(form, 2, "Capacity (seats):", txtCapacity, lblCapacityError);
        Util.addRow(form, 3, "Manufacture year (optional):", txtYear, lblYearError);
        Util.addRow(form, 4, "Airline:", cmbAirline, null);

        JPanel buttons = new JPanel(new FlowLayout());
        JButton[] all = {btnPrevious, btnNext, btnUpdate, btnDelete};
        for (int i = 0; i < all.length; i++) {
            all[i].setFont(Util.LABEL_FONT);
            all[i].addActionListener(this);
            buttons.add(all[i]);
        }

        JPanel top = new JPanel(new FlowLayout());
        top.add(Util.makeTitle("Update/Delete Aircraft"));

        getContentPane().setLayout(new BorderLayout());
        form.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        getContentPane().add(top, BorderLayout.NORTH);
        getContentPane().add(form, BorderLayout.CENTER);
        getContentPane().add(buttons, BorderLayout.SOUTH);
    }

    /** Fills the airline combo box with airline names (done once). */
    private void loadAirlines() {
        try {
            Util.fillCombo(dbCon, cmbAirline, airlineCodes,
                    "SELECT airline_code, airline_name || ' (' || airline_code || ')' "
                    + "FROM airline ORDER BY airline_name", null);
        } catch (SQLException e) {
            Util.sqlError(this, "loading airlines.", e);
        }
    }

    /**
     * Reads all aircraft from the database and shows the aircraft at the
     * given row number (or the last row if there are fewer rows).
     */
    private void loadData(int targetRow) {
        try {
            rs = dbCon.executeStatement("SELECT aircraft_id, model, capacity, manufacture_year, airline_code "
                    + "FROM aircraft ORDER BY aircraft_id ASC");
            if (rs.last()) {
                int total = rs.getRow();
                rs.absolute(Math.max(1, Math.min(targetRow, total)));
                populateFields();
            } else {
                clearFields(); // table is empty
            }
        } catch (SQLException e) {
            Util.sqlError(this, "loading aircraft.", e);
        }
        enableDisableButtons();
    }

    /** Clears the fields when there is no aircraft to show. */
    private void clearFields() {
        txtId.setText("");
        txtModel.setText("");
        txtCapacity.setText("");
        txtYear.setText("");
    }

    /** Shows the current row in the fields; the airline name is selected using indexOf() on the code list. */
    private void populateFields() throws SQLException {
        lblModelError.setVisible(false);
        lblCapacityError.setVisible(false);
        lblYearError.setVisible(false);

        txtId.setText(rs.getString("aircraft_id"));
        txtModel.setText(rs.getString("model"));
        txtCapacity.setText(rs.getString("capacity"));
        String year = rs.getString("manufacture_year"); // null if no year
        txtYear.setText(year == null ? "" : year);

        int idx = airlineCodes.indexOf(rs.getString("airline_code"));
        if (idx >= 0) {
            cmbAirline.setSelectedIndex(idx);
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

    /** Moves to the next aircraft. */
    private void moveNext() {
        try {
            if (!rs.isLast()) {
                rs.next();
                populateFields();
            }
        } catch (SQLException e) {
            Util.sqlError(this, "moving to next aircraft.", e);
        }
        enableDisableButtons();
    }

    /** Moves to the previous aircraft. */
    private void movePrevious() {
        try {
            if (!rs.isFirst()) {
                rs.previous();
                populateFields();
            }
        } catch (SQLException e) {
            Util.sqlError(this, "moving to previous aircraft.", e);
        }
        enableDisableButtons();
    }

    /** Validates the editable fields; shows error labels and returns true if all are valid. */
    private boolean validateInput(String model, String capacity, String year) {
        boolean ok = true;
        lblModelError.setVisible(false);
        lblCapacityError.setVisible(false);
        lblYearError.setVisible(false);

        if (model.length() < 1 || model.length() > 40) {
            Util.showFieldError(lblModelError, "Model must be 1 to 40 characters");
            ok = false;
        }
        if (!capacity.matches("[0-9]{1,4}") || Integer.parseInt(capacity) == 0) {
            Util.showFieldError(lblCapacityError, "Capacity must be a whole number from 1 to 9999");
            ok = false;
        }
        if (year.length() > 0) {
            int maxYear = java.time.Year.now().getValue() + 1;
            if (!year.matches("[0-9]{4}") || Integer.parseInt(year) < 1950 || Integer.parseInt(year) > maxYear) {
                Util.showFieldError(lblYearError, "Year must be between 1950 and " + maxYear);
                ok = false;
            }
        }
        return ok;
    }

    /** Validates, asks for confirmation and updates the shown aircraft. */
    private void updateAircraft() {
        String id = txtId.getText();
        String model = txtModel.getText().trim();
        String capacity = txtCapacity.getText().trim();
        String year = txtYear.getText().trim();

        if (!validateInput(model, capacity, year)) {
            return;
        }
        try {
            if (!Util.confirm(this, "Update aircraft " + id + "?", "Confirm Update")) {
                return; // user clicked No: nothing changes
            }
            String airline = airlineCodes.get(cmbAirline.getSelectedIndex());
            Util.update(dbCon, "UPDATE aircraft SET model = ?, capacity = ?, manufacture_year = ?, "
                    + "airline_code = ? WHERE aircraft_id = ?",
                    model, Integer.valueOf(capacity), Util.toInt(year), airline, Integer.valueOf(id));
            Util.info(this, "Aircraft " + id + " was updated successfully.");
            loadData(currentRow());
        } catch (SQLException e) {
            Util.sqlError(this, "updating aircraft.", e);
        }
    }

    /**
     * Asks for confirmation and deletes the shown aircraft. An aircraft that
     * is still used by flights is not deleted.
     */
    private void deleteAircraft() {
        String id = txtId.getText();
        try {
            int flights = Util.count(dbCon, "SELECT COUNT(*) FROM flight WHERE aircraft_id = ?", Integer.valueOf(id));
            if (flights > 0) {
                Util.error(this, "Aircraft " + id + " cannot be deleted because it is used by "
                        + flights + " flight(s). Change or delete those flights first.");
                return;
            }
            if (!Util.confirm(this, "Delete aircraft " + id + "?", "Confirm Delete")) {
                return; // user clicked No: nothing changes
            }
            Util.update(dbCon, "DELETE FROM aircraft WHERE aircraft_id = ?", Integer.valueOf(id));
            Util.info(this, "Aircraft " + id + " was deleted successfully.");
            loadData(currentRow());
        } catch (SQLException e) {
            if (e.getErrorCode() == 2292) { // ORA-02292: child records found
                Util.error(this, "Aircraft " + id + " cannot be deleted because other records still use it.");
            } else {
                Util.sqlError(this, "deleting aircraft.", e);
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
            updateAircraft();
        } else if (src == btnDelete) {
            deleteAircraft();
        }
    }
}
