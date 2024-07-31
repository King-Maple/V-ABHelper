package me.yowal.updatehelper.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Enumeration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import me.yowal.updatehelper.Natives;
import me.yowal.updatehelper.bean.UpdateInfo;
import me.yowal.updatehelper.manager.UpdateServiceManager;
import me.yowal.updatehelper.service.UpdateService;

public class UpdateUtils {
    private static final String FILE_URL_PREFIX = "file://";

    private static long getOffset(ZipEntry zipEntry, String str) {
        return (zipEntry.getMethod() != 0 ? 16 : 0) + 30 + str.length() + zipEntry.getCompressedSize() + (zipEntry.getExtra() != null ? zipEntry.getExtra().length : 0);
    }

    public static UpdateInfo parse(String file) {
        File mFile = new File(file);
        long payloadSize = 0,payloadOffset = 0,offset = 0;
        ZipFile zipFile = null;
        UpdateInfo mUpdateInfo = new UpdateInfo();
        mUpdateInfo.setUrl(FILE_URL_PREFIX + mFile.getAbsolutePath());
        mUpdateInfo.setType(-1);
        try {
            zipFile = new ZipFile(mFile);
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
                } else if ("META-INF/com/android/metadata.pb".equals(name)) {
                    byte[] example = IOUtils.toByteArray(zipFile.getInputStream(nextElement));
                    boolean isOta = Natives.isOtaZip(example);
                    LogUtils.d("UpdateUtils", "isOta = " + isOta);
                    mUpdateInfo.setType(isOta ? 1 : 0);
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
