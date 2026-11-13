/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package jdbcgui;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import javax.swing.JOptionPane;

/**
 *
 * @author wissam
 */
public class myDBCon {

    String DBURL = "jdbc:oracle:thin:@coeoracle.aus.edu:1521:orcl";
    String DBUSER = "b00100556";
    String DBPASS = "b00100556";

    boolean validLogin = false;

    Connection con;
    Statement statement;
    PreparedStatement prepStatement;
    ResultSet rs;

    public myDBCon() {
        try {
            // Load Oracle JDBC Driver
            Class.forName("oracle.jdbc.driver.OracleDriver");
            // Connect to Oracle Database
            con = DriverManager.getConnection(DBURL, DBUSER, DBPASS);

        } catch (ClassNotFoundException | SQLException e) {
            javax.swing.JLabel label = new javax.swing.JLabel("SQL ErroR - Retreiving usename/password.");
            label.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 18));
            JOptionPane.showMessageDialog(null, label, "ERROR", JOptionPane.ERROR_MESSAGE);
        }

    }

    /** Returns the open connection so other forms can run prepared statements. */
    public Connection getConnection() {
        return con;
    }

    // returns records selected 
    public ResultSet executeStatement(String strSQL) throws SQLException {
        // make the result set scrolable forward/backward updatable
        statement = con.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE);
        // populate valid mgr numbers 
        rs = statement.executeQuery(strSQL);
        return rs;
    }
    
    // update, insert (return number of records affected 
    public int executePrepared(String strSQL) throws SQLException {
        prepStatement = con.prepareStatement(strSQL);
        return prepStatement.executeUpdate();
    }
    
        // encrypts a password with SHA-256 and returns it as a 64-character hex string
    public static String encryptPassword(String password) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(password.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < bytes.length; i++) {
                sb.append(String.format("%02x", bytes[i]));
            }
            return sb.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

}