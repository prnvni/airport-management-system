package jdbcgui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.sql.SQLException;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;

/**
 * Dashboard: main window of the airport application. It replaces the old
 * menu bar. It shows one card for Airports and one card for Employees; each
 * card shows how many rows are in the table and has the buttons that open
 * the existing forms. The counts refresh every time this window is
 * activated again (for example after closing the Add Airport form).
 */
public class Dashboard extends JFrame implements ActionListener {

    static final Color BACKGROUND = new Color(235, 241, 247);
    static final Color BLUE = new Color(30, 90, 160);

    myDBCon dbCon;

    JLabel lblAirportCount = new JLabel("0", SwingConstants.CENTER);
    JLabel lblEmployeeCount = new JLabel("0", SwingConstants.CENTER);
    JLabel lblGateCount = new JLabel("0", SwingConstants.CENTER);

    // airport card buttons
    JButton btnViewAirports = new JButton("View Airports");
    JButton btnAddAirport = new JButton("Add Airport");
    JButton btnUpdateAirport = new JButton("Update Airport");
    JButton btnDeleteAirport = new JButton("Delete Airport");

    // employee card buttons
    JButton btnViewEmployees = new JButton("View Employees");
    JButton btnAddEmployee = new JButton("Add Employee");
    JButton btnUpdateEmployee = new JButton("Update Employee");
    JButton btnDeleteEmployee = new JButton("Delete Employee");
    JButton btnQualifications = new JButton("Update Qualifications");
    
    // gate card buttons
    JButton btnViewGates = new JButton("View Gates");
    JButton btnAddGate = new JButton("Add Gate");
    JButton btnUpdateGate = new JButton("Update Gate");
    JButton btnDeleteGate = new JButton("Delete Gate");
    JButton btnExit = new JButton("Exit");

    /** Builds the dashboard, connects to the database and loads the counts. */
    public Dashboard() {
        setTitle("Airport Management - Dashboard");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        buildGui();
        dbCon = new myDBCon();
        refreshCounts();

        // refresh the numbers whenever the user comes back to this window
        addWindowListener(new WindowAdapter() {
            public void windowActivated(WindowEvent e) {
                refreshCounts();
            }
        });

        pack();
        setMinimumSize(new Dimension(1150, 520));
        setLocationRelativeTo(null); // center form in screen
    }

    /** Creates the title, the two cards and the exit button. */
    private void buildGui() {
        JLabel title = new JLabel("Airport Operations Management System", SwingConstants.CENTER);
        title.setFont(new Font("Tahoma", Font.BOLD, 30));
        title.setForeground(BLUE);
        title.setBorder(BorderFactory.createEmptyBorder(20, 10, 5, 10));

        JPanel cards = new JPanel(new GridLayout(1, 3, 25, 0));
        cards.setBackground(BACKGROUND);
        cards.setBorder(BorderFactory.createEmptyBorder(15, 30, 15, 30));
        cards.add(makeCard("Airports", lblAirportCount, new JButton[] {
            btnViewAirports, btnAddAirport, btnUpdateAirport, btnDeleteAirport }));
        cards.add(makeCard("Employees", lblEmployeeCount, new JButton[] {
            btnViewEmployees, btnAddEmployee, btnUpdateEmployee,
            btnDeleteEmployee, btnQualifications }));
        cards.add(makeCard("Gates", lblGateCount, new JButton[] {
            btnViewGates, btnAddGate, btnUpdateGate, btnDeleteGate }));

        btnExit.setFont(Util.LABEL_FONT);
        btnExit.addActionListener(this);
        JPanel bottom = new JPanel();
        bottom.setBackground(BACKGROUND);
        bottom.add(btnExit);

        JPanel top = new JPanel(new BorderLayout());
        top.setBackground(BACKGROUND);
        top.add(title, BorderLayout.CENTER);

        getContentPane().setBackground(BACKGROUND);
        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(top, BorderLayout.NORTH);
        getContentPane().add(cards, BorderLayout.CENTER);
        getContentPane().add(bottom, BorderLayout.SOUTH);
    }

