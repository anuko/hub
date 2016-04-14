/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.anuko.servlets;

import com.anuko.utils.DatabaseManager;
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

        // Obtain and verify message parameters.
        String uuid = request.getParameter("uuid");
        if (!UUIDUtil.isUUID(uuid)) {
            Log.error("Invalid UUID: " + uuid);
            return;
        }
        String remote = request.getParameter("remote");
        if (!UUIDUtil.isUUID(remote)) {
            Log.error("Invalid UUID: " + remote);
            return;
        }
        String message = request.getParameter("message");

        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = DatabaseManager.getConnection();

            // TODO: document steps.
            // For example:
            // 1) Check if message was already processed.
            // 2) Process a locally attached server.
            // 3) Insert into outbound queue for further processing.

            // Check if we already processed this message. We do so by inserting a row into ah_inbound.
            pstmt = conn.prepareStatement("insert into ah_inbound (uuid) values(?)");
            pstmt.setString(1, uuid);
            int inserted = 0;
            try {
                inserted = pstmt.executeUpdate();
            }
            catch (SQLException e) {
                // This is normal when we attempt to insert a duplicate row.
            }
            if (inserted == 0) {
                Log.info("Message " + uuid + " is already processed.");
                return;
            }

            // TODO: do processing for a locally attached server. How?

            Log.info("Message " + uuid + " is NOT already processed.");
            // TODO: this is work in progress. Add other code here.


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
