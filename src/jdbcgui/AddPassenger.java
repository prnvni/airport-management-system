package jdbcgui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Date;
import java.sql.SQLException;
import java.time.LocalDate;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.WindowConstants;

/**
 * AddPassenger: form used to insert a new row into the PASSENGER table.
 * Enforces column length limits, primary key uniqueness, and the business rule
 * that date of birth (DOB) cannot be in the future.
 */
public class AddPassenger extends JFrame implements ActionListener {

    myDBCon dbCon;

    JTextField txtPassport = new JTextField(20);
    JTextField txtFirst = new JTextField(20);
    JTextField txtLast = new JTextField(20);
    JTextField txtStreet = new JTextField(20);
    JTextField txtCity = new JTextField(20);
    JTextField txtCountry = new JTextField(20);
    JTextField txtDob = new JTextField(20);

    JLabel lblPassportError = Util.makeErrorLabel();
    JLabel lblFirstError = Util.makeErrorLabel();
    JLabel lblLastError = Util.makeErrorLabel();
    JLabel lblStreetError = Util.makeErrorLabel();
    JLabel lblCityError = Util.makeErrorLabel();
    JLabel lblCountryError = Util.makeErrorLabel();
    JLabel lblDobError = Util.makeErrorLabel();

    JButton btnAdd = new JButton("Add");
    JButton btnClear = new JButton("Clear");

    /**
     * Builds the form, opens the database connection, and centers the window.
     */
    public AddPassenger() {
        setTitle("Add New Passenger");
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        buildGui();
        dbCon = new myDBCon();
        pack();
        setResizable(false);
        setLocationRelativeTo(null);
    }

    /**
     * Creates and lays out all labels, text fields, error labels, and buttons
     * on the form.
     */
    private void buildGui() {
        JPanel form = Util.makeFormPanel();
        Util.addRow(form, 0, "Passport No:", txtPassport, lblPassportError);
        Util.addRow(form, 1, "First Name:", txtFirst, lblFirstError);
        Util.addRow(form, 2, "Last Name:", txtLast, lblLastError);
        Util.addRow(form, 3, "Street:", txtStreet, lblStreetError);
        Util.addRow(form, 4, "City:", txtCity, lblCityError);
        Util.addRow(form, 5, "Country:", txtCountry, lblCountryError);
        Util.addRow(form, 6, "DOB (YYYY-MM-DD):", txtDob, lblDobError);

        JPanel buttons = new JPanel(new FlowLayout());
        btnAdd.setFont(Util.LABEL_FONT);
        btnClear.setFont(Util.LABEL_FONT);
        btnAdd.addActionListener(this);
        btnClear.addActionListener(this);
        buttons.add(btnAdd);
        buttons.add(btnClear);

        JPanel top = new JPanel(new FlowLayout());
        top.add(Util.makeTitle("Add New Passenger"));

        getContentPane().setLayout(new BorderLayout());
        form.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        getContentPane().add(top, BorderLayout.NORTH);
        getContentPane().add(form, BorderLayout.CENTER);
        getContentPane().add(buttons, BorderLayout.SOUTH);
    }

    /**
     * Hides all validation error labels on the form.
     */
    private void clearErrorLabels() {
        lblPassportError.setVisible(false);
        lblFirstError.setVisible(false);
        lblLastError.setVisible(false);
        lblStreetError.setVisible(false);
        lblCityError.setVisible(false);
        lblCountryError.setVisible(false);
        lblDobError.setVisible(false);
    }

    /**
     * Clears all input fields and resets validation error labels.
     */
    private void clearFields() {
        txtPassport.setText("");
        txtFirst.setText("");
        txtLast.setText("");
        txtStreet.setText("");
        txtCity.setText("");
        txtCountry.setText("");
        txtDob.setText("");
        clearErrorLabels();
        pack();
    }

    /**
     * Validates user inputs, checks passport uniqueness, and inserts the new
     * passenger into PASSENGER.
     */
    private void addPassenger() {
        String passport = txtPassport.getText().trim().toUpperCase();
        String first = txtFirst.getText().trim();
        String last = txtLast.getText().trim();
        String street = txtStreet.getText().trim();
        String city = txtCity.getText().trim();
        String country = txtCountry.getText().trim();
        boolean ok = true;
        clearErrorLabels();

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
            Util.showFieldError(lblDobError, "Enter a valid date (YYYY-MM-DD.)");
            ok = false;
        } else if (dob.toLocalDate().isAfter(LocalDate.now())) {
            Util.showFieldError(lblDobError, "Date of birth cannot be in the future!");
            ok = false;
        } else if (dob.toLocalDate().isBefore(LocalDate.of(1900, 1, 1))) {
            Util.showFieldError(lblDobError, "Date of birth must be 1900 or later!");
            ok = false;
        }

        if (!ok) {
            pack();
            return;
        }

        try {
            if (Util.count(dbCon, "SELECT COUNT(*) FROM passenger WHERE UPPER(passport_no) = ?", passport) > 0) {
                Util.showFieldError(lblPassportError, "This passport number already exists");
                pack();
                return;
            }
            Util.update(dbCon, "INSERT INTO passenger (passport_no, first_name, last_name, street, city, country, dob) "
                    + "VALUES (?, ?, ?, ?, ?, ?, ?)",
                    passport, first, last,
                    street.isEmpty() ? null : street,
                    city.isEmpty() ? null : city,
                    country.isEmpty() ? null : country,
                    dob);
            Util.info(this, "Passenger " + first + " " + last + " (" + passport + ") was added successfully.");
            clearFields();
        } catch (SQLException e) {
            Util.sqlError(this, "adding passenger.", e);
        }
    }

    /**
     * Handles click events for the Add and Clear buttons.
     */
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == btnAdd) {
            addPassenger();
        } else if (e.getSource() == btnClear) {
            clearFields();
        }
    }
}
