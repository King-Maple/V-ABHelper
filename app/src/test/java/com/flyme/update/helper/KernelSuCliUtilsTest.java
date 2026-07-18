package com.flyme.update.helper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.flyme.update.helper.utils.KernelSuCliUtils;

import org.junit.Test;

public class KernelSuCliUtilsTest {

    @Test
    public void parsesModernKernelVersion() {
        assertEquals(32525, KernelSuCliUtils.parseVersion("Kernel Version: 32525\n"));
        assertEquals(32525, KernelSuCliUtils.parseVersion("version: 32525\nflags: 0x1\n"));
    }

    @Test
    public void parsesLkmMode() {
        String info = "version: 32525\nflags: 0x1\nlkm: true\nlate_load: false\n";

        assertTrue(KernelSuCliUtils.hasLkmField(info));
        assertTrue(KernelSuCliUtils.isLkmMode(info));
    }

    @Test
    public void rejectsMissingOrMalformedFields() {
        assertEquals(-1, KernelSuCliUtils.parseVersion("ksud 3.2.5\n"));
        assertFalse(KernelSuCliUtils.hasLkmField("version: 32525\n"));
        assertFalse(KernelSuCliUtils.isLkmMode("version: 32525\n"));
    }
}
