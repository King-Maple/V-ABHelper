package com.flyme.update.helper.utils;

import android.text.TextUtils;

import com.topjohnwu.superuser.ShellUtils;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.stream.Collectors;

public class Utils {
    public static boolean dd(String ifile, String ofile) {
        return ShellUtils.fastCmdResult("dd if=" + ifile + " of=" + ofile);
    }


    public static void  reboot() {
        ShellUtils.fastCmd("/system/bin/svc power reboot || /system/bin/reboot");
    }

    public static String getprop(String key) {
        try{
            Class<?> c = Class.forName("android.os.SystemProperties");
            Method get = c.getMethod("get", String.class, String.class);
            return (String) (get.invoke(c, key, ""));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    public static boolean isAbDevice() {
        return Utils.getprop("ro.build.ab_update").equals("true");
    }

    public static String bytesToHex(byte[] byteArray) {
        if (byteArray == null) {
            return null;
        }
        char[] hexArray = "0123456789ABCDEF".toCharArray();
        char[] hexChars = new char[byteArray.length * 2];
        for (int j = 0; j < byteArray.length; j++) {
            int v = byteArray[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    public static byte[] hexTobytes(String hexString) {
        if (!TextUtils.isEmpty(hexString)) {
            return null;
        }
        hexString = hexString.toLowerCase().trim();
        final byte[] byteArray = new byte[hexString.length() >> 1];
        int index = 0;
        for (int i = 0; i < hexString.length(); i++) {
            if (index  > hexString.length() - 1) {
                return byteArray;
            }
            byte highDit = (byte) (Character.digit(hexString.charAt(index), 16) & 0xFF);
            byte lowDit = (byte) (Character.digit(hexString.charAt(index + 1), 16) & 0xFF);
            byteArray[i] = (byte) (highDit << 4 | lowDit);
            index += 2;
        }
        return byteArray;
    }

}
