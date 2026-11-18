package jdbcgui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.WindowConstants;

/**
 * UpdateDeleteAirline: form used to browse (Next / Previous), update and
 * delete rows of the AIRLINE table. The airline code is the primary key so
 * it cannot be edited. An airline that still has aircraft or employees is
 * not deleted. Both update and delete ask the user to confirm first.
 */
public class UpdateDeleteAirline extends JFrame implements ActionListener {

    myDBCon dbCon;
    ResultSet rs; // all airlines, scrollable

    JTextField txtCode = new JTextField(20);
    JTextField txtName = new JTextField(20);
    JTextField txtCountry = new JTextField(20);
    JLabel lblNameError = Util.makeErrorLabel();
    JLabel lblCountryError = Util.makeErrorLabel();
    JButton btnPrevious = new JButton("< Previous");
    JButton btnNext = new JButton("Next >");
    JButton btnUpdate = new JButton("Update");
    JButton btnDelete = new JButton("Delete");

    /** Builds the form, connects to the database and shows the first airline. */
    public UpdateDeleteAirline() {
        setTitle("Update/Delete Airline");
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
        txtCode.setEditable(false); // primary key: cannot be edited

        JPanel form = Util.makeFormPanel();
        Util.addRow(form, 0, "Airline code:", txtCode, null);
        Util.addRow(form, 1, "Airline name:", txtName, lblNameError);
        Util.addRow(form, 2, "Country:", txtCountry, lblCountryError);

        JPanel buttons = new JPanel(new FlowLayout());
        JButton[] all = {btnPrevious, btnNext, btnUpdate, btnDelete};
        for (int i = 0; i < all.length; i++) {
            all[i].setFont(Util.LABEL_FONT);
            all[i].addActionListener(this);
            buttons.add(all[i]);
        }

        JPanel top = new JPanel(new FlowLayout());
        top.add(Util.makeTitle("Update/Delete Airline"));

        getContentPane().setLayout(new BorderLayout());
        form.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        getContentPane().add(top, BorderLayout.NORTH);
        getContentPane().add(form, BorderLayout.CENTER);
        getContentPane().add(buttons, BorderLayout.SOUTH);
    }

    /**
     * Reads all airlines from the database and shows the airline at the
     * given row number (or the last row if there are fewer rows).
     */
    private void loadData(int targetRow) {
        try {
            rs = dbCon.executeStatement("SELECT airline_code, airline_name, airline_country "
                    + "FROM airline ORDER BY airline_code ASC");
            if (rs.last()) {
                int total = rs.getRow();
                rs.absolute(Math.max(1, Math.min(targetRow, total)));
                populateFields();
            } else {
                clearFields(); // table is empty
            }
        } catch (SQLException e) {
            Util.sqlError(this, "loading airlines.", e);
        }
        enableDisableButtons();
    }

    /** Clears the fields when there is no airline to show. */
    private void clearFields() {
        txtCode.setText("");
        txtName.setText("");
        txtCountry.setText("");
    }

    /** Shows the current row in the fields. */
    private void populateFields() throws SQLException {
        lblNameError.setVisible(false);
        lblCountryError.setVisible(false);
        txtCode.setText(rs.getString("airline_code"));
        txtName.setText(rs.getString("airline_name"));
        txtCountry.setText(rs.getString("airline_country"));
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

    /** Moves to the next airline. */
    private void moveNext() {
        try {
            if (!rs.isLast()) {
                rs.next();
                populateFields();
            }
        } catch (SQLException e) {
            Util.sqlError(this, "moving to next airline.", e);
        }
        enableDisableButtons();
    }

    /** Moves to the previous airline. */
    private void movePrevious() {
        try {
            if (!rs.isFirst()) {
                rs.previous();
                populateFields();
            }
        } catch (SQLException e) {
            Util.sqlError(this, "moving to previous airline.", e);
        }
        enableDisableButtons();
    }

    /** Validates the editable fields; shows error labels and returns true if all are valid. */
    private boolean validateInput(String name, String country) {
        boolean ok = true;
        lblNameError.setVisible(false);
        lblCountryError.setVisible(false);
        if (name.length() < 1 || name.length() > 60) {
            Util.showFieldError(lblNameError, "Name must be 1 to 60 characters");
            ok = false;
        }
        if (country.length() < 1 || country.length() > 40 || !country.matches("[\\p{L}][\\p{L} .'-]*")) {
            Util.showFieldError(lblCountryError, "Country must be 1 to 40 letters");
            ok = false;
        }
        return ok;
    }

    /** Validates, asks for confirmation and updates the shown airline. */
    private void updateAirline() {
        String code = txtCode.getText();
        String name = txtName.getText().trim();
        String country = txtCountry.getText().trim();

        if (!validateInput(name, country)) {
            return;
        }
        try {
            // the name is UNIQUE: it must not belong to another airline
            if (Util.count(dbCon, "SELECT COUNT(*) FROM airline WHERE UPPER(airline_name) = UPPER(?) "
                    + "AND airline_code <> ?", name, code) > 0) {
                Util.showFieldError(lblNameError, "Another airline already has this name");
                return;
            }
            if (!Util.confirm(this, "Update airline " + code + "?", "Confirm Update")) {
                return; // user clicked No: nothing changes
            }
            Util.update(dbCon, "UPDATE airline SET airline_name = ?, airline_country = ? "
                    + "WHERE airline_code = ?", name, country, code);
            Util.info(this, "Airline " + code + " was updated successfully.");
            loadData(currentRow());
        } catch (SQLException e) {
            Util.sqlError(this, "updating airline.", e);
        }
    }

    /**
     * Asks for confirmation and deletes the shown airline. An airline that
     * still has aircraft or employees is not deleted.
     */
    private void deleteAirline() {
        String code = txtCode.getText();
        try {
            int aircraft = Util.count(dbCon, "SELECT COUNT(*) FROM aircraft WHERE airline_code = ?", code);
            int employees = Util.count(dbCon, "SELECT COUNT(*) FROM a_employee WHERE airline_code = ?", code);
            if (aircraft > 0 || employees > 0) {
                Util.error(this, "Airline " + code + " cannot be deleted because it has "
                        + aircraft + " aircraft and " + employees
                        + " employee(s). Move or delete them first.");
                return;
            }
            if (!Util.confirm(this, "Delete airline " + code + "?", "Confirm Delete")) {
                return; // user clicked No: nothing changes
            }
            Util.update(dbCon, "DELETE FROM airline WHERE airline_code = ?", code);
            Util.info(this, "Airline " + code + " was deleted successfully.");
            loadData(currentRow());
        } catch (SQLException e) {
            if (e.getErrorCode() == 2292) { // ORA-02292: child records found
                Util.error(this, "Airline " + code + " cannot be deleted because other records still use it.");
            } else {
                Util.sqlError(this, "deleting airline.", e);
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
            updateAirline();
        } else if (src == btnDelete) {
            deleteAirline();
        }
    }
}
