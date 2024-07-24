package com.flyme.update.helper.utils;

import android.content.Context;
import android.util.Log;

import com.flyme.update.helper.R;
import com.topjohnwu.superuser.Shell;
import com.topjohnwu.superuser.ShellUtils;

import java.io.InputStream;

public class ShellInit extends Shell.Initializer {

    private String fastCmd(Shell shell,String cmd) {
        return ShellUtils.fastCmd(shell, cmd);
    }

    private boolean getBool(Shell shell,String name) {
        return fastCmd(shell,"echo $" + name).equals("true");
    }

    @Override
    public boolean onInit(Context context, Shell shell) {
        InputStream bashrc = context.getResources().openRawResource(R.raw.manager);
        shell.newJob()
                .add(bashrc)
                .add("app_init")
                .exec();
        Config.isSAR = getBool(shell, "SYSTEM_ROOT");
        Config.recovery = getBool(shell, "RECOVERYMODE");
        Config.keepVerity = getBool(shell, "KEEPVERITY");
        Config.keepEnc = getBool(shell, "KEEPFORCEENCRYPT");
        Config.patchVbmeta = getBool(shell, "PATCHVBMETAFLAG");
        return true;
    }

}
