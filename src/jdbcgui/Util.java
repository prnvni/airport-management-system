package jdbcgui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

/**
 * Util: shared helper functions used by the airport and staff forms
 * (AddAirport, UpdateDeleteAirport, AddStaff, UpdateDeleteStaff,
 * StaffQualifications). It holds the common fonts, the form layout helper,
 * the message / confirm dialogs and small database helpers (count, update,
 * fill combo box). All database access still goes through myDBCon.
 */
public class Util {

    static final Font LABEL_FONT = new Font("Tahoma", Font.BOLD, 16);
    static final Font FIELD_FONT = new Font("Tahoma", Font.PLAIN, 16);
    static final Font TITLE_FONT = new Font("Tahoma", Font.BOLD, 30);

    /** Creates a bold label for a form field. */
    static JLabel makeLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(LABEL_FONT);
        return l;
    }

    /** Creates the big title label shown at the top of a form. */
    static JLabel makeTitle(String text) {
        JLabel l = new JLabel(text);
        l.setFont(TITLE_FONT);
        return l;
    }

    /** Creates a red error label that is hidden until a validation fails. */
    static JLabel makeErrorLabel() {
        JLabel l = new JLabel(" ");
        l.setForeground(Color.RED);
        l.setFont(new Font("Tahoma", Font.PLAIN, 13));
        l.setVisible(false);
        return l;
    }

    /** Shows an error message under a field. */
    static void showFieldError(JLabel lbl, String msg) {
        lbl.setText(msg);
        lbl.setVisible(true);
    }

    /** Adds one row (label, field, error label) to a GridBagLayout panel. */
    static void addRow(JPanel p, int row, String label, JComponent field, JLabel err) {
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 8, 0, 8);
        c.gridy = row;
        c.anchor = GridBagConstraints.WEST;

        c.gridx = 0;
        p.add(makeLabel(label), c);

        c.gridx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        field.setFont(FIELD_FONT);
        p.add(field, c);

        if (err != null) {
            c.gridx = 2;
            c.fill = GridBagConstraints.NONE;
            c.weightx = 0;
            p.add(err, c);
        }
    }

    /** Creates an empty panel that uses a GridBagLayout. */
    static JPanel makeFormPanel() {
        JPanel p = new JPanel();
        p.setLayout(new GridBagLayout());
        return p;
    }

    /** Shows a Yes/No confirmation window; returns true only if user clicks Yes. */
    static boolean confirm(Component parent, String message, String title) {
        int ans = JOptionPane.showConfirmDialog(parent, message, title,
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        return ans == JOptionPane.YES_OPTION;
    }

    /** Shows an information message. */
    static void info(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "Information", JOptionPane.INFORMATION_MESSAGE);
    }

    /** Shows an error message. */
    static void error(Component parent, String message) {
        JOptionPane.showMessageDialog(parent, message, "ERROR", JOptionPane.ERROR_MESSAGE);
    }

    /** Shows a database error with the Oracle message. */
    static void sqlError(Component parent, String where, SQLException e) {
        error(parent, "SQL Error - " + where + "\n" + e.getMessage());
    }

    /** Converts a string to Integer; returns null for null or empty text. */
    static Integer toInt(String s) {
        if (s == null || s.trim().isEmpty()) {
            return null;
        }
        return Integer.valueOf(s.trim());
    }

    /** Returns true if the text is a whole number (digits only). */
    static boolean isWholeNumber(String s) {
        return s != null && s.matches("[0-9]{1,9}");
    }

    /** Binds the parameters to a prepared statement (null is sent as SQL NULL). */
    private static void bind(PreparedStatement ps, Object[] params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            if (params[i] == null) {
                ps.setNull(i + 1, Types.VARCHAR);
            } else {
                ps.setObject(i + 1, params[i]);
            }
        }
    }

    /** Runs a SELECT COUNT(*) query with parameters and returns the count. */
    static int count(myDBCon db, String sql, Object... params) throws SQLException {
        PreparedStatement ps = db.getConnection().prepareStatement(sql);
        try {
            bind(ps, params);
            ResultSet r = ps.executeQuery();
            r.next();
            int n = r.getInt(1);
            r.close();
            return n;
        } finally {
            ps.close();
        }
    }

    /** Runs an INSERT / UPDATE / DELETE with parameters; returns rows affected. */
    static int update(myDBCon db, String sql, Object... params) throws SQLException {
        PreparedStatement ps = db.getConnection().prepareStatement(sql);
        try {
            bind(ps, params);
            return ps.executeUpdate();
        } finally {
            ps.close();
        }
    }

    /**
     * Fills a combo box with meaningful names instead of numbers/codes.
     * The query must return two columns: the key (code / id) and the name
     * to display. The names go in the combo box and the keys are kept in the
     * keys list in the same order, so keys.get(cmb.getSelectedIndex()) gives
     * the key of the selected name. If noneLabel is not null, a first item
     * with that text is added whose key is null (meaning "no value").
     */
    static void fillCombo(myDBCon db, JComboBox<String> cmb, ArrayList<String> keys,
            String sql, String noneLabel) throws SQLException {
        cmb.removeAllItems();
        keys.clear();
        if (noneLabel != null) {
            cmb.addItem(noneLabel);
            keys.add(null);
        }
        java.sql.Statement st = db.getConnection().createStatement();
        try {
            ResultSet r = st.executeQuery(sql);
            while (r.next()) {
                keys.add(r.getString(1));
                cmb.addItem(r.getString(2));
            }
            r.close();
        } finally {
            st.close();
        }
    }

    /** SQL for a list of employees: key = emp_id, name = "First Last (ID n)". */
    static final String EMPLOYEE_NAMES_SQL
            = "SELECT emp_id, first_name || ' ' || last_name || ' (ID ' || emp_id || ')' "
            + "FROM a_employee ";
}
