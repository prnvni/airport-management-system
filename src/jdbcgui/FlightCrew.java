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
import javax.swing.ListSelectionModel;
import javax.swing.WindowConstants;
import javax.swing.table.DefaultTableModel;

/**
 * FlightCrew: form used to manage the FLIGHT_CREW table.
 * The user picks a flight (the flight numbers are kept in the flightNos list
 * in the same order); the table then shows the crew of that flight. A crew
 * member is added from the employee combo box, which lists only employees of
 * the flight's airline who are not already on the crew (the emp_id values are
 * kept in the empIds list). Removing a crew member asks for confirmation.
 */
public class FlightCrew extends JFrame implements ActionListener {

    myDBCon dbCon;
    ArrayList<String> flightNos = new ArrayList<String>(); // flight_number of each item in the flight combo box
    ArrayList<String> empIds = new ArrayList<String>();    // emp_id of each name in the employee combo box
    boolean loading = false; // true while the flight combo box is being filled

    JComboBox<String> cmbFlight = new JComboBox<String>();
    JComboBox<String> cmbEmployee = new JComboBox<String>();
    DefaultTableModel model = new DefaultTableModel(new Object[]{"Emp ID", "Name", "Role"}, 0) {
        public boolean isCellEditable(int row, int col) {
            return false; // the table is read only
        }
    };
    JTable table = new JTable(model);
    JLabel lblCrewError = Util.makeErrorLabel();
    JButton btnAdd = new JButton("Add to Crew");
    JButton btnRemove = new JButton("Remove Selected");

    /** Builds the form, connects to the database and loads the flights. */
    public FlightCrew() {
        setTitle("Flight Crew");
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        buildGui();
        dbCon = new myDBCon();
        loadFlights();
        loadCrew();
        pack();
        setResizable(false);
        setLocationRelativeTo(null); // center form in screen
    }

    /** Creates and lays out all the components of the form. */
    private void buildGui() {
        cmbFlight.addActionListener(this);
        table.setFont(Util.FIELD_FONT);
        table.setRowHeight(24);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        JScrollPane scroll = new JScrollPane(table);
        scroll.setPreferredSize(new java.awt.Dimension(560, 200));

        JPanel form = Util.makeFormPanel();
        Util.addRow(form, 0, "Flight:", cmbFlight, null);
        Util.addRow(form, 1, "Employee to add:", cmbEmployee, lblCrewError);

        JPanel center = new JPanel(new BorderLayout());
        center.add(form, BorderLayout.NORTH);
        center.add(scroll, BorderLayout.CENTER);
        center.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

        JPanel buttons = new JPanel(new FlowLayout());
        JButton[] all = {btnAdd, btnRemove};
        for (int i = 0; i < all.length; i++) {
            all[i].setFont(Util.LABEL_FONT);
            all[i].addActionListener(this);
            buttons.add(all[i]);
        }

        JPanel top = new JPanel(new FlowLayout());
        top.add(Util.makeTitle("Flight Crew"));

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(top, BorderLayout.NORTH);
        getContentPane().add(center, BorderLayout.CENTER);
        getContentPane().add(buttons, BorderLayout.SOUTH);
    }

    /** Fills the flight combo box with flights shown as "number - FROM to TO (departure)". */
    private void loadFlights() {
        loading = true;
        try {
            Util.fillCombo(dbCon, cmbFlight, flightNos,
                    "SELECT flight_number, flight_number || ' - ' || dept_airport || ' to ' || arr_airport "
                    + "|| ' (' || TO_CHAR(dept_time, 'YYYY-MM-DD HH24:MI') || ')' "
                    + "FROM flight ORDER BY dept_time", null);
        } catch (SQLException e) {
            Util.sqlError(this, "loading flights.", e);
        }
        loading = false;
    }

    /** Returns the flight number of the selected flight, or -1 if none. */
    private int selectedFlight() {
        int i = cmbFlight.getSelectedIndex();
        if (i < 0) {
            return -1;
        }
        return Integer.parseInt(flightNos.get(i));
    }

