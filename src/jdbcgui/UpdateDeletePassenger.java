package jdbcgui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.Period;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.WindowConstants;

/**
 * UpdateDeletePassenger: form used to browse (Next / Previous), update, and
 * delete rows of the PASSENGER table. Displays the derived attribute Age
 * calculated from DOB, prevents deleting passengers who still have bookings,
 * and removes linked PASSENGER_PHONE records upon confirmed deletion.
 */
public class UpdateDeletePassenger extends JFrame implements ActionListener {

    myDBCon dbCon;
    ResultSet rs;

    JTextField txtPassport = new JTextField(20);
    JTextField txtFirst = new JTextField(20);
    JTextField txtLast = new JTextField(20);
    JTextField txtStreet = new JTextField(20);
    JTextField txtCity = new JTextField(20);
    JTextField txtCountry = new JTextField(20);
    JTextField txtDob = new JTextField(20);
    JTextField txtAge = new JTextField(20);

    JLabel lblFirstError = Util.makeErrorLabel();
    JLabel lblLastError = Util.makeErrorLabel();
    JLabel lblStreetError = Util.makeErrorLabel();
    JLabel lblCityError = Util.makeErrorLabel();
    JLabel lblCountryError = Util.makeErrorLabel();
    JLabel lblDobError = Util.makeErrorLabel();

    JButton btnPrevious = new JButton("< Previous");
    JButton btnNext = new JButton("Next >");
    JButton btnUpdate = new JButton("Update");
    JButton btnDelete = new JButton("Delete");

    /**
     * Builds the form, connects to the database, and loads the first passenger
     * record.
     */
    public UpdateDeletePassenger() {
        setTitle("Update/Delete Passenger");
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        buildGui();
        dbCon = new myDBCon();
        loadData(1);
        pack();
        setResizable(false);
        setLocationRelativeTo(null);
    }

    /**
     * Creates and lays out all labels, text fields, error labels, and buttons
     * on the form.
     */
    private void buildGui() {
        txtPassport.setEditable(false); // PK cannot be edited
        txtAge.setEditable(false);      // derived attribute

        JPanel form = Util.makeFormPanel();
        Util.addRow(form, 0, "Passport No:", txtPassport, null);
        Util.addRow(form, 1, "First Name:", txtFirst, lblFirstError);
        Util.addRow(form, 2, "Last Name:", txtLast, lblLastError);
        Util.addRow(form, 3, "Street:", txtStreet, lblStreetError);
        Util.addRow(form, 4, "City:", txtCity, lblCityError);
        Util.addRow(form, 5, "Country:", txtCountry, lblCountryError);
        Util.addRow(form, 6, "DOB (YYYY-MM-DD):", txtDob, lblDobError);
        Util.addRow(form, 7, "Age (Derived):", txtAge, null);

        JPanel buttons = new JPanel(new FlowLayout());
        JButton[] all = {btnPrevious, btnNext, btnUpdate, btnDelete};
        for (int i = 0; i < all.length; i++) {
            all[i].setFont(Util.LABEL_FONT);
            all[i].addActionListener(this);
            buttons.add(all[i]);
        }

        JPanel top = new JPanel(new FlowLayout());
        top.add(Util.makeTitle("Update/Delete Passenger"));

        getContentPane().setLayout(new BorderLayout());
        form.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        getContentPane().add(top, BorderLayout.NORTH);
        getContentPane().add(form, BorderLayout.CENTER);
        getContentPane().add(buttons, BorderLayout.SOUTH);
    }

    /**
     * Queries all passengers ordered by passport_no and positions the cursor at
     * targetRow.
     *
     * @param targetRow 1-based row index to display (clamped to the valid row
     * range)
     */
    private void loadData(int targetRow) {
        try {
            rs = dbCon.executeStatement("SELECT passport_no, first_name, last_name, street, city, country, dob "
                    + "FROM passenger ORDER BY passport_no ASC");
            if (rs.last()) {
                int total = rs.getRow();
                rs.absolute(Math.max(1, Math.min(targetRow, total)));
                populateFields();
            } else {
                clearFields();
            }
        } catch (SQLException e) {
            Util.sqlError(this, "loading passengers.", e);
        }
        enableDisableButtons();
    }

