package me.yowal.updatehelper.utils;

import java.lang.reflect.Method;

public class PropUtils {
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
