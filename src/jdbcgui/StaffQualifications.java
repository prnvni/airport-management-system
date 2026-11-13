package jdbcgui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
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
 * StaffQualifications: form used to manage the EMPLOYEE_QUALIFICATION table.
 * The user picks an employee by name (the emp_id values are kept in the
 * empIds list in the same order); the table then shows all qualifications of
 * that employee. A qualification can be added, updated (its text is the second
 * part of the primary key) or deleted. Update and delete ask for confirmation.
 */
public class StaffQualifications extends JFrame implements ActionListener, ListSelectionListener {

    myDBCon dbCon;
    ArrayList<String> empIds = new ArrayList<String>(); // emp_id of each name in the combo box
    boolean loading = false; // true while the combo box is being filled

    JComboBox<String> cmbEmployee = new JComboBox<String>();
    DefaultTableModel model = new DefaultTableModel(new Object[]{"Qualification"}, 0) {
        public boolean isCellEditable(int row, int col) {
            return false; // the table is read only
        }
    };
    JTable table = new JTable(model);
    JTextField txtQualification = new JTextField(25);
    JLabel lblQualError = Util.makeErrorLabel();
    JButton btnAdd = new JButton("Add");
    JButton btnUpdate = new JButton("Update Selected");
    JButton btnDelete = new JButton("Delete Selected");

    /** Builds the form, connects to the database and loads the employee names. */
    public StaffQualifications() {
        setTitle("Staff Qualifications");
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        buildGui();
        dbCon = new myDBCon();
        loadEmployees();
        loadQualifications();
        pack();
        setResizable(false);
        setLocationRelativeTo(null); // center form in screen
    }

    /** Creates and lays out all the components of the form. */
    private void buildGui() {
        cmbEmployee.addActionListener(this);
        table.setFont(Util.FIELD_FONT);
        table.setRowHeight(24);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.getSelectionModel().addListSelectionListener(this);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setPreferredSize(new java.awt.Dimension(450, 180));

        JPanel form = Util.makeFormPanel();
        Util.addRow(form, 0, "Employee:", cmbEmployee, null);
        Util.addRow(form, 1, "Qualification:", txtQualification, lblQualError);

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
        top.add(Util.makeTitle("Staff Qualifications"));

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(top, BorderLayout.NORTH);
        getContentPane().add(center, BorderLayout.CENTER);
        getContentPane().add(buttons, BorderLayout.SOUTH);
    }

    /** Fills the employee combo box with employee names. */
    private void loadEmployees() {
        loading = true;
        try {
            Util.fillCombo(dbCon, cmbEmployee, empIds,
                    Util.EMPLOYEE_NAMES_SQL + "ORDER BY first_name, last_name", null);
        } catch (SQLException e) {
            Util.sqlError(this, "loading employees.", e);
        }
        loading = false;
    }

    /** Returns the emp_id of the selected employee, or -1 if none. */
    private int selectedEmpId() {
        int i = cmbEmployee.getSelectedIndex();
        if (i < 0) {
            return -1;
        }
        return Integer.parseInt(empIds.get(i));
    }

    /** Shows all the qualifications of the selected employee in the table. */
    private void loadQualifications() {
        model.setRowCount(0);
        txtQualification.setText("");
        lblQualError.setVisible(false);
        int id = selectedEmpId();
        if (id < 0) {
            return;
        }
        try {
            Statement st = dbCon.getConnection().createStatement();
            ResultSet r = st.executeQuery("SELECT qualification FROM employee_qualification "
                    + "WHERE emp_id = " + id + " ORDER BY qualification");
            while (r.next()) {
                model.addRow(new Object[]{r.getString(1)});
            }
            r.close();
            st.close();
        } catch (SQLException e) {
            Util.sqlError(this, "loading qualifications.", e);
        }
    }

