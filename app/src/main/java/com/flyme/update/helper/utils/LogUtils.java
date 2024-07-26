package com.flyme.update.helper.utils;

import android.util.Log;

public class LogUtils {
    private static boolean enableLog = true;

    public static void d(String tag, String msg) {
        if (enableLog)
            Log.d(tag, msg);
    }

    public static void e(String tag, String msg) {
        if (enableLog)
            Log.e(tag, msg);
    }

    public static void w(String tag, String msg) {
        if (enableLog)
            Log.w(tag, msg);
    }

    public static void i(String tag, String msg) {
        if (enableLog)
            Log.i(tag, msg);
    }

    public static void v(String tag, String msg) {
        if (enableLog)
            Log.v(tag, msg);
    }
}
