package com.flyme.update.helper;

import android.app.Application;
import android.content.Context;

import com.flyme.update.helper.utils.ShellInit;
import com.kongzue.dialogx.DialogX;
import com.topjohnwu.superuser.Shell;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class App extends Application {

    public static boolean isVab;

    public static String currentSlot;

    public static String flymemodel;

    @Override
    public void onCreate() {
        super.onCreate();
        DialogX.init(this);
        //DialogX.globalStyle = new KongzueStyle();
        isVab = getProp("ro.build.ab_update").equals("true");
        currentSlot = getProp("ro.boot.slot_suffix");
        flymemodel = getProp("ro.product.flyme.model");
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        Shell.setDefaultBuilder(Shell.Builder.create()
                .setInitializers(ShellInit.class)
                .setContext(base)
                .setTimeout(2));
    }

    private String getProp(String name) {
        String line;
        BufferedReader input = null;
        try {
            java.lang.Process p = Runtime.getRuntime().exec("getprop " + name);
            input = new BufferedReader(new InputStreamReader(p.getInputStream()), 1024);
            line = input.readLine();
            input.close();
        } catch (IOException ex) {
            return "";
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return line;
    }


}
