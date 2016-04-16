package com.anuko.utils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.HashMap;

import java.util.ArrayList;
import java.util.List;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

/**
 * Outbound message delivery manager.
 * Implements logic to send an individual outbound message.
 *
 * @author Nik Okuntseff
 */
public class DeliveryManager {

    private static final Logger Log = LoggerFactory.getLogger(DatabaseManager.class);
    private static String origin = null;
    private static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    /**
     * Attempts to send an individual outgoing message.
     * In case of a failure it either sets a message for a retry or discards it.
     * We attempt to retry a few times, then discard.
     *
     * @param uuid the UUID of a message in ah_outbound table.
     */
    public static void deliver(String uuid) {
        // Obtain outgoing message parameters.
        HashMap<String, String> params = getMsgParams(uuid);
        if (params == null) return;

        // Do a post.
        boolean postResult = false;
        try {
            postResult = executePost(params);
        }
        catch (Exception e) {
            Log.error("Exception occured while executing a post: " + e.getMessage());
        }

        // Handle post result.
        int attempts = Integer.parseInt(params.get("attempts"));
        handlePostResult(postResult, uuid, attempts);
    }

    private static HashMap<String, String> getMsgParams(String uuid) {

        HashMap<String, String> map = null;
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        String remote = null;
        String message = null;
        String type = null;
        String uri = null;

        try {
            conn = DatabaseManager.getConnection();
            
            // Obtain data to send.
            pstmt = conn.prepareStatement("select o.remote, o.message, o.type, o.attempts, o.status, n.uri " +
                    "from ah_outbound o " +
                    "left join ah_nodes n on (n.uuid = o.remote) " +
                    "where o.uuid = ?");
            pstmt.setString(1, uuid);
            rs = pstmt.executeQuery();
            if (rs.next()) {
                map = new HashMap();
                map.put("uuid", uuid);
                map.put("remote", rs.getString(1));
                map.put("message", rs.getString(2));
                map.put("type", rs.getString(3));
                map.put("attempts", rs.getString(4));
                map.put("status", rs.getString(5));
                map.put("uri", rs.getString(6));
            }
        }
        catch (SQLException e) {
            Log.error(e.getMessage(), e);
        }
        finally {
            DatabaseManager.closeConnection(rs, pstmt, conn);
        }

        return map;
    }

    private static String getOrigin() {
        if (origin == null)
            origin = "51431704-fe3f-4d93-a0ad-c853d0a39999"; // TODO: obtain in from ah_node_details instead.
        return origin;
    }

    private static boolean executePost(HashMap<String, String> params)
        throws IOException {

        boolean result = false;
        CloseableHttpClient httpClient = HttpClients.createDefault();

        try {
            String uri = params.get("uri");
            if (!uri.startsWith("http://"))
                uri = "http://" + uri;

            HttpPost httpPost = new HttpPost(uri);

            // Set request parameters and properties.
            List<NameValuePair> nvps = new ArrayList<NameValuePair>(5);
            nvps.add(new BasicNameValuePair("uuid", params.get("uuid")));
            nvps.add(new BasicNameValuePair("origin", getOrigin()));
            nvps.add(new BasicNameValuePair("remote", params.get("remote")));
            nvps.add(new BasicNameValuePair("message", params.get("message")));
            nvps.add(new BasicNameValuePair("type", params.get("type")));
            httpPost.setEntity(new UrlEncodedFormEntity(nvps, "UTF-8"));
            CloseableHttpResponse response = httpClient.execute(httpPost);

            int statusCode = response.getStatusLine().getStatusCode();
Log.error("NIK DEBUG.... statusCode is: " + statusCode);
            if (statusCode == 200)
                result = true;
            response.close();
        }
        finally {
            httpClient.close();
        }
        return result;
    }

    private static void handlePostResult(boolean postSuccessful, String uuid, int attemptNum) {
Log.error("............................ we are in handlePostResult for " + postSuccessful + " " + uuid + " " + attemptNum);
        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            conn = DatabaseManager.getConnection();

            // If post was a success or if it was a final attempt - discard message and abandon further attempts.
            if (postSuccessful || attemptNum > 4) {

                pstmt = conn.prepareStatement("delete from ah_outbound where uuid = ?");
                pstmt.setString(1, uuid);
                pstmt.executeUpdate();
            } else {

                // Determine next try time.
                Calendar c = Calendar.getInstance();
                // Hardcoded increments for now.
                if (attemptNum == 0)
                    c.add(Calendar.MINUTE, +1); // Increment by one minute.
                else if (attemptNum == 1)
                    c.add(Calendar.MINUTE, +5); // Increment by 5 minutes.
                else if (attemptNum == 2)
                    c.add(Calendar.MINUTE, +15); // Increment by 15 minutes.
                else if (attemptNum == 3)
                    c.add(Calendar.MINUTE, +60); // Increment by 60 minutes.
                else
                    c.add(Calendar.MINUTE, +180); // Increment by 3 hours.

                //c.add(Calendar.DATE, +1); // Increment by one day for now.
                Date next_try = new Date(c.getTimeInMillis());
                String next_try_timestamp = sdf.format(next_try);
Log.error("............................. next_try_timestamp is: " + next_try_timestamp);

                pstmt = conn.prepareStatement("update ah_outbound set next_try_timestamp = ?, attempts = attempts + 1 where uuid = ?");
                pstmt.setString(1, next_try_timestamp);
                pstmt.setString(2, uuid);
                pstmt.executeUpdate();
            }

            // If we ultimately failed posting to a downstream node, deactivate it.
            if (!postSuccessful && attemptNum > 4) {
                // Was it a downstream node?
                // TODO: add code here...
            }
        }
        catch (SQLException e) {
            Log.error(e.getMessage(), e);
        }
        finally {
Log.error("........................ closing db connection in handlePostResult............");
            DatabaseManager.closeConnection(rs, pstmt, conn);
        }
    }
}
