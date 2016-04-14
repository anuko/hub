/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.anuko.servlets;

import com.anuko.utils.DatabaseManager;
import com.anuko.utils.SQLUtil;
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
import java.util.Set;
import java.util.TreeMap;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 *
 * @author nik
 */
@WebServlet(name = "InboundServlet", urlPatterns = {"/in"})
public class InboundServlet extends HttpServlet {

    private static final Logger Log = LoggerFactory.getLogger(InboundServlet.class);

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
        String local = request.getParameter("local");
        if (!UUIDUtil.isUUID(local)) {
            Log.error("Invalid UUID: " + local);
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
            if (isDownstream(request, local) && !isNodeActive(request, local)) {
                activateNode(request, local);
            }
            // Was the message a ping? No further processing for pings.
            if (type == 0)
                return;

            // Check if we already processed this message. We do so by inserting a row into ah_inbound.
            pstmt = conn.prepareStatement("insert into ah_inbound (uuid) values(?)");
            pstmt.setString(1, uuid);
            int inserted = 0;
            try { inserted = pstmt.executeUpdate(); }
            catch (SQLException e) { /* This is normal when we try to insert a duplicate row. */ }
            if (inserted == 0) {
                Log.info("Message " + uuid + " is already processed.");
                return;
            }

            Log.info("Processing incomig message: " + uuid + ".");

            // Insert messages in outbound queue for all upstream nodes.
            TreeMap upstreamNodes = (TreeMap) request.getServletContext().getAttribute("upstreamNodes");
            Set<String> keys = upstreamNodes.keySet();
            for (String key : keys) {

                // TODO: add created_timestamp.
                // TODO: add next_try_timestamp.
                // TODO: add message.
                // TODO: add status.

                pstmt = conn.prepareStatement("insert into ah_outbound (uuid, remote) values(?, ?)");
                pstmt.setString(1, uuid);
                pstmt.setString(2, key);
                pstmt.executeUpdate();
            }

            // Insert messages in outbound queue for all downstream nodes.
            TreeMap downstreamNodes = (TreeMap) request.getServletContext().getAttribute("downstreamNodes");
            keys = downstreamNodes.keySet();
            for (String key : keys) {

                // TODO: add created_timestamp.
                // TODO: add next_try_timestamp.
                // TODO: add message.
                // TODO: add status.

                pstmt = conn.prepareStatement("insert into ah_outbound (uuid, remote) values(?, ?)");
                pstmt.setString(1, uuid);
                pstmt.setString(2, key);
                pstmt.executeUpdate();
            }

            // Clean up ah_inbound table from old messages.
            // TODO: to clean we need to have the dates...
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

    boolean isDownstream(HttpServletRequest request, String uuid) {
        TreeMap downstreamNodes = (TreeMap) request.getServletContext().getAttribute("downstreamNodes");
        Set<String> keys = downstreamNodes.keySet();
        for (String key : keys) {
            if (key.equals(uuid))
                return true;
        }
        return false;
    }

    boolean isNodeActive(HttpServletRequest request, String uuid) {
        TreeMap downstreamNodes = (TreeMap) request.getServletContext().getAttribute("downstreamNodes");
        HashMap<String, String> map = (HashMap) downstreamNodes.get(uuid);
        String status = map.get("status");
        if (status != null && status.equals("1")) {
            return true;
        }
        return false;
    }

    void activateNode(HttpServletRequest request, String uuid) {

        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            // Activate node in the database.
            conn = DatabaseManager.getConnection();
            pstmt = conn.prepareStatement("update ah_downstream set status = 1 where uuid = ?");
            pstmt.setString(1, uuid);
            pstmt.executeUpdate();

            // Activate node in downstreamNodes map.
            activateNode((TreeMap)request.getServletContext().getAttribute("downstreamNodes"), uuid);
        }
        catch (SQLException e) {
            Log.error(e.getMessage(), e);
        }
        finally {
            DatabaseManager.closeConnection(rs, pstmt, conn);
        }
    }

    void activateNode(TreeMap downstreamNodes, String uuid) {
        Set<String> keys = downstreamNodes.keySet();
        for (String key : keys) {
            if (key.equals(uuid)) {
                HashMap m = (HashMap) downstreamNodes.get(key);
                m.put("status", "1");
                return;
            }
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
