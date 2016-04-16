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

import java.util.Set;
import java.util.TreeMap;
import java.util.HashMap;
import javax.servlet.http.HttpServletRequest;

/**
 *
 * @author nik
 */
public class HubUtil {

    boolean isDownstream(HttpServletRequest request, String uuid) {
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
}
