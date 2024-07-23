package com.flyme.update.helper;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;

import com.flyme.update.helper.activity.LogActivity;
import com.flyme.update.helper.utils.Config;
import com.flyme.update.helper.utils.CrashHandlerUtil;
import com.flyme.update.helper.utils.ShellInit;
import com.flyme.update.helper.utils.Utils;
import com.kongzue.dialogx.DialogX;
import com.topjohnwu.superuser.Shell;

import org.lsposed.hiddenapibypass.HiddenApiBypass;

public class App extends Application {

    public static int StatusBarHeight;

    static {
        HiddenApiBypass.addHiddenApiExemptions("");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        DialogX.init(this);
        CrashHandlerUtil.getInstance().init(this, LogActivity.class);
        Config.isVab = Utils.getprop("ro.build.ab_update").equals("true");
        Config.currentSlot = Utils.getprop("ro.boot.slot_suffix");
        Config.flymemodel = Utils.getprop("ro.product.flyme.model");
        @SuppressLint({"DiscouragedApi", "InternalInsetResource"})
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0)
            StatusBarHeight = getResources().getDimensionPixelSize(resourceId);
    }


}
