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
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.WindowConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;

/**
 * PassengerPhones: form used to manage the multivalued PASSENGER_PHONE table.
 * Allows selecting a passenger from a dropdown to view, add, update, or delete
 * their associated phone numbers.
 */
public class PassengerPhones extends JFrame implements ActionListener, ListSelectionListener {

    myDBCon dbCon;
    ArrayList<String> passportNos = new ArrayList<String>();
    boolean loading = false;

    JComboBox<String> cmbPassenger = new JComboBox<String>();
    DefaultTableModel model = new DefaultTableModel(new Object[]{"Phone Number"}, 0) {
        public boolean isCellEditable(int row, int col) {
            return false;
        }
    };
    JTable table = new JTable(model);
    JTextField txtPhone = new JTextField(22);
    JLabel lblPhoneError = Util.makeErrorLabel();
    JButton btnAdd = new JButton("Add");
    JButton btnUpdate = new JButton("Update Selected");
    JButton btnDelete = new JButton("Delete Selected");

    /**
     * Builds the form, connects to the database, and loads passengers and their
     * phones.
     */
    public PassengerPhones() {
        setTitle("Passenger Phone Numbers");
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        buildGui();
        dbCon = new myDBCon();
        loadPassengers();
        loadPhones();
        pack();
        setResizable(false);
        setLocationRelativeTo(null);
    }

    /**
     * Creates and lays out the passenger dropdown, phone input, table, and
     * action buttons.
     */
    private void buildGui() {
        cmbPassenger.addActionListener(this);
        table.setFont(Util.FIELD_FONT);
        table.setRowHeight(24);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getSelectionModel().addListSelectionListener(this);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setPreferredSize(new java.awt.Dimension(460, 180));

        JPanel form = Util.makeFormPanel();
        Util.addRow(form, 0, "Passenger:", cmbPassenger, null);
        Util.addRow(form, 1, "Phone Number:", txtPhone, lblPhoneError);

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
        top.add(Util.makeTitle("Passenger Phone Numbers"));

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(top, BorderLayout.NORTH);
        getContentPane().add(center, BorderLayout.CENTER);
        getContentPane().add(buttons, BorderLayout.SOUTH);
    }

    /**
     * Populates the Passenger combo box and stores matching passport numbers.
     */
    private void loadPassengers() {
        loading = true;
        try {
            Util.fillCombo(dbCon, cmbPassenger, passportNos,
                    Util.PASSENGER_NAMES_SQL + "ORDER BY first_name, last_name", null);
        } catch (SQLException e) {
            Util.sqlError(this, "loading passengers.", e);
        }
        loading = false;
    }

    /**
     * Returns the passport_no of the currently selected passenger, or null if
     * none.
     */
    private String selectedPassport() {
        int i = cmbPassenger.getSelectedIndex();
        if (i < 0) {
            return null;
        }
        return passportNos.get(i);
    }

    /**
     * Queries and displays all phone numbers belonging to the selected
     * passenger.
     */
    private void loadPhones() {
        model.setRowCount(0);
        txtPhone.setText("");
        lblPhoneError.setVisible(false);
        String passport = selectedPassport();
        if (passport == null) {
            return;
        }
        try {
            PreparedStatement ps = dbCon.getConnection().prepareStatement(
                    "SELECT phone_number FROM passenger_phone WHERE passport_no = ? ORDER BY phone_number");
            ps.setString(1, passport);
            ResultSet r = ps.executeQuery();
            while (r.next()) {
                model.addRow(new Object[]{r.getString(1)});
            }
            r.close();
            ps.close();
        } catch (SQLException e) {
            Util.sqlError(this, "loading phone numbers.", e);
        }
    }

    /**
     * Validates the phone number text field (5 to 20 characters: digits,
     * spaces, +, -, parentheses).
     *
     * @return the trimmed phone number if valid, or null otherwise
     */
    private String validPhone() {
        lblPhoneError.setVisible(false);
        String p = txtPhone.getText().trim();
        if (!p.matches("[+0-9 ()-]{5,20}")) {
            Util.showFieldError(lblPhoneError, "Phone must be 5 to 20 valid characters (+, digits, spaces, -)");
            return null;
        }
        return p;
    }

