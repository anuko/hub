/*
Copyright Anuko International Ltd. (https://www.anuko.com)

LIBERAL FREEWARE LICENSE: This source code document may be used
by anyone for any purpose, and freely redistributed alone or in
combination with other software, provided that the license is obeyed.

There are only two ways to violate the license:

1. To redistribute this code in source form, with the copyright notice or
   license removed or altered. (Distributing in compiled forms without
   embedded copyright notices is permitted).

2. To redistribute modified versions of this code in *any* form
   that bears insufficient indications that the modifications are
   not the work of the original author(s).

This license applies to this document only, not any other software that it
may be combined with.
 */

package com.anuko.utils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Set;
import java.util.TreeMap;
import java.util.HashMap;
import javax.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author nik
 */
public class HubUtil {

    private static final Logger Log = LoggerFactory.getLogger(HubUtil.class);

    public static boolean isDownstream(HttpServletRequest request, String uuid) {
        TreeMap nodes = (TreeMap) request.getServletContext().getAttribute("nodes");
        Set<String> keys = nodes.keySet();
        for (String key : keys) {
            if (key.equals(uuid)) {
                HashMap<String, String> map = (HashMap) nodes.get(key);
                // Local servers (type == null) and downstream nodes (type == 0) are considered downstream.
                if (map.get("type") == null || map.get("type").equals("0"))
                    return true;
            }
        }
        return false;
    }

    public static boolean isNodeActive(HttpServletRequest request, String uuid) {
        TreeMap nodes = (TreeMap) request.getServletContext().getAttribute("nodes");
        HashMap<String, String> map = (HashMap) nodes.get(uuid);
        String status = map.get("status");
        if (status != null && status.equals("1")) {
            return true;
        }
        return false;
    }

    public static void activateNode(HttpServletRequest request, String uuid) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            // Activate node in the database.
            conn = DatabaseManager.getConnection();
            pstmt = conn.prepareStatement("update ah_nodes set status = 1 where uuid = ?");
            pstmt.setString(1, uuid);
            pstmt.executeUpdate();

            // Activate node in nodes map.
            activateNode((TreeMap)request.getServletContext().getAttribute("nodes"), uuid);
        }
        catch (SQLException e) {
            Log.error(e.getMessage(), e);
        }
        finally {
            DatabaseManager.closeConnection(rs, pstmt, conn);
        }
    }

    public static void activateNode(TreeMap nodes, String uuid) {
        Set<String> keys = nodes.keySet();
        for (String key : keys) {
            if (key.equals(uuid)) {
                HashMap m = (HashMap) nodes.get(key);
                m.put("status", "1");
                return;
            }
        }
    }

    public static void deactivateNode(HttpServletRequest request, String uuid) {
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            // Activate node in the database.
            conn = DatabaseManager.getConnection();
            pstmt = conn.prepareStatement("update ah_nodes set status = 0 where uuid = ?");
            pstmt.setString(1, uuid);
            pstmt.executeUpdate();

            // Deactivate node in nodes map.
            deactivateNode((TreeMap)request.getServletContext().getAttribute("nodes"), uuid);
        }
        catch (SQLException e) {
            Log.error(e.getMessage(), e);
        }
        finally {
            DatabaseManager.closeConnection(rs, pstmt, conn);
        }
    }

    public static void deactivateNode(TreeMap nodes, String uuid) {
        Set<String> keys = nodes.keySet();
        for (String key : keys) {
            if (key.equals(uuid)) {
                HashMap m = (HashMap) nodes.get(key);
                m.put("status", "0");
                return;
            }
        }
    }
}
