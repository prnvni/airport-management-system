package jdbcgui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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
 * AddAircraft: form used to insert a new row into the AIRCRAFT table.
 * The airline combo box shows airline names (not codes); the matching
 * airline codes are kept in the airlineCodes list in the same order.
 * The manufacture year is optional.
 */
public class AddAircraft extends JFrame implements ActionListener {

    myDBCon dbCon;
    ArrayList<String> airlineCodes = new ArrayList<String>(); // airline_code of each name in the combo box

    JTextField txtId = new JTextField(20);
    JTextField txtModel = new JTextField(20);
    JTextField txtCapacity = new JTextField(20);
    JTextField txtYear = new JTextField(20);
    JComboBox<String> cmbAirline = new JComboBox<String>();
    JLabel lblIdError = Util.makeErrorLabel();
    JLabel lblModelError = Util.makeErrorLabel();
    JLabel lblCapacityError = Util.makeErrorLabel();
    JLabel lblYearError = Util.makeErrorLabel();
    JLabel lblAirlineError = Util.makeErrorLabel();
    JButton btnAdd = new JButton("Add");
    JButton btnClear = new JButton("Clear");

    /** Builds the form, connects to the database and loads the airline names. */
    public AddAircraft() {
        setTitle("Add New Aircraft");
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        buildGui();
        dbCon = new myDBCon();
        loadAirlines();
        pack();
        setResizable(false);
        setLocationRelativeTo(null); // center form in screen
    }

    /** Creates and lays out all the components of the form. */
    private void buildGui() {
        JPanel form = Util.makeFormPanel();
        Util.addRow(form, 0, "Aircraft ID (1-999999):", txtId, lblIdError);
        Util.addRow(form, 1, "Model:", txtModel, lblModelError);
        Util.addRow(form, 2, "Capacity (seats):", txtCapacity, lblCapacityError);
        Util.addRow(form, 3, "Manufacture year (optional):", txtYear, lblYearError);
        Util.addRow(form, 4, "Airline:", cmbAirline, lblAirlineError);

        JPanel buttons = new JPanel(new FlowLayout());
        btnAdd.setFont(Util.LABEL_FONT);
        btnClear.setFont(Util.LABEL_FONT);
        btnAdd.addActionListener(this);
        btnClear.addActionListener(this);
        buttons.add(btnAdd);
        buttons.add(btnClear);

        JPanel top = new JPanel(new FlowLayout());
        top.add(Util.makeTitle("Add New Aircraft"));

        getContentPane().setLayout(new BorderLayout());
        form.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        getContentPane().add(top, BorderLayout.NORTH);
        getContentPane().add(form, BorderLayout.CENTER);
        getContentPane().add(buttons, BorderLayout.SOUTH);
    }

    /** Fills the airline combo box with airline names. */
    private void loadAirlines() {
        try {
            Util.fillCombo(dbCon, cmbAirline, airlineCodes,
                    "SELECT airline_code, airline_name || ' (' || airline_code || ')' "
                    + "FROM airline ORDER BY airline_name", null);
        } catch (SQLException e) {
            Util.sqlError(this, "loading airlines.", e);
        }
    }

    /** Hides all the error labels. */
    private void clearErrorLabels() {
        lblIdError.setVisible(false);
        lblModelError.setVisible(false);
        lblCapacityError.setVisible(false);
        lblYearError.setVisible(false);
        lblAirlineError.setVisible(false);
    }

    /** Clears all the input fields and the error labels. */
    private void clearFields() {
        txtId.setText("");
        txtModel.setText("");
        txtCapacity.setText("");
        txtYear.setText("");
        if (cmbAirline.getItemCount() > 0) {
            cmbAirline.setSelectedIndex(0);
        }
        clearErrorLabels();
    }

    /**
     * Checks all the fields. Shows an error label for every invalid field
     * and returns true only if everything is valid.
     */
    private boolean validateInput(String id, String model, String capacity, String year) {
        boolean ok = true;
        clearErrorLabels();

        if (!id.matches("[0-9]{1,6}") || Integer.parseInt(id) == 0) {
            Util.showFieldError(lblIdError, "ID must be a whole number from 1 to 999999");
            ok = false;
        }
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
        if (cmbAirline.getSelectedIndex() < 0) {
            Util.showFieldError(lblAirlineError, "Add an airline first");
            ok = false;
        }
        return ok;
    }

    /** Validates the input, checks for a duplicate ID and inserts the aircraft. */
    private void addAircraft() {
        String id = txtId.getText().trim();
        String model = txtModel.getText().trim();
        String capacity = txtCapacity.getText().trim();
        String year = txtYear.getText().trim();

        if (!validateInput(id, model, capacity, year)) {
            return;
        }
        try {
            if (Util.count(dbCon, "SELECT COUNT(*) FROM aircraft WHERE aircraft_id = ?", Integer.valueOf(id)) > 0) {
                Util.showFieldError(lblIdError, "This aircraft ID already exists");
                return;
            }
            String airline = airlineCodes.get(cmbAirline.getSelectedIndex());
            Util.update(dbCon, "INSERT INTO aircraft (aircraft_id, model, capacity, manufacture_year, airline_code) "
                    + "VALUES (?, ?, ?, ?, ?)",
                    Integer.valueOf(id), model, Integer.valueOf(capacity), Util.toInt(year), airline);
            Util.info(this, "Aircraft " + id + " was added successfully.");
            clearFields();
        } catch (SQLException e) {
            Util.sqlError(this, "adding aircraft.", e);
        }
    }

    /** Handles the Add and Clear buttons. */
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == btnAdd) {
            addAircraft();
        } else if (e.getSource() == btnClear) {
            clearFields();
        }
    }
}
