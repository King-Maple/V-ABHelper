package com.flyme.update.helper;

import static org.junit.Assert.assertArrayEquals;

import com.flyme.update.helper.utils.PayloadProperties;

import org.junit.Test;

public class PayloadPropertiesTest {

    @Test
    public void appendsRequiredDowngradeHeaders() {
        String[] original = {"FILE_HASH=abc", "FILE_SIZE=12"};

        String[] result = PayloadProperties.withDowngrade(original);

        assertArrayEquals(new String[]{"FILE_HASH=abc", "FILE_SIZE=12", "POWERWASH=1", "SPL_DOWNGRADE=1"}, result);
        assertArrayEquals(new String[]{"FILE_HASH=abc", "FILE_SIZE=12"}, original);
    }

    @Test
    public void replacesExistingDowngradeHeaderValues() {
        String[] original = {"POWERWASH=0", "SPL_DOWNGRADE=0", "FILE_HASH=abc"};

        String[] result = PayloadProperties.withDowngrade(original);

        assertArrayEquals(new String[]{"FILE_HASH=abc", "POWERWASH=1", "SPL_DOWNGRADE=1"}, result);
    }
}
