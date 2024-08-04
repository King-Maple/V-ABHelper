package me.yowal.updatehelper.utils;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.text.TextUtils;

import com.topjohnwu.superuser.Shell;
import com.topjohnwu.superuser.ShellUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class Utils {

    public static void openUrl(Context context, String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        /*if (url.contains("www.coolapk.com")) {
            intent.setClassName("com.coolapk.market", "com.coolapk.market.view.AppLinkActivity");
            intent.setAction("android.intent.action.VIEW");
        }*/
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    public static void sendEmail(Context context,String email) {
        String[] mailto = { email };
        Intent sendIntent = new Intent(Intent.ACTION_SEND);
        String emailBody = "";
        sendIntent.setType("message/rfc822");
        sendIntent.putExtra(Intent.EXTRA_EMAIL, mailto);
        sendIntent.putExtra(Intent.EXTRA_SUBJECT, "");
        sendIntent.putExtra(Intent.EXTRA_TEXT, emailBody);
        context.startActivity(sendIntent);
    }

    public static void  reboot() {
        reboot("");
    }

    public static void  reboot(String reason) {
        if (reason.equals("recovery")) {
            // KEYCODE_POWER = 26, hide incorrect "Factory data reset" message
            ShellUtils.fastCmd("/system/bin/input keyevent 26");
        }
        ShellUtils.fastCmd("/system/bin/svc power reboot " + reason + " || /system/bin/reboot " + reason);
    }

    public static String getVerName(Context context) {
        String verName = "";
        try {
            verName = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return verName;
    }

    public static boolean keyChecked(String skey) {
        boolean isDigit = false;
        boolean isLetter = false;
        for (int i = 0; i < skey.length(); i++) {
            char ch = skey.charAt(i);
            if (Character.isDigit(ch))
                isDigit = true;
            if (Character.isLetter(ch))
                isLetter = true;
        }
        return isDigit && isLetter && skey.matches("^[a-zA-Z0-9]+$");
    }

    public static String bytesToHex(byte[] byteArray) {
        if (byteArray == null) {
            return null;
        }
        char[] hexArray = "0123456789ABCDEF".toCharArray();
        char[] hexChars = new char[byteArray.length * 2];
        for (int j = 0; j < byteArray.length; j++) {
            int v = byteArray[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars).toLowerCase();
    }

    public static byte[] hexTobytes(String hexString) {
        if (!TextUtils.isEmpty(hexString)) {
            return null;
        }
        hexString = hexString.toLowerCase().trim();
        final byte[] byteArray = new byte[hexString.length() >> 1];
        int index = 0;
        for (int i = 0; i < hexString.length(); i++) {
            if (index  > hexString.length() - 1) {
                return byteArray;
            }
            byte highDit = (byte) (Character.digit(hexString.charAt(index), 16) & 0xFF);
            byte lowDit = (byte) (Character.digit(hexString.charAt(index + 1), 16) & 0xFF);
            byteArray[i] = (byte) (highDit << 4 | lowDit);
            index += 2;
        }
        return byteArray;
    }

    public static int findBytes(byte[] data, byte[] subData) {
        if (subData == null || subData.length == 0) { return -1; }
        if (data == null || data.length == 0) { return -1; }
        for (int i = 0; i <= data.length - subData.length; i++) {
            int j;
            for (j = 0; j < subData.length; j++) {
                if (data[i + j] != subData[j]) {
                    break;
                }
            }
            if (j == subData.length) {
                return i;
            }
        }
        return -1;
    }


    //解压lib
    public static boolean unLibrary(String apkPath, String soName, String outPath) {
        FileOutputStream fos = null;
        FileInputStream in_zip= null;
        ZipInputStream zin_zip= null;
        try {
            boolean result = false;
            fos = new FileOutputStream(outPath);
            in_zip = new FileInputStream(apkPath);
            zin_zip = new ZipInputStream(in_zip);
            ZipEntry entry;
            //开始遍历zip文件
            while ((entry = zin_zip.getNextEntry()) != null) {
                //判断是否是某文件
                String zipFileName = entry.getName();
                if (zipFileName.equals(soName)) {
                    result = IOUtils.copy(zin_zip, fos) > 0;
                    break;
                }
            }
            return result;
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            IOUtils.close(fos);
            IOUtils.close(in_zip);
            IOUtils.close(zin_zip);
        }
        return false;
    }

}
