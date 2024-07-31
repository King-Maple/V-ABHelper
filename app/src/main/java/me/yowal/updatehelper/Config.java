package me.yowal.updatehelper;

import android.text.TextUtils;

import com.topjohnwu.superuser.Shell;
import com.topjohnwu.superuser.ShellUtils;

import org.w3c.dom.Text;

import me.yowal.updatehelper.utils.LogUtils;
import me.yowal.updatehelper.utils.PropUtils;

public class Config {
    private static final String TAG = Config.class.getSimpleName();

    public static boolean isSAR = false;

    public static boolean recovery = false;

    public static boolean keepVerity = false;

    public static boolean keepEnc = false;

    public static boolean patchVbmeta = false;

    public static boolean isVab;

    public static String currentSlot;

    public static String flymemodel;

    public static void init() {
        isVab = PropUtils.getprop("ro.build.ab_update").equals("true");
        LogUtils.d(TAG, "isVab = " + isVab);
        currentSlot = PropUtils.getprop("ro.boot.slot_suffix");
        LogUtils.d(TAG, "currentSlot = " + currentSlot);
        flymemodel = PropUtils.getprop("ro.product.flyme.model");
        LogUtils.d(TAG, "flymemodel = " + flymemodel);

        // Magisk 的环境
        isSAR = !TextUtils.isEmpty(ShellUtils.fastCmd("grep ' / ' /proc/mounts | grep -qv 'rootfs'"));
        LogUtils.d(TAG, "isSAR = " + isSAR);
        keepVerity = isSAR;
        LogUtils.d(TAG, "keepVerity = " + keepVerity);
        recovery = !TextUtils.isEmpty(ShellUtils.fastCmd("grep ' / ' /proc/mounts | grep -q '/dev/root'"));
        LogUtils.d(TAG, "recovery = " + recovery);
        keepEnc = PropUtils.getprop("ro.crypto.state").equals("encrypted");
        LogUtils.d(TAG, "keepEnc = " + keepEnc);


    }
}
