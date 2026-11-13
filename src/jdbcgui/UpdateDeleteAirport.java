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
 * UpdateDeleteAirport: form used to browse (Next / Previous), update and
 * delete rows of the AIRPORT table. The airport code is the primary key so
 * it cannot be edited. An airport that still has employees is not deleted.
 * Both update and delete ask the user to confirm first.
 */
public class UpdateDeleteAirport extends JFrame implements ActionListener {

    myDBCon dbCon;
    ResultSet rs; // all airports, scrollable
    ArrayList<String> mgrIds = new ArrayList<String>(); // emp_id of each name in the manager combo box

    JTextField txtCode = new JTextField(20);
    JTextField txtName = new JTextField(20);
    JTextField txtCity = new JTextField(20);
    JTextField txtCountry = new JTextField(20);
    JComboBox<String> cmbManager = new JComboBox<String>();
    JLabel lblNameError = Util.makeErrorLabel();
    JLabel lblCityError = Util.makeErrorLabel();
    JLabel lblCountryError = Util.makeErrorLabel();
    JButton btnPrevious = new JButton("< Previous");
    JButton btnNext = new JButton("Next >");
    JButton btnUpdate = new JButton("Update");
    JButton btnDelete = new JButton("Delete");

    /** Builds the form, connects to the database and shows the first airport. */
    public UpdateDeleteAirport() {
        setTitle("Update/Delete Airport");
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
        Util.addRow(form, 0, "Airport code:", txtCode, null);
        Util.addRow(form, 1, "Airport name:", txtName, lblNameError);
        Util.addRow(form, 2, "City:", txtCity, lblCityError);
        Util.addRow(form, 3, "Country:", txtCountry, lblCountryError);
        Util.addRow(form, 4, "Manager:", cmbManager, null);

        JPanel buttons = new JPanel(new FlowLayout());
        JButton[] all = {btnPrevious, btnNext, btnUpdate, btnDelete};
        for (int i = 0; i < all.length; i++) {
            all[i].setFont(Util.LABEL_FONT);
            all[i].addActionListener(this);
            buttons.add(all[i]);
        }

        JPanel top = new JPanel(new FlowLayout());
        top.add(Util.makeTitle("Update/Delete Airport"));

        getContentPane().setLayout(new BorderLayout());
        form.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        getContentPane().add(top, BorderLayout.NORTH);
        getContentPane().add(form, BorderLayout.CENTER);
        getContentPane().add(buttons, BorderLayout.SOUTH);
    }

    /**
     * Reads all airports from the database and shows the airport at the
     * given row number (or the last row if there are fewer rows).
     */
    private void loadData(int targetRow) {
        try {
            rs = dbCon.executeStatement("SELECT airport_code, airport_name, airport_city, "
                    + "airport_country, manager_id FROM airport ORDER BY airport_code ASC");
            if (rs.last()) {
                int total = rs.getRow();
                rs.absolute(Math.max(1, Math.min(targetRow, total)));
                populateFields();
            } else {
                clearFields(); // table is empty
            }
        } catch (SQLException e) {
            Util.sqlError(this, "loading airports.", e);
        }
        enableDisableButtons();
    }

    /** Clears the fields when there is no airport to show. */
    private void clearFields() {
        txtCode.setText("");
        txtName.setText("");
        txtCity.setText("");
        txtCountry.setText("");
        cmbManager.removeAllItems();
        mgrIds.clear();
    }

