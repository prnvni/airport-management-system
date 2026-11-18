package jdbcgui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.SQLException;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.WindowConstants;

/**
 * AddAirline: form used to insert a new row into the AIRLINE table.
 * The airline code is the primary key and the airline name is UNIQUE, so
 * both are checked for duplicates before the insert.
 */
public class AddAirline extends JFrame implements ActionListener {

    myDBCon dbCon;

    JTextField txtCode = new JTextField(20);
    JTextField txtName = new JTextField(20);
    JTextField txtCountry = new JTextField(20);
    JLabel lblCodeError = Util.makeErrorLabel();
    JLabel lblNameError = Util.makeErrorLabel();
    JLabel lblCountryError = Util.makeErrorLabel();
    JButton btnAdd = new JButton("Add");
    JButton btnClear = new JButton("Clear");

    /** Builds the form and connects to the database. */
    public AddAirline() {
        setTitle("Add New Airline");
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        buildGui();
        dbCon = new myDBCon();
        pack();
        setResizable(false);
        setLocationRelativeTo(null); // center form in screen
    }

    /** Creates and lays out all the components of the form. */
    private void buildGui() {
        JPanel form = Util.makeFormPanel();
        Util.addRow(form, 0, "Airline code (2-3 characters):", txtCode, lblCodeError);
        Util.addRow(form, 1, "Airline name:", txtName, lblNameError);
        Util.addRow(form, 2, "Country:", txtCountry, lblCountryError);

        JPanel buttons = new JPanel(new FlowLayout());
        btnAdd.setFont(Util.LABEL_FONT);
        btnClear.setFont(Util.LABEL_FONT);
        btnAdd.addActionListener(this);
        btnClear.addActionListener(this);
        buttons.add(btnAdd);
        buttons.add(btnClear);

        JPanel top = new JPanel(new FlowLayout());
        top.add(Util.makeTitle("Add New Airline"));

        getContentPane().setLayout(new BorderLayout());
        form.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        getContentPane().add(top, BorderLayout.NORTH);
        getContentPane().add(form, BorderLayout.CENTER);
        getContentPane().add(buttons, BorderLayout.SOUTH);
    }

    /** Hides all the error labels. */
    private void clearErrorLabels() {
        lblCodeError.setVisible(false);
        lblNameError.setVisible(false);
        lblCountryError.setVisible(false);
    }

    /** Clears all the input fields and the error labels. */
    private void clearFields() {
        txtCode.setText("");
        txtName.setText("");
        txtCountry.setText("");
        clearErrorLabels();
    }
    /** True if the text is 1 to 60 characters and contains at least one letter. */
    static boolean isNameText(String s) {
        return s.length() >= 1 && s.length() <= 60 && s.matches(".*\\p{L}.*");
    }
        /** Checks all the fields; shows an error label for every invalid field. */
    private boolean validateInput(String code, String name, String country) {
        boolean ok = true;
        clearErrorLabels();

        if (!code.matches("[A-Za-z0-9]{2,3}")) {
            Util.showFieldError(lblCodeError, "Code must be 2 or 3 letters/digits");
            ok = false;
        }
        if (!isNameText(name)) {
            Util.showFieldError(lblNameError, "Name must be text with at least one letter (1 to 60 characters)");
            ok = false;
        }
        if (country.length() < 1 || country.length() > 40 || !country.matches("[\\p{L}][\\p{L} .'-]*")) {
            Util.showFieldError(lblCountryError, "Country must be 1 to 40 letters");
            ok = false;
        }
        return ok;
    }

    /** Validates the input, checks for duplicates and inserts the airline. */
    private void addAirline() {
        String code = txtCode.getText().trim().toUpperCase();
        String name = txtName.getText().trim();
        String country = txtCountry.getText().trim();

        if (!validateInput(code, name, country)) {
            return;
        }
        try {
            if (Util.count(dbCon, "SELECT COUNT(*) FROM airline WHERE airline_code = ?", code) > 0) {
                Util.showFieldError(lblCodeError, "This airline code already exists");
                return;
            }
            if (Util.count(dbCon, "SELECT COUNT(*) FROM airline WHERE UPPER(airline_name) = UPPER(?)", name) > 0) {
                Util.showFieldError(lblNameError, "This airline name already exists");
                return;
            }
            Util.update(dbCon, "INSERT INTO airline (airline_code, airline_name, airline_country) "
                    + "VALUES (?, ?, ?)", code, name, country);
            Util.info(this, "Airline " + code + " was added successfully.");
            clearFields();
        } catch (SQLException e) {
            Util.sqlError(this, "adding airline.", e);
        }
    }

    /** Handles the Add and Clear buttons. */
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == btnAdd) {
            addAirline();
        } else if (e.getSource() == btnClear) {
            clearFields();
        }
    }
}
