package com.flyme.update.helper.shell;

import android.content.Context;
import android.util.Log;

import com.flyme.update.helper.R;
import com.flyme.update.helper.utils.Config;
import com.topjohnwu.superuser.Shell;
import com.topjohnwu.superuser.ShellUtils;

import org.apache.commons.io.IOUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

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
        try {
            String result = new BufferedReader(new InputStreamReader(bashrc))
                    .lines().collect(Collectors.joining("\n"));
            shell.newJob()
                    .add(result)
                    .add("app_init")
                    .exec();
            Config.isSAR = getBool(shell, "SYSTEM_ROOT");
            Log.d("ShellInit","isSAR = " + Config.isSAR);
            Config.recovery = getBool(shell, "RECOVERYMODE");
            Log.d("ShellInit","recovery = " + Config.recovery);
            Config.keepVerity = getBool(shell, "KEEPVERITY");
            Log.d("ShellInit","keepVerity = " + Config.keepVerity);
            Config.keepEnc = getBool(shell, "KEEPFORCEENCRYPT");
            Log.d("ShellInit","keepEnc = " + Config.keepEnc);
            Config.patchVbmeta = getBool(shell, "PATCHVBMETAFLAG");
            Log.d("ShellInit","patchVbmeta = " + Config.patchVbmeta);
            IOUtils.close(bashrc);
            return true;
        } catch (IOException e) {
            Log.e("ShellInit","onInit Error = " + e.getMessage());
            return false;
        }
    }

}
