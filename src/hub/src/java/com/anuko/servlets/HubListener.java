/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.anuko.servlets;

import com.anuko.utils.DatabaseManager;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletContext;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Web application lifecycle listener.
 *
 * @author nik
 */
public class HubListener implements ServletContextListener {

    private static final Logger Log = LoggerFactory.getLogger(HubListener.class);
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

        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = DatabaseManager.getConnection();
            pstmt = conn.prepareStatement("select uuid, uri from ah_nodes");
            rs = pstmt.executeQuery();
            // For now treat the above set as our upstream nodes, until this product matures.

            while (rs.next()) {
                upstreamNodes.put(rs.getString(1), rs.getString(2));
            }

            // Add the map to the application context.
            sce.getServletContext().setAttribute("upstreamNodes", upstreamNodes);

            // Insert messages into outgoing queue to ping upstream nodes.
            Set<String> keys = upstreamNodes.keySet();
            for(String key : keys){
                UUID uuid = UUID.randomUUID();
                String remote = key;
                // TODO: add created_timestamp.
                // TODO: add next_try_timestamp.
                // TODO: add message.
                // TODO: add status.

                // Determine if we already have a ping message queued.
                pstmt = conn.prepareStatement("select uuid from ah_outbound where remote = ?");
                pstmt.setString(1, remote);
                rs = pstmt.executeQuery();
                if (!rs.next()) {
                    // No ping message exists in queue, insert.
                    pstmt = conn.prepareStatement("insert into ah_outbound (uuid, remote) values(?, ?)");
                    pstmt.setString(1, uuid.toString());
                    pstmt.setString(2, remote);
                    pstmt.executeUpdate();
                }
            }
        }
        catch (SQLException e) {
            Log.error(e.getMessage(), e);
        }
        finally {
            DatabaseManager.closeConnection(rs, pstmt, conn);
        }

        // TODO: start outgoing message processing thread here.
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        Log.info(".............. contextDestroyed ........................");
    }
}
