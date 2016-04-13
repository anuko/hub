/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.anuko.utils;

import java.sql.Connection;
import java.sql.SQLException;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author nik
 */
public class DatabaseManager {

    public static String dataSourceName = "java:comp/env/jdbc/hub";

    private static final Logger Log = LoggerFactory.getLogger(DatabaseManager.class);

    public static Connection getConnection() throws SQLException {
        try {
            Context ctx = new InitialContext();
            DataSource ds = (DataSource)ctx.lookup(dataSourceName);
            if (ds == null)
                throw new SQLException("No datasource " + dataSourceName);
            return ds.getConnection();

        } catch (NamingException e) {
            throw new SQLException("Cannot get datasource " + dataSourceName, e);
        }
    }

    public static void closeConnection(Connection conn) {
        if (conn != null) {
            try {
               conn.close();
            }
            catch (Exception e) {
                Log.error(e.getMessage(), e);
            }
        }
    }

    public static void closeResultSet(ResultSet rs) {
        if (rs != null) {
            try {
                    rs.close();
                }
            catch (SQLException e) {
                Log.error(e.getMessage(), e);
            }
        }
    }

    public static void closeStatement(Statement stmt) {
        if (stmt != null) {
            try {
                stmt.close();
            }
            catch (Exception e) {
                Log.error(e.getMessage(), e);
            }
        }
    }

    public static void closeStatement(ResultSet rs, Statement stmt) {
        closeResultSet(rs);
        closeStatement(stmt);
    }

    public static void closeConnection(ResultSet rs, Statement stmt, Connection conn) {
        closeResultSet(rs);
        closeStatement(stmt);
        closeConnection(conn);
    }

    public static void closeConnection(Statement stmt, Connection con) {
        closeStatement(stmt);
        closeConnection(con);
    }
}