    /** Fills the employee combo box with employees of the flight's airline who are not on the crew yet. */
    private void loadEmployeeChoices(int flight) {
        cmbEmployee.removeAllItems();
        empIds.clear();
        if (flight < 0) {
            return;
        }
        try {
            Util.fillCombo(dbCon, cmbEmployee, empIds,
                    "SELECT emp_id, first_name || ' ' || last_name || ' - ' || role || ' (ID ' || emp_id || ')' "
                    + "FROM a_employee "
                    + "WHERE airline_code = (SELECT a.airline_code FROM flight f "
                    + "JOIN aircraft a ON a.aircraft_id = f.aircraft_id WHERE f.flight_number = " + flight + ") "
                    + "AND emp_id NOT IN (SELECT emp_id FROM flight_crew WHERE flight_number = " + flight + ") "
                    + "ORDER BY role, first_name, last_name", null);
        } catch (SQLException e) {
            Util.sqlError(this, "loading employees.", e);
        }
        if (cmbEmployee.getItemCount() == 0) {
            Util.showFieldError(lblCrewError, "No more employees of this airline available");
        }
    }

    /** Shows the crew of the selected flight in the table and refreshes the employee choices. */
    private void loadCrew() {
        model.setRowCount(0);
        lblCrewError.setVisible(false);
        int flight = selectedFlight();
        loadEmployeeChoices(flight);
        if (flight < 0) {
            return;
        }
        try {
            Statement st = dbCon.getConnection().createStatement();
            ResultSet r = st.executeQuery("SELECT e.emp_id, e.first_name || ' ' || e.last_name, e.role "
                    + "FROM flight_crew fc JOIN a_employee e ON e.emp_id = fc.emp_id "
                    + "WHERE fc.flight_number = " + flight + " ORDER BY e.role, e.first_name, e.last_name");
            while (r.next()) {
                model.addRow(new Object[]{r.getString(1), r.getString(2), r.getString(3)});
            }
            r.close();
            st.close();
        } catch (SQLException e) {
            Util.sqlError(this, "loading crew.", e);
        }
    }

    /** Adds the chosen employee to the crew of the selected flight. */
    private void addCrewMember() {
        int flight = selectedFlight();
        int i = cmbEmployee.getSelectedIndex();
        if (flight < 0 || i < 0) {
            Util.showFieldError(lblCrewError, "Choose a flight and an employee first");
            return;
        }
        Integer emp = Integer.valueOf(empIds.get(i));
        try {
            if (Util.count(dbCon, "SELECT COUNT(*) FROM flight_crew WHERE flight_number = ? AND emp_id = ?",
                    Integer.valueOf(flight), emp) > 0) {
                Util.showFieldError(lblCrewError, "This employee is already on the crew");
                return;
            }
            Util.update(dbCon, "INSERT INTO flight_crew (flight_number, emp_id) VALUES (?, ?)",
                    Integer.valueOf(flight), emp);
            Util.info(this, "Crew member was added successfully.");
            loadCrew();
        } catch (SQLException e) {
            Util.sqlError(this, "adding crew member.", e);
        }
    }

    /** Removes the crew member selected in the table (after confirmation). */
    private void removeCrewMember() {
        int flight = selectedFlight();
        int row = table.getSelectedRow();
        if (flight < 0 || row < 0) {
            Util.error(this, "Select a crew member from the table first.");
            return;
        }
        Integer emp = Integer.valueOf(model.getValueAt(row, 0).toString());
        String name = model.getValueAt(row, 1).toString();
        if (!Util.confirm(this, "Remove " + name + " from flight " + flight + "?", "Confirm Remove")) {
            return; // user clicked No: nothing changes
        }
        try {
            Util.update(dbCon, "DELETE FROM flight_crew WHERE flight_number = ? AND emp_id = ?",
                    Integer.valueOf(flight), emp);
            Util.info(this, "Crew member was removed successfully.");
            loadCrew();
        } catch (SQLException e) {
            Util.sqlError(this, "removing crew member.", e);
        }
    }

    /** Handles the buttons and the flight combo box. */
    public void actionPerformed(ActionEvent e) {
        Object src = e.getSource();
        if (src == cmbFlight) {
            if (!loading) {
                loadCrew(); // another flight was selected
            }
        } else if (src == btnAdd) {
            addCrewMember();
        } else if (src == btnRemove) {
            removeCrewMember();
        }
    }
}
