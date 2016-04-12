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

/**
 *
 * @author nik
 */
public class Data {

    public static String dataSourceName = "java:comp/env/jdbc/hub";

    public static Connection getConnection()
    throws SQLException {
        try {
            Context ctx = new InitialContext();
            DataSource ds = (DataSource)ctx.lookup(dataSourceName);
            if (ds == null)
                throw new SQLException("No datasource "+dataSourceName);
            return ds.getConnection();

        } catch (NamingException x) {
            throw new SQLException("Cannot get datasource "+dataSourceName, x);
        }
    }
}
