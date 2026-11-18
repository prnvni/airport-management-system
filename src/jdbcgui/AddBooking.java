package jdbcgui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.SQLException;
import java.time.LocalDate;
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
 * AddBooking: form used to insert a new row into the BOOKING table. The
 * Passenger combo box displays passenger names and passport numbers; the
 * matching passport_no values are stored in passportNos in the same order and
 * used as the foreign key in the INSERT statement.
 */
public class AddBooking extends JFrame implements ActionListener {

    myDBCon dbCon;
    ArrayList<String> passportNos = new ArrayList<String>();

    JTextField txtBookingId = new JTextField(20);
    JComboBox<String> cmbPassenger = new JComboBox<String>();
    JTextField txtBookingDate = new JTextField(20);
    JTextField txtAmountPaid = new JTextField(20);

    JLabel lblIdError = Util.makeErrorLabel();
    JLabel lblDateError = Util.makeErrorLabel();
    JLabel lblAmountError = Util.makeErrorLabel();

    JButton btnAdd = new JButton("Add");
    JButton btnClear = new JButton("Clear");

    /**
     * Builds the form, connects to the database, loads passengers, and sets
     * default values.
     */
    public AddBooking() {
        setTitle("Add New Booking");
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        buildGui();
        dbCon = new myDBCon();
        loadPassengers();
        suggestNextId();
        txtBookingDate.setText(LocalDate.now().toString());
        pack();
        setResizable(false);
        setLocationRelativeTo(null);
    }

    /**
     * Creates and lays out all labels, input fields, error labels, and buttons
     * on the form.
     */
    private void buildGui() {
        JPanel form = Util.makeFormPanel();
        Util.addRow(form, 0, "Booking ID:", txtBookingId, lblIdError);
        Util.addRow(form, 1, "Passenger:", cmbPassenger, null);
        Util.addRow(form, 2, "Booking Date (YYYY-MM-DD):", txtBookingDate, lblDateError);
        Util.addRow(form, 3, "Amount Paid:", txtAmountPaid, lblAmountError);

        JPanel buttons = new JPanel(new FlowLayout());
        btnAdd.setFont(Util.LABEL_FONT);
        btnClear.setFont(Util.LABEL_FONT);
        btnAdd.addActionListener(this);
        btnClear.addActionListener(this);
        buttons.add(btnAdd);
        buttons.add(btnClear);

        JPanel top = new JPanel(new FlowLayout());
        top.add(Util.makeTitle("Add New Booking"));

        getContentPane().setLayout(new BorderLayout());
        form.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        getContentPane().add(top, BorderLayout.NORTH);
        getContentPane().add(form, BorderLayout.CENTER);
        getContentPane().add(buttons, BorderLayout.SOUTH);
    }

    /**
     * Populates the Passenger combo box with formatted names and stores
     * matching passport numbers.
     */
    private void loadPassengers() {
        try {
            Util.fillCombo(dbCon, cmbPassenger, passportNos,
                    Util.PASSENGER_NAMES_SQL + "ORDER BY first_name, last_name", null);
        } catch (SQLException e) {
            Util.sqlError(this, "loading passengers.", e);
        }
    }

    /**
     * Queries the current maximum booking_id and pre-fills the next available
     * ID.
     */
    private void suggestNextId() {
        try {
            int next = Util.count(dbCon, "SELECT NVL(MAX(booking_id), 50000000) + 1 FROM booking");
            txtBookingId.setText(String.valueOf(next));
        } catch (SQLException e) {
            txtBookingId.setText("");
        }
    }

    /**
     * Hides all validation error labels on the form.
     */
    private void clearErrorLabels() {
        lblIdError.setVisible(false);
        lblDateError.setVisible(false);
        lblAmountError.setVisible(false);
    }

    /**
     * Resets all input fields to their defaults, hides error labels, and
     * suggests a new booking ID.
     */
    private void clearFields() {
        if (cmbPassenger.getItemCount() > 0) {
            cmbPassenger.setSelectedIndex(0);
        }
        txtBookingDate.setText(LocalDate.now().toString());
        txtAmountPaid.setText("");
        clearErrorLabels();
        suggestNextId();
    }

    /** Validates amount_paid >= 0 with at most 8 integer digits and 2 decimals. */
    static BigDecimal parseAmount(String text) {
        try {
            BigDecimal amt = new BigDecimal(text.trim());
            if (amt.signum() < 0 || amt.scale() > 2 || amt.compareTo(new BigDecimal("99999999.99")) > 0) {
                return null;
            }
            return amt;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Validates all inputs, checks that the booking ID is unique, and inserts
     * the row into BOOKING.
     */
    private void addBooking() {
        String idText = txtBookingId.getText().trim();
        boolean ok = true;
        clearErrorLabels();

        if (!idText.matches("[0-9]{1,8}") || Integer.parseInt(idText) < 1) {
            Util.showFieldError(lblIdError, "Booking ID must be a number from 1 to 99999999");
            ok = false;
        }
        if (cmbPassenger.getItemCount() == 0) {
            Util.error(this, "There are no passengers! Add a passenger first.");
            ok = false;
        }
        Date bookingDate = Util.parseSqlDate(txtBookingDate.getText());
        if (bookingDate == null) {
            Util.showFieldError(lblDateError, "Enter a valid date (YYYY-MM-DD)");
            ok = false;
        }
        BigDecimal amount = parseAmount(txtAmountPaid.getText());
        if (amount == null) {
            Util.showFieldError(lblAmountError, "Amount must be >= 0 (max 2 decimals)");
            ok = false;
        }

        if (!ok) {
            return;
        }

        try {
            int id = Integer.parseInt(idText);
            if (Util.count(dbCon, "SELECT COUNT(*) FROM booking WHERE booking_id = ?", id) > 0) {
                Util.showFieldError(lblIdError, "This booking ID already exists");
                return;
            }
            String passport = passportNos.get(cmbPassenger.getSelectedIndex());
            Util.update(dbCon, "INSERT INTO booking (booking_id, passport_no, booking_date, amount_paid) "
                    + "VALUES (?, ?, ?, ?)", id, passport, bookingDate, amount);
            Util.info(this, "Booking #" + id + " was added successfully.");
            clearFields();
        } catch (SQLException e) {
            Util.sqlError(this, "adding booking.", e);
        }
    }

    /**
     * Handles click events for the Add and Clear buttons.
     */
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == btnAdd) {
            addBooking();
        } else if (e.getSource() == btnClear) {
            clearFields();
        }
    }
}