    /** Creates one white card: name, big row count, then the column of buttons. */
    private JPanel makeCard(String name, JLabel countLabel, JButton[] buttons) {
        JLabel lblName = new JLabel(name, SwingConstants.CENTER);
        lblName.setFont(new Font("Tahoma", Font.BOLD, 24));

        countLabel.setFont(new Font("Tahoma", Font.BOLD, 56));
        countLabel.setForeground(BLUE);

        JPanel head = new JPanel(new GridLayout(2, 1));
        head.setOpaque(false);
        head.add(lblName);
        head.add(countLabel);

        JPanel buttonPanel = new JPanel(new GridLayout(buttons.length, 1, 0, 8));
        buttonPanel.setOpaque(false);
        for (int i = 0; i < buttons.length; i++) {
            buttons[i].setFont(Util.LABEL_FONT);
            buttons[i].setFocusPainted(false);
            buttons[i].addActionListener(this);
            buttonPanel.add(buttons[i]);
        }

        JPanel card = new JPanel(new BorderLayout(0, 15));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200)),
                BorderFactory.createEmptyBorder(20, 25, 20, 25)));
        card.add(head, BorderLayout.NORTH);
        card.add(buttonPanel, BorderLayout.CENTER);
        return card;
    }

    /** Reads the number of airports and employees from the database. */
    private void refreshCounts() {
        if (dbCon == null) {
            return;
        }
        try {
            lblAirportCount.setText("" + Util.count(dbCon, "SELECT COUNT(*) FROM airport"));
            lblEmployeeCount.setText("" + Util.count(dbCon, "SELECT COUNT(*) FROM a_employee"));
        } catch (SQLException e) {
            Util.sqlError(this, "reading the counts", e);
        }
        try {
            lblGateCount.setText("" + Util.count(dbCon, "SELECT COUNT(*) FROM gate"));
        } catch (SQLException e) {
            lblGateCount.setText("-");
        }
    }

    /** Opens the form that matches the clicked button (or exits). */
    public void actionPerformed(ActionEvent e) {
        Object src = e.getSource();

        if (src == btnExit) {
            System.exit(0);
        } else if (src == btnViewAirports) {
            (new ViewTable("Airports",
                    "SELECT a.airport_code AS \"Code\", a.airport_name AS \"Name\", "
                    + "a.airport_city AS \"City\", a.airport_country AS \"Country\", "
                    + "m.first_name || ' ' || m.last_name AS \"Manager\" "
                    + "FROM airport a LEFT JOIN a_employee m ON m.emp_id = a.manager_id "
                    + "ORDER BY a.airport_code")).setVisible(true);
        } else if (src == btnAddAirport) {
            (new AddAirport()).setVisible(true);
        } else if (src == btnUpdateAirport || src == btnDeleteAirport) {
            // the existing form has both the Update and the Delete button
            (new UpdateDeleteAirport()).setVisible(true);
        } else if (src == btnViewEmployees) {
            (new ViewTable("Employees",
                    "SELECT e.emp_id AS \"ID\", e.first_name AS \"First name\", "
                    + "e.last_name AS \"Last name\", e.role AS \"Role\", "
                    + "e.salary AS \"Salary\", e.airport_code AS \"Airport\", "
                    + "s.first_name || ' ' || s.last_name AS \"Supervisor\", "
                    + "e.airline_code AS \"Airline\" "
                    + "FROM a_employee e LEFT JOIN a_employee s ON s.emp_id = e.supervisor_id "
                    + "ORDER BY e.emp_id")).setVisible(true);
        } else if (src == btnAddEmployee) {
            (new AddStaff()).setVisible(true);
        } else if (src == btnUpdateEmployee || src == btnDeleteEmployee) {
            // the existing form has both the Update and the Delete button
            (new UpdateDeleteStaff()).setVisible(true);
        } else if (src == btnQualifications) {
            (new StaffQualifications()).setVisible(true);
        } else if (src == btnViewGates) {
            (new ViewTable("Gates",
                    "SELECT g.airport_code AS \"Airport\", g.gate_no AS \"Gate No\", "
                    + "g.terminal AS \"Terminal\" FROM gate g "
                    + "ORDER BY g.airport_code, g.gate_no")).setVisible(true);
        } else if (src == btnAddGate) {
            (new AddGate()).setVisible(true);
        } else if (src == btnUpdateGate || src == btnDeleteGate) {
            // the form has both the Update and the Delete button
            (new UpdateDeleteGate()).setVisible(true);
        }
    }
}
