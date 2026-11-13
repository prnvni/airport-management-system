package jdbcgui;

/**
 * JDBCGUI: main class of the application. It starts the program by opening
 * the Menu window.
 */
public class JDBCGUI {

    /**
     * Starts the application.
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        (new Menu()).setVisible(true);
    }
}
