/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mockup;

import java.sql.*;
import java.util.UUID;


/**
 *
 * @author nik
 */
public class Mockup {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {

        // Generate random UUIDs - example.
        // UUID idOne = UUID.randomUUID();
        // UUID idTwo = UUID.randomUUID();
        // System.out.println("UUID One: " + idOne);
        // System.out.println("UUID Two: " + idTwo);

        // Experimentation with database access.
        try {
            String url = "jdbc:mysql://localhost:3306/hub";
            String username = "testuser";
            String password = "topsecret";

            Connection conn = DriverManager.getConnection(url, username, password);

            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT uuid, name FROM ah_nodes");

            if (rs.next()) {
                String uuid = rs.getString(1);
                String name = rs.getString(2);
                System.out.println("uuid: " + uuid + ", name: " + name);
            }

            rs.close();
            stmt.close();
            conn.close();

        } catch (SQLException e) {
            System.out.println("Exception caught: " + e.getMessage());
        }
    }
}