    /** Checks the qualification text (1 to 60 characters); returns it or null if invalid. */
    private String validQualification() {
        lblQualError.setVisible(false);
        String q = txtQualification.getText().trim();
        if (q.length() < 1 || q.length() > 60) {
            Util.showFieldError(lblQualError, "Qualification must be 1 to 60 characters");
            return null;
        }
        return q;
    }

    /** Returns the qualification selected in the table, or null if none is selected. */
    private String selectedQualification() {
        int row = table.getSelectedRow();
        if (row < 0) {
            Util.error(this, "Select a qualification from the table first.");
            return null;
        }
        return (String) model.getValueAt(row, 0);
    }

    /** Adds the typed qualification to the selected employee. */
    private void addQualification() {
        int id = selectedEmpId();
        String q = validQualification();
        if (id < 0 || q == null) {
            return;
        }
        try {
            if (Util.count(dbCon, "SELECT COUNT(*) FROM employee_qualification "
                    + "WHERE emp_id = ? AND UPPER(qualification) = UPPER(?)", id, q) > 0) {
                Util.showFieldError(lblQualError, "This employee already has this qualification");
                return;
            }
            Util.update(dbCon, "INSERT INTO employee_qualification (emp_id, qualification) VALUES (?, ?)", id, q);
            Util.info(this, "Qualification was added successfully.");
            loadQualifications();
        } catch (SQLException e) {
            Util.sqlError(this, "adding qualification.", e);
        }
    }

    /** Changes the text of the selected qualification (after confirmation). */
    private void updateQualification() {
        int id = selectedEmpId();
        String old = selectedQualification();
        if (id < 0 || old == null) {
            return;
        }
        String q = validQualification();
        if (q == null) {
            return;
        }
        try {
            if (Util.count(dbCon, "SELECT COUNT(*) FROM employee_qualification "
                    + "WHERE emp_id = ? AND UPPER(qualification) = UPPER(?) AND qualification <> ?",
                    id, q, old) > 0) {
                Util.showFieldError(lblQualError, "This employee already has this qualification");
                return;
            }
            if (!Util.confirm(this, "Change \"" + old + "\" to \"" + q + "\"?", "Confirm Update")) {
                return; // user clicked No: nothing changes
            }
            Util.update(dbCon, "UPDATE employee_qualification SET qualification = ? "
                    + "WHERE emp_id = ? AND qualification = ?", q, id, old);
            Util.info(this, "Qualification was updated successfully.");
            loadQualifications();
        } catch (SQLException e) {
            Util.sqlError(this, "updating qualification.", e);
        }
    }

    /** Deletes the selected qualification (after confirmation). */
    private void deleteQualification() {
        int id = selectedEmpId();
        String old = selectedQualification();
        if (id < 0 || old == null) {
            return;
        }
        if (!Util.confirm(this, "Delete qualification \"" + old + "\"?", "Confirm Delete")) {
            return; // user clicked No: nothing changes
        }
        try {
            Util.update(dbCon, "DELETE FROM employee_qualification WHERE emp_id = ? AND qualification = ?", id, old);
            Util.info(this, "Qualification was deleted successfully.");
            loadQualifications();
        } catch (SQLException e) {
            Util.sqlError(this, "deleting qualification.", e);
        }
    }

    /** Handles the buttons and the employee combo box. */
    public void actionPerformed(ActionEvent e) {
        Object src = e.getSource();
        if (src == cmbEmployee) {
            if (!loading) {
                loadQualifications(); // another employee was selected
            }
        } else if (src == btnAdd) {
            addQualification();
        } else if (src == btnUpdate) {
            updateQualification();
        } else if (src == btnDelete) {
            deleteQualification();
        }
    }

    /** Copies the qualification selected in the table into the text box. */
    public void valueChanged(ListSelectionEvent e) {
        if (!e.getValueIsAdjusting() && table.getSelectedRow() >= 0) {
            txtQualification.setText((String) model.getValueAt(table.getSelectedRow(), 0));
            lblQualError.setVisible(false);
        }
    }
}
