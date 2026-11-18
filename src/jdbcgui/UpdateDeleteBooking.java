package jdbcgui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.math.BigDecimal;
import java.sql.Date;
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
 * UpdateDeleteBooking: form used to browse (Next / Previous), update, and
 * delete rows of the BOOKING table. The booking ID is the primary key and
 * cannot be edited. Deleting a booking also removes any linked rows in
 * FLIGHT_BOOKING after user confirmation.
 */
public class UpdateDeleteBooking extends JFrame implements ActionListener {

    myDBCon dbCon;
    ResultSet rs;
    ArrayList<String> passportNos = new ArrayList<String>();

    JTextField txtBookingId = new JTextField(20);
    JComboBox<String> cmbPassenger = new JComboBox<String>();
    JTextField txtBookingDate = new JTextField(20);
    JTextField txtAmountPaid = new JTextField(20);

    JLabel lblDateError = Util.makeErrorLabel();
    JLabel lblAmountError = Util.makeErrorLabel();

    JButton btnPrevious = new JButton("< Previous");
    JButton btnNext = new JButton("Next >");
    JButton btnUpdate = new JButton("Update");
    JButton btnDelete = new JButton("Delete");

    /**
     * Builds the form, connects to the database, and loads the first booking
     * record.
     */
    public UpdateDeleteBooking() {
        setTitle("Update/Delete Booking");
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        buildGui();
        dbCon = new myDBCon();
        loadData(1);
        pack();
        setResizable(false);
        setLocationRelativeTo(null);
    }

    /**
     * Creates and lays out all labels, input fields, error labels, and buttons
     * on the form.
     */
    private void buildGui() {
        txtBookingId.setEditable(false); // PK cannot be edited

        JPanel form = Util.makeFormPanel();
        Util.addRow(form, 0, "Booking ID:", txtBookingId, null);
        Util.addRow(form, 1, "Passenger:", cmbPassenger, null);
        Util.addRow(form, 2, "Booking Date (YYYY-MM-DD):", txtBookingDate, lblDateError);
        Util.addRow(form, 3, "Amount Paid:", txtAmountPaid, lblAmountError);

        JPanel buttons = new JPanel(new FlowLayout());
        JButton[] all = {btnPrevious, btnNext, btnUpdate, btnDelete};
        for (int i = 0; i < all.length; i++) {
            all[i].setFont(Util.LABEL_FONT);
            all[i].addActionListener(this);
            buttons.add(all[i]);
        }

        JPanel top = new JPanel(new FlowLayout());
        top.add(Util.makeTitle("Update/Delete Booking"));

        getContentPane().setLayout(new BorderLayout());
        form.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        getContentPane().add(top, BorderLayout.NORTH);
        getContentPane().add(form, BorderLayout.CENTER);
        getContentPane().add(buttons, BorderLayout.SOUTH);
    }

    /**
     * Queries all bookings ordered by booking_id and positions the cursor at
     * targetRow.
     *
     * @param targetRow 1-based row index to display (clamped to the valid row
     * range)
     */
    private void loadData(int targetRow) {
        try {
            rs = dbCon.executeStatement("SELECT booking_id, passport_no, booking_date, amount_paid "
                    + "FROM booking ORDER BY booking_id ASC");
            if (rs.last()) {
                int total = rs.getRow();
                rs.absolute(Math.max(1, Math.min(targetRow, total)));
                populateFields();
            } else {
                clearFields();
            }
        } catch (SQLException e) {
            Util.sqlError(this, "loading bookings.", e);
        }
        enableDisableButtons();
    }

    /**
     * Clears all input fields and combo box items when the BOOKING table is
     * empty.
     */
    private void clearFields() {
        txtBookingId.setText("");
        cmbPassenger.removeAllItems();
        txtBookingDate.setText("");
        txtAmountPaid.setText("");
    }

