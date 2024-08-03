package me.yowal.updatehelper.application;

import android.app.Application;

import com.kongzue.dialogx.DialogX;

import me.yowal.updatehelper.Config;
import me.yowal.updatehelper.activity.LogActivity;
import me.yowal.updatehelper.utils.CrashHandlerUtil;
import me.yowal.updatehelper.utils.PropUtils;

public class myApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        DialogX.globalTheme = DialogX.THEME.AUTO;
        DialogX.init(this);
        Config.init();
        CrashHandlerUtil.getInstance().init(this, LogActivity.class);
    }
}
