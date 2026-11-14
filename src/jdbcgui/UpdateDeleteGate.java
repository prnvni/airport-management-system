package jdbcgui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.WindowConstants;

/**
 * UpdateDeleteGate: form used to browse (Next / Previous), update and
 * delete rows of the GATE table. The airport and the gate number together
 * are the primary key so they cannot be edited; only the terminal can be
 * changed. A gate that still has gate assignments is not deleted.
 * Both update and delete ask the user to confirm first.
 */
public class UpdateDeleteGate extends JFrame implements ActionListener {

    myDBCon dbCon;
    ResultSet rs; // all gates, scrollable

    JTextField txtCode = new JTextField(20);
    JTextField txtAirport = new JTextField(20);
    JTextField txtGateNo = new JTextField(20);
    JTextField txtTerminal = new JTextField(20);
    JLabel lblTerminalError = Util.makeErrorLabel();
    JButton btnPrevious = new JButton("< Previous");
    JButton btnNext = new JButton("Next >");
    JButton btnUpdate = new JButton("Update");
    JButton btnDelete = new JButton("Delete");

    /** Builds the form, connects to the database and shows the first gate. */
    public UpdateDeleteGate() {
        setTitle("Update/Delete Gate");
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        buildGui();
        dbCon = new myDBCon();
        loadData(1);
        pack();
        setResizable(false);
        setLocationRelativeTo(null); // center form in screen
    }

    /** Creates and lays out all the components of the form. */
    private void buildGui() {
        txtCode.setEditable(false);    // primary key: cannot be edited
        txtAirport.setEditable(false); // only shown for information
        txtGateNo.setEditable(false);  // primary key: cannot be edited

        JPanel form = Util.makeFormPanel();
        Util.addRow(form, 0, "Airport code:", txtCode, null);
        Util.addRow(form, 1, "Airport:", txtAirport, null);
        Util.addRow(form, 2, "Gate number:", txtGateNo, null);
        Util.addRow(form, 3, "Terminal (number):", txtTerminal, lblTerminalError);

        JPanel buttons = new JPanel(new FlowLayout());
        JButton[] all = {btnPrevious, btnNext, btnUpdate, btnDelete};
        for (int i = 0; i < all.length; i++) {
            all[i].setFont(Util.LABEL_FONT);
            all[i].addActionListener(this);
            buttons.add(all[i]);
        }

        JPanel top = new JPanel(new FlowLayout());
        top.add(Util.makeTitle("Update/Delete Gate"));

        getContentPane().setLayout(new BorderLayout());
        form.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        getContentPane().add(top, BorderLayout.NORTH);
        getContentPane().add(form, BorderLayout.CENTER);
        getContentPane().add(buttons, BorderLayout.SOUTH);
    }

    /**
     * Reads all gates from the database and shows the gate at the
     * given row number (or the last row if there are fewer rows).
     */
    private void loadData(int targetRow) {
        try {
            rs = dbCon.executeStatement("SELECT g.airport_code, a.airport_name, g.gate_no, g.terminal "
                    + "FROM gate g, airport a WHERE g.airport_code = a.airport_code "
                    + "ORDER BY g.airport_code ASC, g.gate_no ASC");
            if (rs.last()) {
                int total = rs.getRow();
                rs.absolute(Math.max(1, Math.min(targetRow, total)));
                populateFields();
            } else {
                clearFields(); // table is empty
            }
        } catch (SQLException e) {
            Util.sqlError(this, "loading gates.", e);
        }
        enableDisableButtons();
    }

    /** Clears the fields when there is no gate to show. */
    private void clearFields() {
        txtCode.setText("");
        txtAirport.setText("");
        txtGateNo.setText("");
        txtTerminal.setText("");
        lblTerminalError.setVisible(false);
    }