    /**
     * Populates the form fields and selects the matching passenger in the
     * dropdown for the current row.
     */
    private void populateFields() throws SQLException {
        lblDateError.setVisible(false);
        lblAmountError.setVisible(false);

        txtBookingId.setText(rs.getString("booking_id"));
        Date d = rs.getDate("booking_date");
        txtBookingDate.setText(d == null ? "" : d.toLocalDate().toString());
        txtAmountPaid.setText(rs.getBigDecimal("amount_paid").toPlainString());

        Util.fillCombo(dbCon, cmbPassenger, passportNos,
                Util.PASSENGER_NAMES_SQL + "ORDER BY first_name, last_name", null);
        int i = passportNos.indexOf(rs.getString("passport_no"));
        cmbPassenger.setSelectedIndex(i < 0 ? 0 : i);
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
     * Advances the result set cursor to the next booking record and refreshes
     * the display.
     */
    private void moveNext() {
        try {
            if (!rs.isLast()) {
                rs.next();
                populateFields();
            }
        } catch (SQLException e) {
            Util.sqlError(this, "moving to next booking.", e);
        }
        enableDisableButtons();
    }

    /**
     * Moves the result set cursor to the previous booking record and refreshes
     * the display.
     */
    private void movePrevious() {
        try {
            if (!rs.isFirst()) {
                rs.previous();
                populateFields();
            }
        } catch (SQLException e) {
            Util.sqlError(this, "moving to previous booking.", e);
        }
        enableDisableButtons();
    }

    /**
     * Validates inputs, asks for confirmation, and updates the current booking
     * in BOOKING.
     */
    private void updateBooking() {
        boolean ok = true;
        lblDateError.setVisible(false);
        lblAmountError.setVisible(false);

        Date bookingDate = Util.parseSqlDate(txtBookingDate.getText());
        if (bookingDate == null) {
            Util.showFieldError(lblDateError, "Enter a valid date (YYYY-MM-DD)");
            ok = false;
        }
        BigDecimal amount = AddBooking.parseAmount(txtAmountPaid.getText());
        if (amount == null) {
            Util.showFieldError(lblAmountError, "Amount must be >= 0 (max 2 decimals)");
            ok = false;
        }
        if (!ok) {
            return;
        }

        int id = Integer.parseInt(txtBookingId.getText());
        if (!Util.confirm(this, "Update booking #" + id + "?", "Confirm Update")) {
            return;
        }

        try {
            String passport = passportNos.get(cmbPassenger.getSelectedIndex());
            Util.update(dbCon, "UPDATE booking SET passport_no = ?, booking_date = ?, amount_paid = ? "
                    + "WHERE booking_id = ?", passport, bookingDate, amount, id);
            Util.info(this, "Booking #" + id + " was updated successfully!");
            loadData(currentRow());
        } catch (SQLException e) {
            Util.sqlError(this, "updating booking.", e);
        }
    }

    /**
     * Deletes the current booking after confirmation, removing any linked rows
     * in FLIGHT_BOOKING first to preserve referential integrity.
     */
    private void deleteBooking() {
        int id = Integer.parseInt(txtBookingId.getText());
        try {
            int flights = Util.count(dbCon, "SELECT COUNT(*) FROM flight_booking WHERE booking_id = ?", id);
            String msg = "Delete booking #" + id + "?";
            if (flights > 0) {
                msg += "\nIts " + flights + " linked flight assignment(s) will also be removed.";
            }
            if (!Util.confirm(this, msg, "Confirm Delete")) {
                return;
            }
            if (flights > 0) {
                Util.update(dbCon, "DELETE FROM flight_booking WHERE booking_id = ?", id);
            }
            Util.update(dbCon, "DELETE FROM booking WHERE booking_id = ?", id);
            Util.info(this, "Booking #" + id + " was deleted successfully.");
            loadData(currentRow());
        } catch (SQLException e) {
            if (e.getErrorCode() == 2292) {
                Util.error(this, "This booking cannot be deleted because other records still use it!");
            } else {
                Util.sqlError(this, "deleting booking.", e);
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
            updateBooking();
        } else if (src == btnDelete) {
            deleteBooking();
        }
    }
}
