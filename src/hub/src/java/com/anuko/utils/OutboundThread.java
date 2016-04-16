package com.anuko.utils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Outbound message processing thread.
 * Implements a thread that processes outgoing messages.
 *
 * @author Nik Okuntseff
 */
public class OutboundThread implements Runnable {

    private static final Logger Log = LoggerFactory.getLogger(OutboundThread.class);
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    /**
     * Implements outbound message processing.
     */
    @Override
    public void run() {

        while (!Thread.currentThread().isInterrupted()) {

            Connection conn = null;
            PreparedStatement pstmt = null;
            ResultSet rs = null;

            try {
                conn = DatabaseManager.getConnection();

                // Obtain message uuids ready for outbound delivery.
                Date nowDate = new Date();
                String now = sdf.format(nowDate);
                pstmt = conn.prepareStatement("select uuid " +
                    "from ah_outbound where next_try_timestamp <= ?");
                pstmt.setString(1, now);
                rs = pstmt.executeQuery();
                while (rs.next()) {
                    DeliveryManager.deliver(rs.getString(1));

                    // Exit the thread if we are interrupted.
                    if (Thread.currentThread().isInterrupted()) {
                        Log.info("OutboundThread was interrupted during delivery loop.");
                        return;
                    }
                }
            }
            catch (SQLException e) {
                Log.error(e.getMessage(), e);
            }
            finally {
                DatabaseManager.closeConnection(rs, pstmt, conn);
            }

            // Wait a minute.
            try {
                Thread.sleep(60000);
            }
            catch (InterruptedException e) {
                Log.info("OutboundThread interrupted while sleeping.");
                return;
            }
        }
    }
}
