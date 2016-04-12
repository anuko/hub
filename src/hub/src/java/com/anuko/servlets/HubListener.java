/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.anuko.servlets;

import com.anuko.utils.Data;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.sql.DataSource;
import javax.servlet.ServletContext;
import java.util.UUID;

/**
 * Web application lifecycle listener.
 *
 * @author nik
 */
public class HubListener implements ServletContextListener {

    ServletContext context;

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        // We get here when the application initializes.
        // We do our application initialization steps here.
        // Specifically:
        //   1) Read upstream nodes info.
        //   2) Put it into application servlet context for further use.
        //   3) Insert messages into outgoing queue to ping upstream nodes.

        // Read upstream nodes info.
        Map upstreamNodes = new TreeMap();
        try {
            Connection conn = Data.getConnection();
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("select uuid, uri from ah_nodes");
            // For now treat the above set as our upstream nodes, until this product matures.

            while (rs.next()) {
                upstreamNodes.put(rs.getString(1), rs.getString(2));
            }

            rs.close();
            stmt.close();
            conn.close();

        } catch (Exception e) {
            System.out.println("Exception caught: " + e.getMessage());
        }

        // Add the map to the application context.
        context = sce.getServletContext();
        context.setAttribute("upstreamNodes", upstreamNodes);

        // Insert messages into outgoing queue to ping upstream nodes.
        Set<String> keys = upstreamNodes.keySet();
        for(String key : keys){

            UUID uuid = UUID.randomUUID();
            String remote = key;
            // TODO: add created_timestamp.
            // TODO: add next_try_timestamp.
            // TODO: add message.
            // TODO: add status.
            String query = "insert into ah_msgs_out (uuid, remote) values('" + uuid + "', '" + remote + "')";
            // TODO: redo the above as prepared statement.

            try {
                Connection conn = Data.getConnection();
                Statement stmt = conn.createStatement();
                int i = stmt.executeUpdate(query);
                stmt.close();
                conn.close();

            } catch (Exception e) {
                System.out.println("Exception caught: " + e.getMessage());
            }
        }

        // TODO: start outgoing message processing thred here.
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        System.out.println(".............. contextDestroyed ........................");
    }
}
