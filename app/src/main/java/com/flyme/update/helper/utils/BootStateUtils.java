package com.flyme.update.helper.utils;

public final class BootStateUtils {
    private static final String VERIFIED_BOOT_STATE = "androidboot.verifiedbootstate=orange";
    private static final String VBMETA_DEVICE_STATE = "androidboot.vbmeta.device_state=unlocked";

    private BootStateUtils() {
    }

    public static boolean isActuallyUnlocked(String bootConfig) {
        if (bootConfig == null || bootConfig.isEmpty()) {
            return false;
        }
        String normalized = bootConfig.replace("\"", "").replaceAll("\\s+", "");
        return normalized.contains(VERIFIED_BOOT_STATE)
                && normalized.contains(VBMETA_DEVICE_STATE);
    }
}
