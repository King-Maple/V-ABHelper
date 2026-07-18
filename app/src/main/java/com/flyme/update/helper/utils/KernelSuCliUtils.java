package com.flyme.update.helper.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class KernelSuCliUtils {
    private static final Pattern VERSION = Pattern.compile(
            "(?m)^(?:Kernel Version|version):\\s*(\\d+)\\s*$");
    private static final Pattern LKM = Pattern.compile("(?m)^lkm:\\s*(true|false)\\s*$");

    private KernelSuCliUtils() {
    }

    public static int parseVersion(String output) {
        if (output == null) {
            return -1;
        }
        Matcher matcher = VERSION.matcher(output);
        if (!matcher.find()) {
            return -1;
        }
        try {
            return Integer.parseInt(matcher.group(1));
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    public static boolean hasLkmField(String output) {
        return output != null && LKM.matcher(output).find();
    }

    public static boolean isLkmMode(String output) {
        if (output == null) {
            return false;
        }
        Matcher matcher = LKM.matcher(output);
        return matcher.find() && Boolean.parseBoolean(matcher.group(1));
    }
}
