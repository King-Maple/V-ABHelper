package com.flyme.update.helper.utils;

import com.flyme.update.helper.bean.UpdateInfo;

import org.apache.commons.io.IOUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class UpdateParser {
    private static final String FILE_URL_PREFIX = "file://";
    private final File mFile;

    public UpdateParser(String file) {
        this.mFile = new File(file);
    }

    private long getOffset(ZipEntry zipEntry, String str) {
        return (zipEntry.getMethod() != 0 ? 16 : 0) + 30 + str.length() + zipEntry.getCompressedSize() + (zipEntry.getExtra() != null ? zipEntry.getExtra().length : 0);
    }

    public UpdateInfo parse() {
        long payloadSize = 0,payloadOffset = 0,offset = 0;
        ZipFile zipFile = null;
        UpdateInfo mUpdateInfo = new UpdateInfo();
        mUpdateInfo.setUrl(FILE_URL_PREFIX + this.mFile.getAbsolutePath());
        mUpdateInfo.setType(-1);
        try {
            zipFile = new ZipFile(this.mFile);
            Enumeration<? extends ZipEntry> entries = zipFile.entries();
            while (entries.hasMoreElements()) {
                ZipEntry nextElement = entries.nextElement();
                String name = nextElement.getName();
                offset += getOffset(nextElement, name);

                if (nextElement.isDirectory()) {
                    offset -= nextElement.getCompressedSize();
                } else if ("payload.bin".equals(name)) {
                    if (nextElement.getMethod() != 0) {
                        break;
                    }
                    payloadSize = nextElement.getCompressedSize();
                    payloadOffset = offset - nextElement.getCompressedSize();
                } else if ("payload_properties.txt".equals(name)) {
                    try (BufferedReader buffer = new BufferedReader(new InputStreamReader(
                            zipFile.getInputStream(nextElement), StandardCharsets.UTF_8))) {
                        mUpdateInfo.setHeaderKeyValuePairs(buffer.lines().toArray(String[]::new));
                    }
                } else if ("META-INF/build.prop".equals(name)) {
                    try (BufferedReader buffer = new BufferedReader(new InputStreamReader(
                            zipFile.getInputStream(nextElement), StandardCharsets.UTF_8))) {
                        String line;
                        while ((line = buffer.readLine()) != null) {
                            if (line.startsWith("ro.product.flyme.model=")) {
                                mUpdateInfo.setFlymeid(propertyValue(line));
                            } else if (line.startsWith("ro.build.display.id=")) {
                                mUpdateInfo.setDisplayid(propertyValue(line));
                            } else if (line.startsWith("ro.build.mask.id=")) {
                                mUpdateInfo.setMaskid(propertyValue(line));
                            } else if (line.startsWith("ro.product.system.model=")) {
                                mUpdateInfo.setBuildInfo(propertyValue(line));
                            } else if (line.startsWith("ro.build.date.utc=")) {
                                try {
                                    mUpdateInfo.setBuildTimestamp(Long.parseLong(propertyValue(line)));
                                } catch (NumberFormatException ignored) {
                                    mUpdateInfo.setBuildTimestamp(0);
                                }
                            }
                        }
                    }
                } else if ("type.txt".equals(name)) {
                    String example = IOUtils.toString(zipFile.getInputStream(nextElement), StandardCharsets.UTF_8);
                    try {
                        mUpdateInfo.setType(Integer.parseInt(example.trim()));
                    } catch (NumberFormatException ignored) {
                        mUpdateInfo.setType(-1);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (zipFile != null) {
                try {
                    zipFile.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        mUpdateInfo.setOffset(payloadOffset);
        mUpdateInfo.setSize(payloadSize);
        return mUpdateInfo;
    }

    private static String propertyValue(String line) {
        int separator = line.indexOf('=');
        return separator >= 0 ? line.substring(separator + 1).trim() : "";
    }
}
