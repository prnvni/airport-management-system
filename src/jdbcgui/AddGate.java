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
 * AddGate: form used to insert a new row into the GATE table.
 * The airport combo box shows airport names (not codes); the matching
 * airport_code values are kept in the airportCodes list in the same order.
 * The primary key is (airport_code, gate_no), so the same gate number can
 * exist at different airports but only once at the same airport.
 */
public class AddGate extends JFrame implements ActionListener {

    myDBCon dbCon;
    ArrayList<String> airportCodes = new ArrayList<String>(); // airport_code of each name in the combo box

    JComboBox<String> cmbAirport = new JComboBox<String>();
    JTextField txtGateNo = new JTextField(20);
    JTextField txtTerminal = new JTextField(20);
    JLabel lblGateNoError = Util.makeErrorLabel();
    JLabel lblTerminalError = Util.makeErrorLabel();
    JButton btnAdd = new JButton("Add");
    JButton btnClear = new JButton("Clear");

    /** Builds the form, connects to the database and loads the airport names. */
    public AddGate() {
        setTitle("Add New Gate");
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        buildGui();
        dbCon = new myDBCon();
        loadAirports();
        pack();
        setResizable(false);
        setLocationRelativeTo(null); // center form in screen
    }

    /** Creates and lays out all the components of the form. */
    private void buildGui() {
        JPanel form = Util.makeFormPanel();
        Util.addRow(form, 0, "Airport:", cmbAirport, null);
        Util.addRow(form, 1, "Gate number (1-4 letters/digits):", txtGateNo, lblGateNoError);
        Util.addRow(form, 2, "Terminal (number):", txtTerminal, lblTerminalError);

        JPanel buttons = new JPanel(new FlowLayout());
        btnAdd.setFont(Util.LABEL_FONT);
        btnClear.setFont(Util.LABEL_FONT);
        btnAdd.addActionListener(this);
        btnClear.addActionListener(this);
        buttons.add(btnAdd);
        buttons.add(btnClear);

        JPanel top = new JPanel(new FlowLayout());
        top.add(Util.makeTitle("Add New Gate"));

        getContentPane().setLayout(new BorderLayout());
        form.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        getContentPane().add(top, BorderLayout.NORTH);
        getContentPane().add(form, BorderLayout.CENTER);
        getContentPane().add(buttons, BorderLayout.SOUTH);
    }

    /** Fills the airport combo box with airport names. */
    private void loadAirports() {
        try {
            Util.fillCombo(dbCon, cmbAirport, airportCodes,
                    "SELECT airport_code, airport_name || ' (' || airport_code || ')' "
                    + "FROM airport ORDER BY airport_name", null);
        } catch (SQLException e) {
            Util.sqlError(this, "loading airports.", e);
        }
    }

    /** Hides all the error labels. */
    private void clearErrorLabels() {
        lblGateNoError.setVisible(false);
        lblTerminalError.setVisible(false);
    }

    /** Clears all the input fields and the error labels. */
    private void clearFields() {
        txtGateNo.setText("");
        txtTerminal.setText("");
        if (cmbAirport.getItemCount() > 0) {
            cmbAirport.setSelectedIndex(0);
        }
        clearErrorLabels();
        pack();
    }

    /**
     * Checks all the fields. Shows an error label for every invalid field
     * and returns true only if everything is valid.
     */
    private boolean validateInput(String gateNo, String terminal) {
        boolean ok = true;
        clearErrorLabels();

        if (!gateNo.matches("[A-Za-z0-9]{1,4}")) {
            Util.showFieldError(lblGateNoError, "Use 1 to 4 letters or digits");
            ok = false;
        }
        if (!Util.isWholeNumber(terminal) || Integer.parseInt(terminal) < 1 || Integer.parseInt(terminal) > 999) {
            Util.showFieldError(lblTerminalError, "Enter a number from 1 to 999");
            ok = false;
        }
        pack(); // resize the window so the error labels fit
        return ok;
    }

    /** Validates the input, checks for duplicates and inserts the gate. */
    private void addGate() {
        String gateNo = txtGateNo.getText().trim().toUpperCase();
        String terminal = txtTerminal.getText().trim();

        if (cmbAirport.getSelectedIndex() < 0) {
            Util.error(this, "Please select an airport.");
            return;
        }
        if (!validateInput(gateNo, terminal)) {
            return;
        }
        String code = airportCodes.get(cmbAirport.getSelectedIndex());
        try {
            // the primary key is (airport_code, gate_no): check it first
            if (Util.count(dbCon, "SELECT COUNT(*) FROM gate WHERE airport_code = ? AND gate_no = ?",
                    code, gateNo) > 0) {
                Util.showFieldError(lblGateNoError, "This airport already has gate " + gateNo);
                pack();
                return;
            }
            Util.update(dbCon, "INSERT INTO gate (airport_code, gate_no, terminal) VALUES (?, ?, ?)",
                    code, gateNo, Integer.valueOf(terminal));
            Util.info(this, "Gate " + gateNo + " at " + code + " was added successfully.");
            clearFields();
        } catch (SQLException e) {
            Util.sqlError(this, "adding gate.", e);
        }
    }

    /** Handles the Add and Clear buttons. */
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == btnAdd) {
            addGate();
        } else if (e.getSource() == btnClear) {
            clearFields();
        }
    }
}