    /**
     * Clears all text fields when the PASSENGER table contains no rows.
     */
    private void clearFields() {
        txtPassport.setText("");
        txtFirst.setText("");
        txtLast.setText("");
        txtStreet.setText("");
        txtCity.setText("");
        txtCountry.setText("");
        txtDob.setText("");
        txtAge.setText("");
    }

    /**
     * Populates the form fields from the current result set row and computes
     * derived Age.
     */
    private void populateFields() throws SQLException {
        lblFirstError.setVisible(false);
        lblLastError.setVisible(false);
        lblStreetError.setVisible(false);
        lblCityError.setVisible(false);
        lblCountryError.setVisible(false);
        lblDobError.setVisible(false);

        txtPassport.setText(rs.getString("passport_no"));
        txtFirst.setText(rs.getString("first_name"));
        txtLast.setText(rs.getString("last_name"));
        txtStreet.setText(rs.getString("street") == null ? "" : rs.getString("street"));
        txtCity.setText(rs.getString("city") == null ? "" : rs.getString("city"));
        txtCountry.setText(rs.getString("country") == null ? "" : rs.getString("country"));

        Date dob = rs.getDate("dob");
        if (dob != null) {
            LocalDate localDob = dob.toLocalDate();
            txtDob.setText(localDob.toString());
            int age = Period.between(localDob, LocalDate.now()).getYears();
            txtAge.setText(age + " years");
        } else {
            txtDob.setText("");
            txtAge.setText("");
        }
    }

    /**
     * Enables or disables Previous, Next, Update, and Delete buttons based on
     * cursor position.
     */
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

    /**
     * Advances the result set cursor to the next passenger record and refreshes
     * the display.
     */
    private void moveNext() {
        try {
            if (!rs.isLast()) {
                rs.next();
                populateFields();
            }
        } catch (SQLException e) {
            Util.sqlError(this, "moving to next passenger.", e);
        }
        enableDisableButtons();
    }

    /**
     * Moves the result set cursor to the previous passenger record and
     * refreshes the display.
     */
    private void movePrevious() {
        try {
            if (!rs.isFirst()) {
                rs.previous();
                populateFields();
            }
        } catch (SQLException e) {
            Util.sqlError(this, "moving to previous passenger.", e);
        }
        enableDisableButtons();
    }