    /** Shows the current row in the fields. */
    private void populateFields() throws SQLException {
        lblTerminalError.setVisible(false);
        txtCode.setText(rs.getString("airport_code"));
        txtAirport.setText(rs.getString("airport_name"));
        txtGateNo.setText(rs.getString("gate_no"));
        txtTerminal.setText(rs.getString("terminal"));
        pack(); // shrink the window back after the error label was shown
    }

    /** Enables / disables Next, Previous, Update and Delete depending on the current row. */
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

    /** Moves to the next gate. */
    private void moveNext() {
        try {
            if (!rs.isLast()) {
                rs.next();
                populateFields();
            }
        } catch (SQLException e) {
            Util.sqlError(this, "moving to next gate.", e);
        }
        enableDisableButtons();
    }

    /** Moves to the previous gate. */
    private void movePrevious() {
        try {
            if (!rs.isFirst()) {
                rs.previous();
                populateFields();
            }
        } catch (SQLException e) {
            Util.sqlError(this, "moving to previous gate.", e);
        }
        enableDisableButtons();
    }

    /** Validates the terminal; shows the error label and returns true if it is valid. */
    private boolean validateInput(String terminal) {
        boolean ok = true;
        lblTerminalError.setVisible(false);
        if (!Util.isWholeNumber(terminal) || Integer.parseInt(terminal) < 1 || Integer.parseInt(terminal) > 999) {
            Util.showFieldError(lblTerminalError, "Enter a number from 1 to 999");
            ok = false;
        }
        pack(); // resize the window so the error label fits
        return ok;
    }

    /** Validates, asks for confirmation and updates the terminal of the shown gate. */
    private void updateGate() {
        String code = txtCode.getText();
        String gateNo = txtGateNo.getText();
        String terminal = txtTerminal.getText().trim();

        if (!validateInput(terminal)) {
            return;
        }
        try {
            if (!Util.confirm(this, "Update gate " + gateNo + " at " + code + "?", "Confirm Update")) {
                return; // user clicked No: nothing changes
            }
            Util.update(dbCon, "UPDATE gate SET terminal = ? WHERE airport_code = ? AND gate_no = ?",
                    Integer.valueOf(terminal), code, gateNo);
            Util.info(this, "Gate " + gateNo + " at " + code + " was updated successfully.");
            loadData(currentRow());
        } catch (SQLException e) {
            Util.sqlError(this, "updating gate.", e);
        }
    }

    /**
     * Asks for confirmation and deletes the shown gate. Oracle refuses
     * (ORA-02292) if the gate still has gate assignments.
     */
    private void deleteGate() {
        String code = txtCode.getText();
        String gateNo = txtGateNo.getText();
        try {
            if (!Util.confirm(this, "Delete gate " + gateNo + " at " + code + "?", "Confirm Delete")) {
                return; // user clicked No: nothing changes
            }
            Util.update(dbCon, "DELETE FROM gate WHERE airport_code = ? AND gate_no = ?", code, gateNo);
            Util.info(this, "Gate " + gateNo + " at " + code + " was deleted successfully.");
            loadData(currentRow());
        } catch (SQLException e) {
            if (e.getErrorCode() == 2292) { // ORA-02292: child records found
                Util.error(this, "Gate " + gateNo + " at " + code
                        + " cannot be deleted because it has gate assignments. Delete those first.");
            } else {
                Util.sqlError(this, "deleting gate.", e);
            }
        }
    }

    /** Returns the current row number of the result set (1 if none). */
    private int currentRow() {
        try {
            return Math.max(1, rs.getRow());
        } catch (SQLException e) {
            return 1;
        }
    }

    /** Handles the Next, Previous, Update and Delete buttons. */
    public void actionPerformed(ActionEvent e) {
        Object src = e.getSource();
        if (src == btnNext) {
            moveNext();
        } else if (src == btnPrevious) {
            movePrevious();
        } else if (src == btnUpdate) {
            updateGate();
        } else if (src == btnDelete) {
            deleteGate();
        }
    }
}