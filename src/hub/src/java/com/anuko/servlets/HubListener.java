package com.anuko.servlets;

import com.anuko.utils.DatabaseManager;
import com.anuko.utils.SQLUtil;
import com.anuko.utils.OutboundThread;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.HashMap;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletContext;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.text.SimpleDateFormat;
import java.util.Date;


/**
 * Hub application listener.
 * Its methods are called during application initialization and destruction.
 * We use the contextInitialized call to initialize the application.
 * Here, we determine who our upstream and downstream nodes are
 * and put this information in application context for further use.
 *
 * @author Nik Okuntseff
 */
public class HubListener implements ServletContextListener {

    private static final Logger Log = LoggerFactory.getLogger(HubListener.class);
    private static ServletContext context;
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    Thread outboundThread = new Thread(new OutboundThread());

    /**
     * Initializes Hub application as a whole.
     *
     * @param sce the ServletContextEvent containing the ServletContext that is being initialized.
     */
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        // We get here when the application initializes.
        // We do our application initialization steps here.
        // Specifically:
        //   1) Read nodes info.
        //   2) Put it into application servlet context for further use.
        //   3) Insert messages into outgoing queue to ping upstream nodes.

        context = sce.getServletContext();

        // Read nodes info.
        Map nodes = new TreeMap();

        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = DatabaseManager.getConnection();
            pstmt = conn.prepareStatement("select uuid, type, name, uri, status from ah_nodes");
            rs = pstmt.executeQuery();

            while (rs.next()) {
                HashMap<String, String> map = SQLUtil.rowToMap(rs);
                nodes.put(rs.getString(1), map);
            }
            // Add the map to the application context.
             context.setAttribute("nodes", nodes);

            // Insert messages into outgoing queue to ping upstream nodes.
            Set<String> keys = nodes.keySet();
            for (String key : keys) {
                HashMap<String, String> m = (HashMap) nodes.get(key);
                boolean upstream = (m.get("type") != null && m.get("type").equals("1"));
                if (!upstream) continue;

                UUID uuid = UUID.randomUUID();
                String remote = key;
                Date now = new Date();
                String created_timestamp = sdf.format(now);

                // Determine if we already have a ping message queued.
                pstmt = conn.prepareStatement("select uuid from ah_outbound where remote = ?");
                pstmt.setString(1, remote);
                rs = pstmt.executeQuery();
                if (!rs.next()) {
                    // No previous ping message exists, insert.
                    pstmt = conn.prepareStatement("insert into ah_outbound " +
                        "(uuid, remote, created_timestamp, next_try_timestamp, type, attempts, status) " +
                        "values(?, ?, ?, ?, 0, 0, 0)");
                    pstmt.setString(1, uuid.toString());
                    pstmt.setString(2, remote);
                    pstmt.setString(3, created_timestamp);
                    pstmt.setString(4, created_timestamp);
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

        // Start outgoing message processing thread.
        outboundThread.start();
    }

    /**
     * Does cleanup before application exit.
     *
     * @param sce the ServletContextEvent containing the ServletContext that is being destroyed.
     */
    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        outboundThread.interrupt();
        context = null;
    }

    /**
     * Returns application servlet context.
     * Intended use is for other areas of the application that are not aware of it.
     */
    public static ServletContext getServletContext() {
        return context;
    }
}
