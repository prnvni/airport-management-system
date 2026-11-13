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
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.WindowConstants;
import javax.swing.table.DefaultTableModel;

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

        btnRefresh.setFont(Util.LABEL_FONT);
        btnClose.setFont(Util.LABEL_FONT);
        btnRefresh.addActionListener(this);
        btnClose.addActionListener(this);
        JPanel buttons = new JPanel(new FlowLayout());
        buttons.add(btnRefresh);
        buttons.add(btnClose);

        JPanel top = new JPanel(new FlowLayout());
        top.add(Util.makeTitle(title));

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(top, BorderLayout.NORTH);
        getContentPane().add(new JScrollPane(table), BorderLayout.CENTER);
        getContentPane().add(buttons, BorderLayout.SOUTH);

        dbCon = new myDBCon();
        loadRows();

        setPreferredSize(new Dimension(900, 450));
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

    /** Refresh reloads the rows; Close closes only this window. */
    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == btnRefresh) {
            loadRows();
        } else if (e.getSource() == btnClose) {
            dispose();
        }
    }
}
