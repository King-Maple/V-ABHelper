package com.flyme.update.helper;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import com.flyme.update.helper.bean.UpdateInfo;
import com.flyme.update.helper.utils.UpdateParser;

import org.junit.Test;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class UpdateParserTest {

    @Test
    public void parsesStoredPayloadAndFlymeMetadata() throws Exception {
        byte[] payload = "CrAU-payload".getBytes(StandardCharsets.UTF_8);
        File ota = createOta(payload, "1\n");

        UpdateInfo info = new UpdateParser(ota.getAbsolutePath()).parse();

        assertEquals(payload.length, info.getSize());
        assertEquals(1, info.getType());
        assertEquals("M2481", info.getFlymeid());
        assertEquals("Flyme 10.5.5.1A", info.getDisplayid());
        assertEquals(1739436037L, info.getBuildTimestamp());
        assertArrayEquals(new String[]{"FILE_HASH=abc", "FILE_SIZE=12"}, info.getHeaderKeyValuePairs());

        byte[] parsedPayload = new byte[payload.length];
        try (RandomAccessFile file = new RandomAccessFile(ota, "r")) {
            file.seek(info.getOffset());
            file.readFully(parsedPayload);
        }
        assertArrayEquals(payload, parsedPayload);
    }

    @Test
    public void malformedTypeDoesNotCrashSelectionFlow() throws Exception {
        File ota = createOta("payload".getBytes(StandardCharsets.UTF_8), "full\n");

        UpdateInfo info = new UpdateParser(ota.getAbsolutePath()).parse();

        assertEquals(-1, info.getType());
        assertEquals("M2481", info.getFlymeid());
    }

    private static File createOta(byte[] payload, String type) throws Exception {
        File ota = Files.createTempFile("vabhelper-ota-", ".zip").toFile();
        ota.deleteOnExit();
        try (ZipOutputStream zip = new ZipOutputStream(Files.newOutputStream(ota.toPath()))) {
            addText(zip, "META-INF/build.prop",
                    "ro.product.flyme.model=M2481\n"
                            + "ro.build.display.id=Flyme 10.5.5.1A\n"
                            + "ro.build.date.utc=1739436037\n"
                            + "ro.build.mask.id=14-test\n"
                            + "ro.product.system.model=MEIZU 21 Pro\n");
            addStored(zip, "payload.bin", payload);
            addText(zip, "payload_properties.txt", "FILE_HASH=abc\nFILE_SIZE=12\n");
            addText(zip, "type.txt", type);
        }
        return ota;
    }

    private static void addText(ZipOutputStream zip, String name, String value) throws Exception {
        zip.putNextEntry(new ZipEntry(name));
        zip.write(value.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    private static void addStored(ZipOutputStream zip, String name, byte[] value) throws Exception {
        CRC32 crc = new CRC32();
        crc.update(value);
        ZipEntry entry = new ZipEntry(name);
        entry.setMethod(ZipEntry.STORED);
        entry.setSize(value.length);
        entry.setCompressedSize(value.length);
        entry.setCrc(crc.getValue());
        zip.putNextEntry(entry);
        zip.write(value);
        zip.closeEntry();
    }
}
