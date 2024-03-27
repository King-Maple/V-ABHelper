package com.flyme.update.helper.utils;

import com.topjohnwu.superuser.ShellUtils;

import java.lang.reflect.Method;

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
}
