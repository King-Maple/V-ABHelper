package com.flyme.update.helper.utils;

import java.util.ArrayList;
import java.util.List;

public final class PayloadProperties {
    private static final String POWERWASH_PREFIX = "POWERWASH=";
    private static final String SPL_DOWNGRADE_PREFIX = "SPL_DOWNGRADE=";

    private PayloadProperties() {
    }

    public static String[] withDowngrade(String[] properties) {
        List<String> result = new ArrayList<>();
        if (properties != null) {
            for (String property : properties) {
                if (property != null
                        && !property.startsWith(POWERWASH_PREFIX)
                        && !property.startsWith(SPL_DOWNGRADE_PREFIX)) {
                    result.add(property);
                }
            }
        }
        result.add("POWERWASH=1");
        result.add("SPL_DOWNGRADE=1");
        return result.toArray(new String[0]);
    }
}
