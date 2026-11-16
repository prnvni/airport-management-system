package jdbcgui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
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
    JLabel lblAssignCount = new JLabel("0", SwingConstants.CENTER);

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
    
    // gate assignment card buttons
    JButton btnViewAssignments = new JButton("View Assignments");
    JButton btnAddAssignment = new JButton("Add Assignment");
    JButton btnUpdateAssignment = new JButton("Update Assignment");
    JButton btnDeleteAssignment = new JButton("Delete Assignment");

    // ===== ADDED (flights section): labels =====
    JLabel lblAirlineCount = new JLabel("0", SwingConstants.CENTER);
    JLabel lblAircraftCount = new JLabel("0", SwingConstants.CENTER);
    JLabel lblFlightCount = new JLabel("0", SwingConstants.CENTER);
 
    // ===== ADDED (flights section): airline card buttons =====
    JButton btnViewAirlines = new JButton("View Airlines");
    JButton btnAddAirline = new JButton("Add Airline");
    JButton btnUpdateAirline = new JButton("Update Airline");
    JButton btnDeleteAirline = new JButton("Delete Airline");
 
    // ===== ADDED (flights section): aircraft card buttons =====
    JButton btnViewAircraft = new JButton("View Aircraft");
    JButton btnAddAircraft = new JButton("Add Aircraft");
    JButton btnUpdateAircraft = new JButton("Update Aircraft");
    JButton btnDeleteAircraft = new JButton("Delete Aircraft");
 
    // ===== ADDED (flights section): flight card buttons =====
    JButton btnViewFlights = new JButton("View Flights");
    JButton btnAddFlight = new JButton("Add Flight");
    JButton btnUpdateFlight = new JButton("Update Flight");
    JButton btnDeleteFlight = new JButton("Delete Flight");
    JButton btnFlightCrew = new JButton("Manage Crew");

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
        JLabel title = new JLabel("\u2708  Airport Operations Management System  \u2708", SwingConstants.CENTER);
        title.setFont(new Font("Tahoma", Font.BOLD, 30));
        title.setForeground(Color.WHITE);
        title.setBorder(BorderFactory.createEmptyBorder(20, 10, 5, 10));

        JPanel cards = new JPanel(new GridLayout(2, 5, 20, 20));
        cards.setOpaque(false);
        cards.setBorder(BorderFactory.createEmptyBorder(15, 30, 15, 30));
        cards.add(makeCard("Airports", lblAirportCount, new JButton[] {
            btnViewAirports, btnAddAirport, btnUpdateAirport, btnDeleteAirport }));
        cards.add(makeCard("Employees", lblEmployeeCount, new JButton[] {
            btnViewEmployees, btnAddEmployee, btnUpdateEmployee,
            btnDeleteEmployee, btnQualifications }));
        cards.add(makeCard("Gates", lblGateCount, new JButton[] {
            btnViewGates, btnAddGate, btnUpdateGate, btnDeleteGate }));
        cards.add(makeCard("Gate Assignments", lblAssignCount, new JButton[] {
            btnViewAssignments, btnAddAssignment, btnUpdateAssignment, btnDeleteAssignment }));
        // ===== ADDED (flights section): cards =====
        cards.add(makeCard("Airlines", lblAirlineCount, new JButton[] {
            btnViewAirlines, btnAddAirline, btnUpdateAirline, btnDeleteAirline }));
        cards.add(makeCard("Aircraft", lblAircraftCount, new JButton[] {
            btnViewAircraft, btnAddAircraft, btnUpdateAircraft, btnDeleteAircraft }));
        cards.add(makeCard("Flights", lblFlightCount, new JButton[] {
            btnViewFlights, btnAddFlight, btnUpdateFlight, btnDeleteFlight, btnFlightCrew }));

        // ADDED: transparent scroll pane so nothing is cut off on small screens
        JScrollPane cardScroll = new JScrollPane(cards);
        cardScroll.setOpaque(false);
        cardScroll.getViewport().setOpaque(false);
        cardScroll.setBorder(null);
        cardScroll.getVerticalScrollBar().setUnitIncrement(16);



        btnExit.setFont(Util.LABEL_FONT);
        btnExit.addActionListener(this);
        JPanel bottom = new JPanel();
        bottom.setOpaque(false);
        bottom.add(btnExit);

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(title, BorderLayout.CENTER);

        // background picture (falls back to a plain light background if the file is missing)
        java.net.URL url = getClass().getResource("/jdbcgui/background.jpg");
        final java.awt.Image bg = (url == null) ? null : new javax.swing.ImageIcon(url).getImage();

        JPanel sky = new JPanel(new BorderLayout()) {
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                if (bg != null) {
                    g2.drawImage(bg, 0, 0, getWidth(), getHeight(), this);
                    // dark see-through layer so the white title stays readable
                    g2.setColor(new Color(0, 0, 0, 90));
                    g2.fillRect(0, 0, getWidth(), getHeight());
                } else {
                    g2.setColor(BACKGROUND);
                    g2.fillRect(0, 0, getWidth(), getHeight());
                }
            }
        };
        setContentPane(sky);
        sky.add(top, BorderLayout.NORTH);
        sky.add(cards, BorderLayout.CENTER);
        sky.add(bottom, BorderLayout.SOUTH);
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
        try {
            lblAssignCount.setText("" + Util.count(dbCon, "SELECT COUNT(*) FROM gate_assignment"));
        } catch (SQLException e) {
            lblAssignCount.setText("-");
        }
        // ===== ADDED (flights section) =====
        setCount(lblAirlineCount, "airline");
        setCount(lblAircraftCount, "aircraft");
        setCount(lblFlightCount, "flight");
    }

    /** ADDED: shows the row count of one table, or "-" if the table cannot be read. */
    private void setCount(JLabel label, String table) {
        try {
            label.setText("" + Util.count(dbCon, "SELECT COUNT(*) FROM " + table));
        } catch (SQLException e) {
            label.setText("-");
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
        } else if (src == btnViewAssignments) {
            (new ViewTable("Gate Assignments",
                    "SELECT ga.flight_number AS \"Flight\", f.dept_airport || ' to ' || f.arr_airport AS \"Route\", "
                    + "ga.airport_code AS \"Airport\", ga.gate_no AS \"Gate No\", g.terminal AS \"Terminal\", "
                    + "e.first_name || ' ' || e.last_name AS \"Employee\" "
                    + "FROM gate_assignment ga "
                    + "JOIN flight f ON f.flight_number = ga.flight_number "
                    + "JOIN gate g ON g.airport_code = ga.airport_code AND g.gate_no = ga.gate_no "
                    + "LEFT JOIN a_employee e ON e.emp_id = ga.emp_id "
                    + "ORDER BY ga.flight_number, ga.airport_code")).setVisible(true);
        } else if (src == btnAddAssignment) {
            (new AddGateAssignment()).setVisible(true);
        } else if (src == btnUpdateAssignment || src == btnDeleteAssignment) {
            // the form has both the Update and the Delete button
            (new UpdateDeleteGateAssignment()).setVisible(true);
        // ===== ADDED (flights section): airlines =====
        } else if (src == btnViewAirlines) {
            (new ViewTable("Airlines",
                    "SELECT airline_code AS \"Code\", airline_name AS \"Name\", "
                    + "airline_country AS \"Country\" FROM airline "
                    + "ORDER BY airline_code")).setVisible(true);
        } else if (src == btnAddAirline) {
            (new AddAirline()).setVisible(true);
        } else if (src == btnUpdateAirline || src == btnDeleteAirline) {
            (new UpdateDeleteAirline()).setVisible(true);
 
        // ===== ADDED (flights section): aircraft =====
        } else if (src == btnViewAircraft) {
            (new ViewTable("Aircraft",
                    "SELECT a.aircraft_id AS \"ID\", a.model AS \"Model\", "
                    + "a.capacity AS \"Capacity\", a.manufacture_year AS \"Year\", "
                    + "al.airline_name AS \"Airline\" "
                    + "FROM aircraft a JOIN airline al ON al.airline_code = a.airline_code "
                    + "ORDER BY a.aircraft_id")).setVisible(true);
        } else if (src == btnAddAircraft) {
            (new AddAircraft()).setVisible(true);
        } else if (src == btnUpdateAircraft || src == btnDeleteAircraft) {
            (new UpdateDeleteAircraft()).setVisible(true);
 
        // ===== ADDED (flights section): flights =====
        } else if (src == btnViewFlights) {
            (new ViewTable("Flights",
                    "SELECT f.flight_number AS \"Flight\", f.dept_airport AS \"From\", "
                    + "f.arr_airport AS \"To\", "
                    + "TO_CHAR(f.dept_time, 'YYYY-MM-DD HH24:MI') AS \"Departure\", "
                    + "TO_CHAR(f.arr_time, 'YYYY-MM-DD HH24:MI') AS \"Arrival\", "
                    + "ROUND((CAST(f.arr_time AS DATE) - CAST(f.dept_time AS DATE)) * 24, 2) AS \"Duration (h)\", "
                    + "f.status AS \"Status\", a.model AS \"Aircraft\", al.airline_name AS \"Airline\" "
                    + "FROM flight f "
                    + "JOIN aircraft a ON a.aircraft_id = f.aircraft_id "
                    + "JOIN airline al ON al.airline_code = a.airline_code "
                    + "ORDER BY f.dept_time")).setVisible(true);
        } else if (src == btnAddFlight) {
            (new AddFlight()).setVisible(true);
        } else if (src == btnUpdateFlight || src == btnDeleteFlight) {
            (new UpdateDeleteFlight()).setVisible(true);
        } else if (src == btnFlightCrew) {
            (new FlightCrew()).setVisible(true);
        }
    }
    }
}