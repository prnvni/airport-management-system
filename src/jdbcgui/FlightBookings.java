package jdbcgui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.WindowConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;

/**
 * FlightBookings: manages the many-to-many FLIGHT_BOOKING junction table
 * using dropdowns for both Booking (booking_id) and Flight (flight_number).
 * Lets us select a booking from a dropdown, view all flights attached to that 
 * booking in a table, and ADD, UPDATE SELECTED, OR DELETE selected flights using a
 * Flight dropdown.
 */
public class FlightBookings extends JFrame implements ActionListener, ListSelectionListener {

    myDBCon dbCon;
    ArrayList<String> bookingIds = new ArrayList<String>();
    ArrayList<String> flightNumbers = new ArrayList<String>();
    boolean loading = false;

    JComboBox<String> cmbBooking = new JComboBox<String>();
    JComboBox<String> cmbFlight = new JComboBox<String>();
    DefaultTableModel model = new DefaultTableModel(
            new Object[]{"Flight No", "Route", "Departure", "Status"}, 0) {
        public boolean isCellEditable(int row, int col) {
            return false;
        }
    };
    JTable table = new JTable(model);
    JButton btnAdd = new JButton("Add Flight");
    JButton btnUpdate = new JButton("Update Selected");
    JButton btnDelete = new JButton("Remove Selected");

    /**
     * Builds the form, connects to the database, and loads bookings and
     * flights.
     */
    public FlightBookings() {
        setTitle("Flight Bookings");
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        buildGui();
        dbCon = new myDBCon();
        loadCombos();
        loadBookedFlights();
        pack();
        setResizable(false);
        setLocationRelativeTo(null);
    }

    /**
     * Creates and lays out the combo boxes, flight table, and action buttons.
     */
    private void buildGui() {
        cmbBooking.addActionListener(this);
        table.setFont(Util.FIELD_FONT);
        table.setRowHeight(24);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getSelectionModel().addListSelectionListener(this);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setPreferredSize(new java.awt.Dimension(620, 190));

        JPanel form = Util.makeFormPanel();
        Util.addRow(form, 0, "Booking:", cmbBooking, null);
        Util.addRow(form, 1, "Flight:", cmbFlight, null);

        JPanel center = new JPanel(new BorderLayout());
        center.add(form, BorderLayout.NORTH);
        center.add(scroll, BorderLayout.CENTER);
        center.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

        JPanel buttons = new JPanel(new FlowLayout());
        JButton[] all = {btnAdd, btnUpdate, btnDelete};
        for (int i = 0; i < all.length; i++) {
            all[i].setFont(Util.LABEL_FONT);
            all[i].addActionListener(this);
            buttons.add(all[i]);
        }

        JPanel top = new JPanel(new FlowLayout());
        top.add(Util.makeTitle("Flight Bookings"));

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(top, BorderLayout.NORTH);
        getContentPane().add(center, BorderLayout.CENTER);
        getContentPane().add(buttons, BorderLayout.SOUTH);
    }

    /**
     * Populates both the Booking and Flight combo boxes from the database.
     */
    private void loadCombos() {
        loading = true;
        try {
            Util.fillCombo(dbCon, cmbBooking, bookingIds,
                    "SELECT b.booking_id, 'Booking #' || b.booking_id || ' - ' || p.first_name || ' ' || p.last_name "
                    + "|| ' (' || b.passport_no || ')' "
                    + "FROM booking b JOIN passenger p ON p.passport_no = b.passport_no "
                    + "ORDER BY b.booking_id", null);
            Util.fillCombo(dbCon, cmbFlight, flightNumbers,
                    "SELECT flight_number, 'Flight ' || flight_number || ': ' || dept_airport || ' -> ' "
                    + "|| arr_airport || ' (' || status || ')' "
                    + "FROM flight ORDER BY flight_number", null);
        } catch (SQLException e) {
            Util.sqlError(this, "loading bookings / flights.", e);
        }
        loading = false;
    }

    /**
     * Returns the booking_id currently selected in the Booking combo box, or -1
     * if none.
     */
    private int selectedBookingId() {
        int i = cmbBooking.getSelectedIndex();
        return i < 0 ? -1 : Integer.parseInt(bookingIds.get(i));
    }

    /**
     * Returns the flight_number currently selected in the Flight combo box, or
     * -1 if none.
     */
    private int selectedDropdownFlight() {
        int i = cmbFlight.getSelectedIndex();
        return i < 0 ? -1 : Integer.parseInt(flightNumbers.get(i));
    }