    /**
     * Shows the current row in the fields. The manager combo box lists the
     * employees who manage no airport plus the current manager of this airport,
     * and the manager's name is selected using indexOf() on the id list.
     */
    private void populateFields() throws SQLException {
        lblNameError.setVisible(false);
        lblCityError.setVisible(false);
        lblCountryError.setVisible(false);

        txtCode.setText(rs.getString("airport_code"));
        txtName.setText(rs.getString("airport_name"));
        txtCity.setText(rs.getString("airport_city"));
        txtCountry.setText(rs.getString("airport_country"));

        String currentMgr = rs.getString("manager_id"); // null if no manager
        String sql = Util.EMPLOYEE_NAMES_SQL
                + "WHERE emp_id NOT IN (SELECT manager_id FROM airport WHERE manager_id IS NOT NULL)";
        if (currentMgr != null) {
            sql = sql + " OR emp_id = " + Integer.parseInt(currentMgr);
        }
        sql = sql + " ORDER BY first_name, last_name";
        Util.fillCombo(dbCon, cmbManager, mgrIds, sql, "(No manager)");

        int idx = mgrIds.indexOf(currentMgr); // null finds the "(No manager)" item
        cmbManager.setSelectedIndex(idx < 0 ? 0 : idx);
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

    /** Moves to the next airport. */
    private void moveNext() {
        try {
            if (!rs.isLast()) {
                rs.next();
                populateFields();
            }
        } catch (SQLException e) {
            Util.sqlError(this, "moving to next airport.", e);
        }
        enableDisableButtons();
    }

    /** Moves to the previous airport. */
    private void movePrevious() {
        try {
            if (!rs.isFirst()) {
                rs.previous();
                populateFields();
            }
        } catch (SQLException e) {
            Util.sqlError(this, "moving to previous airport.", e);
        }
        enableDisableButtons();
    }

    /** Validates the editable fields; shows error labels and returns true if all are valid. */
    private boolean validateInput(String name, String city, String country) {
        boolean ok = true;
        lblNameError.setVisible(false);
        lblCityError.setVisible(false);
        lblCountryError.setVisible(false);
        if (name.length() < 1 || name.length() > 80) {
            Util.showFieldError(lblNameError, "Name must be 1 to 80 characters");
            ok = false;
        }
        if (city.length() < 1 || city.length() > 40) {
            Util.showFieldError(lblCityError, "City must be 1 to 40 characters");
            ok = false;
        }
        if (country.length() < 1 || country.length() > 40) {
            Util.showFieldError(lblCountryError, "Country must be 1 to 40 characters");
            ok = false;
        }
        return ok;
    }

    /** Validates, asks for confirmation and updates the shown airport. */
    private void updateAirport() {
        String code = txtCode.getText();
        String name = txtName.getText().trim();
        String city = txtCity.getText().trim();
        String country = txtCountry.getText().trim();

        if (!validateInput(name, city, country)) {
            return;
        }
        try {
            // the name is UNIQUE: it must not belong to another airport
            if (Util.count(dbCon, "SELECT COUNT(*) FROM airport WHERE UPPER(airport_name) = UPPER(?) "
                    + "AND airport_code <> ?", name, code) > 0) {
                Util.showFieldError(lblNameError, "Another airport already has this name");
                return;
            }
            if (!Util.confirm(this, "Update airport " + code + "?", "Confirm Update")) {
                return; // user clicked No: nothing changes
            }
            Integer mgr = Util.toInt(mgrIds.get(cmbManager.getSelectedIndex()));
            Util.update(dbCon, "UPDATE airport SET airport_name = ?, airport_city = ?, "
                    + "airport_country = ?, manager_id = ? WHERE airport_code = ?",
                    name, city, country, mgr, code);
            Util.info(this, "Airport " + code + " was updated successfully.");
            loadData(currentRow());
        } catch (SQLException e) {
            Util.sqlError(this, "updating airport.", e);
        }
    }

    /**
     * Asks for confirmation and deletes the shown airport. An airport that
     * still has employees is not deleted.
     */
    private void deleteAirport() {
        String code = txtCode.getText();
        try {
            int employees = Util.count(dbCon, "SELECT COUNT(*) FROM a_employee WHERE airport_code = ?", code);
            if (employees > 0) {
                Util.error(this, "Airport " + code + " cannot be deleted because it has "
                        + employees + " employee(s). Move or delete them first.");
                return;
            }
            if (!Util.confirm(this, "Delete airport " + code + "?", "Confirm Delete")) {
                return; // user clicked No: nothing changes
            }
            Util.update(dbCon, "DELETE FROM airport WHERE airport_code = ?", code);
            Util.info(this, "Airport " + code + " was deleted successfully.");
            loadData(currentRow());
        } catch (SQLException e) {
            if (e.getErrorCode() == 2292) { // ORA-02292: child records found
                Util.error(this, "Airport " + code + " cannot be deleted because other records still use it.");
            } else {
                Util.sqlError(this, "deleting airport.", e);
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
            updateAirport();
        } else if (src == btnDelete) {
            deleteAirport();
        }
    }
}