    /**
     * Returns the phone number selected in the table, or null if no row is
     * selected.
     */
    private String selectedPhone() {
        int row = table.getSelectedRow();
        if (row < 0) {
            Util.error(this, "Select a phone number from the table first.");
            return null;
        }
        return (String) model.getValueAt(row, 0);
    }

    /**
     * Inserts a new phone number for the selected passenger after checking for
     * duplicates.
     */
    private void addPhone() {
        String passport = selectedPassport();
        String p = validPhone();
        if (passport == null || p == null) {
            return;
        }
        try {
            if (Util.count(dbCon, "SELECT COUNT(*) FROM passenger_phone "
                    + "WHERE passport_no = ? AND phone_number = ?", passport, p) > 0) {
                Util.showFieldError(lblPhoneError, "This passenger already has this phone number");
                return;
            }
            Util.update(dbCon, "INSERT INTO passenger_phone (passport_no, phone_number) VALUES (?, ?)", passport, p);
            Util.info(this, "Phone number was added successfully!");
            loadPhones();
        } catch (SQLException e) {
            Util.sqlError(this, "adding phone number.", e);
        }
    }

    /**
     * Updates the selected phone number in PASSENGER_PHONE after confirmation.
     */
    private void updatePhone() {
        String passport = selectedPassport();
        String old = selectedPhone();
        if (passport == null || old == null) {
            return;
        }
        String p = validPhone();
        if (p == null) {
            return;
        }
        try {
            if (Util.count(dbCon, "SELECT COUNT(*) FROM passenger_phone "
                    + "WHERE passport_no = ? AND phone_number = ? AND phone_number <> ?",
                    passport, p, old) > 0) {
                Util.showFieldError(lblPhoneError, "This passenger already has this phone number.");
                return;
            }
            if (!Util.confirm(this, "Change \"" + old + "\" to \"" + p + "\"?", "Confirm Update")) {
                return;
            }
            Util.update(dbCon, "UPDATE passenger_phone SET phone_number = ? "
                    + "WHERE passport_no = ? AND phone_number = ?", p, passport, old);
            Util.info(this, "Phone number was updated successfully.");
            loadPhones();
        } catch (SQLException e) {
            Util.sqlError(this, "updating phone number.", e);
        }
    }
    
    /**
     * Deletes the selected phone number from PASSENGER_PHONE after
     * confirmation.
     */
    private void deletePhone() {
        String passport = selectedPassport();
        String old = selectedPhone();
        if (passport == null || old == null) {
            return;
        }
        if (!Util.confirm(this, "Delete phone number \"" + old + "\"?", "Confirm Delete")) {
            return;
        }
        try {
            Util.update(dbCon, "DELETE FROM passenger_phone WHERE passport_no = ? AND phone_number = ?", passport, old);
            Util.info(this, "Phone number was deleted successfully.");
            loadPhones();
        } catch (SQLException e) {
            Util.sqlError(this, "deleting phone number.", e);
        }
    }

    /**
     * Handles Passenger combo box changes and Add/Update/Delete button clicks.
     */
    public void actionPerformed(ActionEvent e) {
        Object src = e.getSource();
        if (src == cmbPassenger) {
            if (!loading) {
                loadPhones();
            }
        } else if (src == btnAdd) {
            addPhone();
        } else if (src == btnUpdate) {
            updatePhone();
        } else if (src == btnDelete) {
            deletePhone();
        }
    }

    /**
     * Copies the selected table row's phone number into the text field for
     * editing.
     */
    public void valueChanged(ListSelectionEvent e) {
        if (!e.getValueIsAdjusting() && table.getSelectedRow() >= 0) {
            txtPhone.setText((String) model.getValueAt(table.getSelectedRow(), 0));
            lblPhoneError.setVisible(false);
        }
    }
}