    /**
     * Queries and displays all flights linked to the currently selected
     * booking.
     */
    private void loadBookedFlights() {
        model.setRowCount(0);
        int bookingId = selectedBookingId();
        if (bookingId < 0) {
            return;
        }
        try {
            PreparedStatement ps = dbCon.getConnection().prepareStatement(
                    "SELECT f.flight_number, f.dept_airport || ' -> ' || f.arr_airport, "
                    + "TO_CHAR(f.dept_time, 'YYYY-MM-DD HH24:MI'), f.status "
                    + "FROM flight_booking fb JOIN flight f ON f.flight_number = fb.flight_number "
                    + "WHERE fb.booking_id = ? ORDER BY f.flight_number");
            ps.setInt(1, bookingId);
            ResultSet r = ps.executeQuery();
            while (r.next()) {
                model.addRow(new Object[]{r.getInt(1), r.getString(2), r.getString(3), r.getString(4)});
            }
            r.close();
            ps.close();
        } catch (SQLException e) {
            Util.sqlError(this, "loading booked flights.", e);
        }
    }

    /**
     * Returns the flight_number of the row selected in the table, or null if no
     * row is selected.
     */
    private Integer selectedTableFlight() {
        int row = table.getSelectedRow();
        if (row < 0) {
            Util.error(this, "Select a flight row from the table first!");
            return null;
        }
        return (Integer) model.getValueAt(row, 0);
    }

    /**
     * Inserts a new (flight_number, booking_id) link if it does not already
     * exist.
     */
    private void addFlightToBooking() {
        int bookingId = selectedBookingId();
        int flightNo = selectedDropdownFlight();
        if (bookingId < 0 || flightNo < 0) {
            return;
        }
        try {
            if (Util.count(dbCon, "SELECT COUNT(*) FROM flight_booking "
                    + "WHERE booking_id = ? AND flight_number = ?", bookingId, flightNo) > 0) {
                Util.error(this, "Flight " + flightNo + " is already included in Booking #" + bookingId + ".");
                return;
            }
            Util.update(dbCon, "INSERT INTO flight_booking (flight_number, booking_id) VALUES (?, ?)",
                    flightNo, bookingId);
            Util.info(this, "Flight " + flightNo + " was added to Booking #" + bookingId + ".");
            loadBookedFlights();
        } catch (SQLException e) {
            Util.sqlError(this, "adding flight to booking.", e);
        }
    }

    /**
     * Replaces the selected flight in the current booking with the flight
     * chosen in the dropdown.
     */
    private void updateFlightInBooking() {
        int bookingId = selectedBookingId();
        Integer oldFlight = selectedTableFlight();
        int newFlight = selectedDropdownFlight();
        if (bookingId < 0 || oldFlight == null || newFlight < 0) {
            return;
        }
        try {
            if (Util.count(dbCon, "SELECT COUNT(*) FROM flight_booking "
                    + "WHERE booking_id = ? AND flight_number = ? AND flight_number <> ?",
                    bookingId, newFlight, oldFlight) > 0) {
                Util.error(this, "Flight " + newFlight + " is already included in Booking #" + bookingId + ".");
                return;
            }
            if (!Util.confirm(this, "Change Flight " + oldFlight + " to Flight " + newFlight + "?", "Confirm Update")) {
                return;
            }
            Util.update(dbCon, "UPDATE flight_booking SET flight_number = ? "
                    + "WHERE booking_id = ? AND flight_number = ?", newFlight, bookingId, oldFlight);
            Util.info(this, "Flight booking updated successfully!");
            loadBookedFlights();
        } catch (SQLException e) {
            Util.sqlError(this, "updating flight booking.", e);
        }
    }

    /**
     * Deletes the selected flight link from the current booking after
     * confirmation.
     */
    private void removeFlightFromBooking() {
        int bookingId = selectedBookingId();
        Integer oldFlight = selectedTableFlight();
        if (bookingId < 0 || oldFlight == null) {
            return;
        }
        if (!Util.confirm(this, "Remove Flight " + oldFlight + " from Booking #" + bookingId + "?", "Confirm Delete")) {
            return;
        }
        try {
            Util.update(dbCon, "DELETE FROM flight_booking WHERE booking_id = ? AND flight_number = ?",
                    bookingId, oldFlight);
            Util.info(this, "Flight removed from booking.");
            loadBookedFlights();
        } catch (SQLException e) {
            Util.sqlError(this, "removing flight from booking.", e);
        }
    }

    /**
     * Handles Booking combo box selection changes and Add/Update/Remove button
     * clicks.
     */
    public void actionPerformed(ActionEvent e) {
        Object src = e.getSource();
        if (src == cmbBooking) {
            if (!loading) {
                loadBookedFlights();
            }
        } else if (src == btnAdd) {
            addFlightToBooking();
        } else if (src == btnUpdate) {
            updateFlightInBooking();
        } else if (src == btnDelete) {
            removeFlightFromBooking();
        }
    }

    /**
     * Synchronizes the Flight dropdown when the user clicks a row in the table.
     */
    public void valueChanged(ListSelectionEvent e) {
        if (!e.getValueIsAdjusting() && table.getSelectedRow() >= 0) {
            String flightNo = String.valueOf(model.getValueAt(table.getSelectedRow(), 0));
            int idx = flightNumbers.indexOf(flightNo);
            if (idx >= 0) {
                cmbFlight.setSelectedIndex(idx);
            }
        }
    }
}