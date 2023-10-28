package com.flyme.update.helper.utils;

import org.apache.commons.io.IOUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Enumeration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
                    BufferedReader buffer = new BufferedReader(new InputStreamReader(zipFile.getInputStream(nextElement)));
                    mUpdateInfo.setHeaderKeyValuePairs(buffer.lines().toArray(String[]::new));
                } else if ("META-INF/build.prop".equals(name)) {
                    BufferedReader buffer = new BufferedReader(new InputStreamReader(zipFile.getInputStream(nextElement)));
                    String[] example = buffer.lines().toArray(String[]::new);
                    for (String str : example) {
                        if (str.contains("ro.product.flyme.model")) {
                            Pattern pattern = Pattern.compile("ro.product.flyme.model=(.*)\\n");
                            Matcher matcher = pattern.matcher(str + "\n");
                            if (matcher.find())
                                mUpdateInfo.setFlymeid(matcher.group(1));
                        } else if (str.contains("ro.build.display.id")) {
                            Pattern pattern = Pattern.compile("ro.build.display.id=(.*)\\n");
                            Matcher matcher = pattern.matcher(str + "\n");
                            if (matcher.find())
                                mUpdateInfo.setDisplayid(matcher.group(1));
                        } else if (str.contains("ro.build.mask.id")) {
                            Pattern pattern = Pattern.compile("ro.build.mask.id=(.*)\\n");
                            Matcher matcher = pattern.matcher(str + "\n");
                            if (matcher.find())
                                mUpdateInfo.setMaskid(matcher.group(1));
                        } else if (str.contains("ro.product.system.model")) {
                            Pattern pattern = Pattern.compile("ro.product.system.model=(.*)\\n");
                            Matcher matcher = pattern.matcher(str + "\n");
                            if (matcher.find())
                                mUpdateInfo.setBuildInfo(matcher.group(1));
                        }
                    }
                } else if ("type.txt".equals(name)) {
                    String example = IOUtils.toString(zipFile.getInputStream(nextElement));
                    mUpdateInfo.setType(Integer.parseInt(example));
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
}
