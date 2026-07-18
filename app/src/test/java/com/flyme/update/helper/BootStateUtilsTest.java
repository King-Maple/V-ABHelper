package com.flyme.update.helper;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.flyme.update.helper.utils.BootStateUtils;

import org.junit.Test;

public class BootStateUtilsTest {

    @Test
    public void acceptsUnlockedOrangeBootConfig() {
        String bootConfig = "androidboot.verifiedbootstate = \"orange\"\n"
                + "androidboot.vbmeta.device_state = \"unlocked\"\n";

        assertTrue(BootStateUtils.isActuallyUnlocked(bootConfig));
    }

    @Test
    public void rejectsSpoofedRuntimeStateWhenBootConfigIsLocked() {
        String bootConfig = "androidboot.verifiedbootstate = \"green\"\n"
                + "androidboot.vbmeta.device_state = \"locked\"\n";

        assertFalse(BootStateUtils.isActuallyUnlocked(bootConfig));
    }

    @Test
    public void requiresBothBootloaderSignals() {
        assertFalse(BootStateUtils.isActuallyUnlocked(
                "androidboot.verifiedbootstate=orange\n"));
    }
}
