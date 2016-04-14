/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.anuko.utils;

import java.util.UUID;

/**
 *
 * @author nik
 */
public class UUIDUtil {

    public static boolean isUUID(String uuid) {
        if (uuid != null && uuid.matches("[0-9a-f]{8}-[0-9a-f]{4}-[34][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}")) {
            return true;
        }
        return false;
    }
}
