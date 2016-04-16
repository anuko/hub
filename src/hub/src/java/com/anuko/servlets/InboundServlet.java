package com.anuko.servlets;

import com.anuko.utils.DatabaseManager;
import com.anuko.utils.SQLUtil;
import com.anuko.utils.HubUtil;
import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.anuko.utils.UUIDUtil;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Set;
import java.util.TreeMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.Calendar;

/**
 *
 * @author nik
 */
@WebServlet(name = "InboundServlet", urlPatterns = {"/in"})
public class InboundServlet extends HttpServlet {

    private static final Logger Log = LoggerFactory.getLogger(InboundServlet.class);
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    /**
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code>
     * methods.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");

        // Inbound servlet is used to process messages that come in.
        // What we need to do:
        //   1) Check if message was already processed. Abort if so.
        //   2) Insert messages in outbound queue:
        //     2.1) For all upstream nodes.
        //     2.2) For all downstream nodes.

        // Obtain and verify message parameters.
        String uuid = request.getParameter("uuid");
        if (!UUIDUtil.isUUID(uuid)) {
            Log.error("Invalid UUID: " + uuid);
            return;
        }
        String origin = request.getParameter("origin");
        if (!UUIDUtil.isUUID(origin)) {
            Log.error("Invalid UUID: " + origin);
            return;
        }
        String remote = request.getParameter("remote");
        if (!UUIDUtil.isUUID(remote)) {
            Log.error("Invalid UUID: " + remote);
            return;
        }
        String message = request.getParameter("message");
        int type = request.getParameter("type") == null ? 0 : Integer.parseInt(request.getParameter("type"));

        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = DatabaseManager.getConnection();

            // Is this message from a downstream node? If so, we need to update its status.
            if (HubUtil.isDownstream(request, origin) && !HubUtil.isNodeActive(request, origin)) {
                HubUtil.activateNode(request, origin);
            }
            // Was the message a ping? No further processing for pings.
            if (type == 0)
                return;

            Date now = new Date();
            String created_timestamp = sdf.format(now);

            // Check if we already processed this message. We do so by inserting a row into ah_inbound.
            pstmt = conn.prepareStatement("insert into ah_inbound " +
                "(uuid, origin, created_timestamp, message, type, status) " +
                "values(?, ?, ?, ?, ?, 0)");
            pstmt.setString(1, uuid);
            pstmt.setString(2, origin);
            pstmt.setString(3, created_timestamp);
            pstmt.setString(4, message);
            pstmt.setInt(5, type);

            int inserted = 0;
            try { inserted = pstmt.executeUpdate(); }
            catch (SQLException e) { /* This is normal when we try to insert a duplicate row. */ }
            if (inserted == 0) {
                Log.info("Message " + uuid + " is already processed.");
                return;
            }

            Log.info("Processing incomig message: " + uuid + ".");

            // Insert messages in outbound queue for all nodes.
            TreeMap nodes = (TreeMap) request.getServletContext().getAttribute("nodes");
            Set<String> keys = nodes.keySet();
            for (String key : keys) {
                pstmt = conn.prepareStatement("insert into ah_outbound " +
                    "(uuid, remote, created_timestamp, next_try_timestamp, message, type, attempts, status) " +
                    "values(?, ?, ?, ?, ?, ?, 0, 0)");
                pstmt.setString(1, uuid);
                pstmt.setString(2, key);
                pstmt.setString(3, created_timestamp);
                pstmt.setString(4, created_timestamp);
                pstmt.setString(5, message);
                pstmt.setInt(6, type);
                pstmt.executeUpdate();
            }

            // Clean up ah_inbound table from old messages.
            Calendar c = Calendar.getInstance();
            c.add(Calendar.DATE, -1);
            Date cutoff = new Date(c.getTimeInMillis());
            String cutoff_date = sdf.format(cutoff);

            pstmt = conn.prepareStatement("delete from ah_inbound where created_timestamp < ?");
            pstmt.setString(1, cutoff_date);
            pstmt.executeUpdate();
        }
        catch (SQLException e) {
            Log.error(e.getMessage(), e);
        }
        finally {
            DatabaseManager.closeConnection(rs, pstmt, conn);
        }

        PrintWriter out = response.getWriter();
        try {
            /* TODO output your page here. You may use following sample code. */
            out.println("<!DOCTYPE html>");
            out.println("<html>");
            out.println("<head>");
            out.println("<title>Servlet InboundServlet</title>");
            out.println("</head>");
            out.println("<body>");
            out.println("<h1>Servlet InboundServlet at " + request.getContextPath() + "</h1>");

            out.println("<p>uuid: " + uuid + "</p>");
            out.println("<p>remote: " + remote + "</p>");
            out.println("<p>message: " + message + "</p>");

            out.println("</body>");
            out.println("</html>");
        } finally {
            out.close();
        }
    }

    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    /**
     * Handles the HTTP <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Handles the HTTP <code>POST</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Short description";
    }// </editor-fold>
}
