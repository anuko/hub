/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.anuko.utils;

/**
 * Outbound message delivery manager.
 * Implements logic to send an individual outbound message.
 *
 * @author Nik Okuntseff
 */
public class DeliveryManager {

    /**
     * Attempts to send an individual outgoing message.
     * In case of a failure it either sets a message for a retry or discards it.
     * We attempt to retry a few times, then discard.
     *
     * @param uuid the UUID of a message in ah_outbound table.
     */
    public static void deliver(String uuid) {
        // This function is not implemented at this point. But we'll do it this way:

        // Obtain message parameters.
        // Construct and a POST to remote.
        // Discard message from ah_outbound if we are successful, or if it was our final attempt.
        // Adjust next_try_timestamp and attempts if we failed, unless it was a final attempt, i
    }
}
