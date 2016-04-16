package com.anuko.utils;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author nik
 */
public class SQLUtil {

    public static final HashMap<String, String> rowToMap(ResultSet rs)
    throws SQLException {

        ResultSetMetaData meta = rs.getMetaData();
        HashMap<String, String> map = new HashMap();

        for (int i = 1; i < meta.getColumnCount()+1; i++) {
            map.put(meta.getColumnName(i), rs.getString(i));
        }
        return map;
    }
}
