package com.anuko.utils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Outbound message delivery manager.
 * Implements logic to send an individual outbound message.
 *
 * @author Nik Okuntseff
 */
public class DeliveryManager {

    private static final Logger Log = LoggerFactory.getLogger(DatabaseManager.class);

    /**
     * Attempts to send an individual outgoing message.
     * In case of a failure it either sets a message for a retry or discards it.
     * We attempt to retry a few times, then discard.
     *
     * @param uuid the UUID of a message in ah_outbound table.
     */
    public static void deliver(String uuid) {

        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = DatabaseManager.getConnection();

            // Obtain data to send.
            // Under construction...
            /*
            pstmt = conn.prepareStatement("select uuid, remote, message " +
                    "from ah_outbound " +
                    "left join ah_upstream"
                    where uuid = ?");"
                            + ""
            
                            "select o.uuid, o.remote, o.message, u.uri as up_uri from ah_outbound o left join ah_upstream u on (u.uuid = o.remote)"
                            + "
            pstmt.setString(1, uuid);
            rs = pstmt.executeQuery();
            if (!rs.next()) return;
            
Log.error("deliver deliver ................ " + rs.getString(1) + " " + rs.getString(2) + " ............................");
            */
            

            }
            catch (SQLException e) {
                Log.error(e.getMessage(), e);
            }
            finally {
                DatabaseManager.closeConnection(rs, pstmt, conn);
            }
        
        // This function is not implemented at this point. But we'll do it this way:

        // Obtain message parameters.
        // Construct and a POST to remote.
        // Discard message from ah_outbound if we are successful, or if it was our final attempt.
        // Adjust next_try_timestamp and attempts if we failed, unless it was a final attempt, i
    }
}
