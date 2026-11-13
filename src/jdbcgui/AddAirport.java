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
 * AddAirport: form used to insert a new row into the AIRPORT table.
 * The manager combo box shows employee names (not emp_id); the matching
 * emp_id values are kept in the mgrIds list in the same order.
 * Only employees who do not already manage another airport are listed
 * because AIRPORT.manager_id is UNIQUE.
 */
public class AddAirport extends JFrame implements ActionListener {

    myDBCon dbCon;
    ArrayList<String> mgrIds = new ArrayList<String>(); // emp_id of each name in the combo box

    JTextField txtCode = new JTextField(20);
    JTextField txtName = new JTextField(20);
    JTextField txtCity = new JTextField(20);
    JTextField txtCountry = new JTextField(20);
    JComboBox<String> cmbManager = new JComboBox<String>();
    JLabel lblCodeError = Util.makeErrorLabel();
    JLabel lblNameError = Util.makeErrorLabel();
    JLabel lblCityError = Util.makeErrorLabel();
    JLabel lblCountryError = Util.makeErrorLabel();
    JButton btnAdd = new JButton("Add");
    JButton btnClear = new JButton("Clear");

    /** Builds the form, connects to the database and loads the manager names. */
    public AddAirport() {
        setTitle("Add New Airport");
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        buildGui();
        dbCon = new myDBCon();
        loadManagers();
        pack();
        setResizable(false);
        setLocationRelativeTo(null); // center form in screen
    }

    /** Creates and lays out all the components of the form. */
    private void buildGui() {
        JPanel form = Util.makeFormPanel();
        Util.addRow(form, 0, "Airport code (3 letters):", txtCode, lblCodeError);
        Util.addRow(form, 1, "Airport name:", txtName, lblNameError);
        Util.addRow(form, 2, "City:", txtCity, lblCityError);
        Util.addRow(form, 3, "Country:", txtCountry, lblCountryError);
        Util.addRow(form, 4, "Manager:", cmbManager, null);

        JPanel buttons = new JPanel(new FlowLayout());
        btnAdd.setFont(Util.LABEL_FONT);
        btnClear.setFont(Util.LABEL_FONT);
        btnAdd.addActionListener(this);
        btnClear.addActionListener(this);
        buttons.add(btnAdd);
        buttons.add(btnClear);

        JPanel top = new JPanel(new FlowLayout());
        top.add(Util.makeTitle("Add New Airport"));

        getContentPane().setLayout(new BorderLayout());
        form.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        getContentPane().add(top, BorderLayout.NORTH);
        getContentPane().add(form, BorderLayout.CENTER);
        getContentPane().add(buttons, BorderLayout.SOUTH);
    }

    /** Fills the manager combo box with names of employees who manage no airport yet. */
    private void loadManagers() {
        try {
            Util.fillCombo(dbCon, cmbManager, mgrIds,
                    Util.EMPLOYEE_NAMES_SQL
                    + "WHERE emp_id NOT IN (SELECT manager_id FROM airport WHERE manager_id IS NOT NULL) "
                    + "ORDER BY first_name, last_name",
                    "(No manager)");
        } catch (SQLException e) {
            Util.sqlError(this, "loading managers.", e);
        }
    }

    /** Hides all the error labels. */
    private void clearErrorLabels() {
        lblCodeError.setVisible(false);
        lblNameError.setVisible(false);
        lblCityError.setVisible(false);
        lblCountryError.setVisible(false);
    }

    /** Clears all the input fields and the error labels. */
    private void clearFields() {
        txtCode.setText("");
        txtName.setText("");
        txtCity.setText("");
        txtCountry.setText("");
        if (cmbManager.getItemCount() > 0) {
            cmbManager.setSelectedIndex(0);
        }
        clearErrorLabels();
    }

    /**
     * Checks all the fields. Shows an error label for every invalid field
     * and returns true only if everything is valid.
     */
    private boolean validateInput(String code, String name, String city, String country) {
        boolean ok = true;
        clearErrorLabels();

        if (!code.matches("[A-Za-z]{3}")) {
            Util.showFieldError(lblCodeError, "Code must be exactly 3 letters");
            ok = false;
        }
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

    /** Validates the input, checks for duplicates and inserts the airport. */
    private void addAirport() {
        String code = txtCode.getText().trim().toUpperCase();
        String name = txtName.getText().trim();
        String city = txtCity.getText().trim();
        String country = txtCountry.getText().trim();

        if (!validateInput(code, name, city, country)) {
            return;
        }
        try {
            // the code is the primary key and the name is UNIQUE: check both
            if (Util.count(dbCon, "SELECT COUNT(*) FROM airport WHERE airport_code = ?", code) > 0) {
                Util.showFieldError(lblCodeError, "This airport code already exists");
                return;
            }
            if (Util.count(dbCon, "SELECT COUNT(*) FROM airport WHERE UPPER(airport_name) = UPPER(?)", name) > 0) {
                Util.showFieldError(lblNameError, "This airport name already exists");
                return;
            }
            // number of the selected manager name (null when "No manager")
            Integer mgr = Util.toInt(mgrIds.get(cmbManager.getSelectedIndex()));

            Util.update(dbCon,
                    "INSERT INTO airport (airport_code, airport_name, airport_city, airport_country, manager_id) "
                    + "VALUES (?, ?, ?, ?, ?)", code, name, city, country, mgr);
            Util.info(this, "Airport " + code + " was added successfully.");
            clearFields();
            loadManagers(); // the chosen manager is no longer available
        } catch (SQLException e) {
            Util.sqlError(this, "adding airport.", e);
        }
    }

    /** Handles the Add and Clear buttons. */
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == btnAdd) {
            addAirport();
        } else if (e.getSource() == btnClear) {
            clearFields();
        }
    }
}
