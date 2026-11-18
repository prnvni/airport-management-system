package jdbcgui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.regex.Pattern;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.RowFilter;
import javax.swing.WindowConstants;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;

/**
 * ViewTable: read-only window that shows the result of any SELECT query in a
 * table. It is used by the dashboard for "View Airports" and
 * "View Employees". The column titles are the column names (or aliases) of
 * the query.
 */
public class ViewTable extends JFrame implements ActionListener {

    myDBCon dbCon;
    DefaultTableModel model = new DefaultTableModel() {
        public boolean isCellEditable(int row, int col) {
            return false; // view only
        }
    };
    JTable table = new JTable(model);
    TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<DefaultTableModel>(model);
    JTextField txtSearch = new JTextField(22);
    JButton btnSearch = new JButton("Search");
    JButton btnClearSearch = new JButton("Show All");
    JButton btnRefresh = new JButton("Refresh");
    JButton btnClose = new JButton("Close");
    String sql;

    /** Builds the window and loads the rows of the query. */
    public ViewTable(String title, String sql) {
        this.sql = sql;
        setTitle("View " + title);
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        table.setFont(Util.FIELD_FONT);
        table.setRowHeight(24);
        table.getTableHeader().setFont(Util.LABEL_FONT);
        table.setRowSorter(sorter);

        txtSearch.setFont(Util.FIELD_FONT);
        btnSearch.setFont(Util.LABEL_FONT);
        btnClearSearch.setFont(Util.LABEL_FONT);
        btnRefresh.setFont(Util.LABEL_FONT);
        btnClose.setFont(Util.LABEL_FONT);

        btnSearch.addActionListener(this);
        btnClearSearch.addActionListener(this);
        btnRefresh.addActionListener(this);
        btnClose.addActionListener(this);
        txtSearch.addActionListener(this); // pressing Enter searches

        JPanel searchBar = new JPanel(new FlowLayout());
        searchBar.add(Util.makeLabel("Search:"));
        searchBar.add(txtSearch);
        searchBar.add(btnSearch);
        searchBar.add(btnClearSearch);

        JPanel top = new JPanel(new BorderLayout());
        JPanel titlePanel = new JPanel(new FlowLayout());
        titlePanel.add(Util.makeTitle(title));
        top.add(titlePanel, BorderLayout.NORTH);
        top.add(searchBar, BorderLayout.SOUTH);

        JPanel buttons = new JPanel(new FlowLayout());
        buttons.add(btnRefresh);
        buttons.add(btnClose);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(top, BorderLayout.NORTH);
        getContentPane().add(new JScrollPane(table), BorderLayout.CENTER);
        getContentPane().add(buttons, BorderLayout.SOUTH);

        dbCon = new myDBCon();
        loadRows();

        setPreferredSize(new Dimension(950, 480));
        pack();
        setLocationRelativeTo(null); // center form in screen 
    }

    /** Runs the query and fills the table with its columns and rows. */
    private void loadRows() {
        try {
            Statement st = dbCon.getConnection().createStatement();
            try {
                ResultSet rs = st.executeQuery(sql);
                ResultSetMetaData md = rs.getMetaData();
                int cols = md.getColumnCount();

                model.setRowCount(0);
                model.setColumnCount(0);
                for (int i = 1; i <= cols; i++) {
                    model.addColumn(md.getColumnLabel(i));
                }
                while (rs.next()) {
                    Object[] row = new Object[cols];
                    for (int i = 1; i <= cols; i++) {
                        row[i - 1] = rs.getObject(i);
                    }
                    model.addRow(row);
                }
                rs.close();
            } finally {
                st.close();
            }
        } catch (SQLException e) {
            Util.sqlError(this, "loading the rows", e);
        }
    }

    /** Applies a case insensitive filter across all columns. */
    private void applyFilter() {
        String text = txtSearch.getText().trim();
        if (text.isEmpty()) {
            sorter.setRowFilter(null);
        } else {
            sorter.setRowFilter(RowFilter.regexFilter("(?i)" + Pattern.quote(text)));
        }
    }
    /**
     * Refresh reloads the rows; Close closes only this window.
     */
    public void actionPerformed(ActionEvent e) {
        Object src = e.getSource();
        if (src == btnSearch || src == txtSearch) {
            applyFilter();
        } else if (src == btnClearSearch) {
            txtSearch.setText("");
            sorter.setRowFilter(null);
        } else if (src == btnRefresh) {
            loadRows();
            applyFilter();
        } else if (src == btnClose) {
            dispose();
        }
    }
}