    /**
     * Validates edited passenger fields, requests confirmation, and updates
     * PASSENGER.
     */
    private void updatePassenger() {
        String first = txtFirst.getText().trim();
        String last = txtLast.getText().trim();
        String street = txtStreet.getText().trim();
        String city = txtCity.getText().trim();
        String country = txtCountry.getText().trim();
        boolean ok = true;

        lblFirstError.setVisible(false);
        lblLastError.setVisible(false);
        lblStreetError.setVisible(false);
        lblCityError.setVisible(false);
        lblCountryError.setVisible(false);
        lblDobError.setVisible(false);

        String namePattern = "[\\p{L}][\\p{L} .'-]*";
        if (first.length() < 1 || first.length() > 30 || !first.matches(namePattern)) {
            Util.showFieldError(lblFirstError, "First name must be 1 to 30 letters!");
            ok = false;
        }
        if (last.length() < 1 || last.length() > 30 || !last.matches(namePattern)) {
            Util.showFieldError(lblLastError, "Last name must be 1 to 30 letters!");
            ok = false;
        }
        if (street.length() > 60) {
            Util.showFieldError(lblStreetError, "Street must be at most 60 characters!");
            ok = false;
        }
        if (city.length() > 40 || (city.length() > 0 && !city.matches(namePattern))) {
            Util.showFieldError(lblCityError, "City must be letters only (max 40)!");
            ok = false;
        }
        if (country.length() > 40 || (country.length() > 0 && !country.matches(namePattern))) {
            Util.showFieldError(lblCountryError, "Country must be letters only (max 40)!");
            ok = false;
        }

        Date dob = Util.parseSqlDate(txtDob.getText());
        if (dob == null) {
            Util.showFieldError(lblDobError, "Enter a valid date (YYYY-MM-DD)");
            ok = false;
        } else if (dob.toLocalDate().isAfter(LocalDate.now())) {
            Util.showFieldError(lblDobError, "Date of birth cannot be in the future.");
            ok = false;
        } else if (dob.toLocalDate().isBefore(LocalDate.of(1900, 1, 1))) {
            Util.showFieldError(lblDobError, "Date of birth must be 1900 or later.");
            ok = false;
        }

        if (!ok) {
            return;
        }

        String passport = txtPassport.getText();
        if (!Util.confirm(this, "Update passenger " + passport + "?", "Confirm Update")) {
            return;
        }

        try {
            Util.update(dbCon, "UPDATE passenger SET first_name = ?, last_name = ?, street = ?, "
                    + "city = ?, country = ?, dob = ? WHERE passport_no = ?",
                    first, last,
                    street.isEmpty() ? null : street,
                    city.isEmpty() ? null : city,
                    country.isEmpty() ? null : country,
                    dob, passport);
            Util.info(this, "Passenger " + passport + " was updated successfully.");
            loadData(currentRow());
        } catch (SQLException e) {
            Util.sqlError(this, "updating passenger.", e);
        }
    }

    /**
     * Checks for dependent bookings, asks for confirmation, removes any linked
     * PASSENGER_PHONE records, and deletes the current passenger from
     * PASSENGER.
     */
    private void deletePassenger() {
        String passport = txtPassport.getText();
        try {
            int bookings = Util.count(dbCon, "SELECT COUNT(*) FROM booking WHERE passport_no = ?", passport);
            if (bookings > 0) {
                Util.error(this, "This passenger has " + bookings + " booking(s) and cannot be deleted.\n"
                        + "Delete their bookings first.");
                return;
            }
            int phones = Util.count(dbCon, "SELECT COUNT(*) FROM passenger_phone WHERE passport_no = ?", passport);
            String msg = "Delete passenger " + passport + "?";
            if (phones > 0) {
                msg += "\nTheir " + phones + " phone number(s) will also be deleted.";
            }
            if (!Util.confirm(this, msg, "Confirm Delete")) {
                return;
            }
            if (phones > 0) {
                Util.update(dbCon, "DELETE FROM passenger_phone WHERE passport_no = ?", passport);
            }
            Util.update(dbCon, "DELETE FROM passenger WHERE passport_no = ?", passport);
            Util.info(this, "Passenger " + passport + " was deleted successfully.");
            loadData(currentRow());
        } catch (SQLException e) {
            if (e.getErrorCode() == 2292) {
                Util.error(this, "This passenger cannot be deleted because other records still use it.");
            } else {
                Util.sqlError(this, "deleting passenger.", e);
            }
        }
    }

    /**
     * Returns the current 1-based row position of the result set (defaults to
     * 1).
     */
    private int currentRow() {
        try {
            return Math.max(1, rs.getRow());
        } catch (SQLException e) {
            return 1;
        }
    }

    /**
     * Handles click events for the Next, Previous, Update, and Delete buttons.
     */
    public void actionPerformed(ActionEvent e) {
        Object src = e.getSource();
        if (src == btnNext) {
            moveNext();
        } else if (src == btnPrevious) {
            movePrevious();
        } else if (src == btnUpdate) {
            updatePassenger();
        } else if (src == btnDelete) {
            deletePassenger();
        }
    }
}