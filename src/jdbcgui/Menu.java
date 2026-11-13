package jdbcgui;

import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;
import javax.swing.WindowConstants;

/**
 * Menu: main window of the airport application. Its menu bar opens every
 * form: Airport (add, update/delete), Staff (add, update/delete, qualifications)
 * and File (exit).
 */
public class Menu extends JFrame implements ActionListener {

    static final Font MENU_FONT = new Font("Arial Rounded MT Bold", Font.PLAIN, 22);

    JMenuItem itemExit;
    JMenuItem itemAddAirport;
    JMenuItem itemUpdateAirport;
    JMenuItem itemAddStaff;
    JMenuItem itemUpdateStaff;
    JMenuItem itemQualifications;

    /** Creates the menu window and centers it on the screen. */
    public Menu() {
        buildGui();
        this.setLocationRelativeTo(null); // center form in screen
    }

    /** Creates the background picture and the whole menu bar. */
    private void buildGui() {
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setTitle("Airport Management - Menu");
        setResizable(false);

        // background picture (employees.jpg is in the jdbcgui package); skipped if missing
        java.net.URL pic = getClass().getResource("/jdbcgui/employees.jpg");
        if (pic != null) {
            getContentPane().add(new JLabel(new ImageIcon(pic)));
        } else {
            getContentPane().setPreferredSize(new java.awt.Dimension(700, 400));
        }

        JMenuBar bar = new JMenuBar();

        JMenu menuFile = makeMenu("File");
        itemExit = makeItem(menuFile, "Exit", KeyEvent.VK_X);
        bar.add(menuFile);

        JMenu menuAirport = makeMenu("Airport");
        itemAddAirport = makeItem(menuAirport, "Add New Airport", KeyEvent.VK_1);
        itemUpdateAirport = makeItem(menuAirport, "Update/Delete Airport", KeyEvent.VK_2);
        bar.add(menuAirport);

        JMenu menuStaff = makeMenu("Staff");
        itemAddStaff = makeItem(menuStaff, "Add New Staff", KeyEvent.VK_3);
        itemUpdateStaff = makeItem(menuStaff, "Update/Delete Staff", KeyEvent.VK_4);
        itemQualifications = makeItem(menuStaff, "Qualifications", KeyEvent.VK_5);
        bar.add(menuStaff);

        setJMenuBar(bar);
        pack();
    }

    /** Creates a menu with the standard menu font. */
    private JMenu makeMenu(String text) {
        JMenu m = new JMenu(text);
        m.setFont(MENU_FONT);
        return m;
    }

    /** Creates a menu item (with a Ctrl+key shortcut), adds it to the menu and listens to its clicks. */
    private JMenuItem makeItem(JMenu menu, String text, int key) {
        JMenuItem item = new JMenuItem(text);
        item.setFont(MENU_FONT);
        item.setAccelerator(KeyStroke.getKeyStroke(key, InputEvent.CTRL_DOWN_MASK));
        item.addActionListener(this);
        menu.add(item);
        return item;
    }

    /** Opens the form that matches the clicked menu item (or exits). */
    public void actionPerformed(ActionEvent e) {
        Object src = e.getSource();

        if (src == itemExit) {
            System.exit(0);
        } else if (src == itemAddAirport) {
            (new AddAirport()).setVisible(true);
        } else if (src == itemUpdateAirport) {
            (new UpdateDeleteAirport()).setVisible(true);
        } else if (src == itemAddStaff) {
            (new AddStaff()).setVisible(true);
        } else if (src == itemUpdateStaff) {
            (new UpdateDeleteStaff()).setVisible(true);
        } else if (src == itemQualifications) {
            (new StaffQualifications()).setVisible(true);
        }
    }
}