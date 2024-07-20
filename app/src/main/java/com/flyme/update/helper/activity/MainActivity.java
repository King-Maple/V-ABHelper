package com.flyme.update.helper.activity;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.flyme.update.helper.BuildConfig;
import com.flyme.update.helper.R;
import com.flyme.update.helper.fragment.AboutFragment;
import com.flyme.update.helper.fragment.HomeFragment;
import com.flyme.update.helper.service.IUpdateService;
import com.flyme.update.helper.service.UpdateService;
import com.flyme.update.helper.utils.UpdateServiceManager;
import com.flyme.update.helper.widget.NavigationBar;
import com.kongzue.dialogx.dialogs.TipDialog;
import com.kongzue.dialogx.dialogs.WaitDialog;
import com.topjohnwu.superuser.Shell;
import com.topjohnwu.superuser.ShellUtils;
import com.topjohnwu.superuser.ipc.RootService;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends BaseActivity {

    private final List<Fragment> fragments = new ArrayList<>();
    private static final int MSG_WHAT_REQUEST_ROOT_PERMISSION = 0;
    private static final int MSG_WHAT_NO_ROOT_PERMISSON = 1;

    private Shell SHELL;


    @SuppressLint("HandlerLeak")
    private final Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            if (msg.what == MSG_WHAT_REQUEST_ROOT_PERMISSION) {
                try {
                    openService();
                } catch (IOException e) {
                    TipDialog.show(MainActivity.this,"启动服务失败!", WaitDialog.TYPE.ERROR);
                }
            } else if (msg.what == MSG_WHAT_NO_ROOT_PERMISSON) {
                TipDialog.show(MainActivity.this,"请授予超级权限后使用!", WaitDialog.TYPE.ERROR);
            }
        }
    };

    @SuppressLint("WrongConstant")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setDoubleTouchClose(true);
        fragments.add(new HomeFragment());
        fragments.add(new AboutFragment());
        replaceFragment(fragments.get(0));
        NavigationBar navigationBar = findViewById(R.id.main_navigation);
        navigationBar.bindData(new String[]{ "开始", "关于" }, new int[] { R.mipmap.ic_home, R.mipmap.ic_settings });
        navigationBar.setPositionListener((view, position) -> replaceFragment(fragments.get(position)));
        WaitDialog.show(MainActivity.this,"正在检测超级权限...");
        getASynHandler().postDelayed(() -> {
            SHELL = createRootShell();
            if (!Boolean.TRUE.equals(SHELL.isAppGrantedRoot())) {
                mHandler.sendEmptyMessage(MSG_WHAT_NO_ROOT_PERMISSON);
            } else {
                try {
                    ShellUtils.fastCmd("rm -r " + getFilesDir().toString());
                    ShellUtils.fastCmd("mkdir " + getFilesDir().toString());
                    FileUtils.copyToFile(getAssets().open("magiskboot"), new File(getFilesDir(),"magiskboot"));
                    FileUtils.copyToFile(getResources().openRawResource(R.raw.apatch), new File(getFilesDir(),"apatch.sh"));
                    ShellUtils.fastCmd("chmod -R 777 " + getFilesDir().toString());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                mHandler.sendEmptyMessage(MSG_WHAT_REQUEST_ROOT_PERMISSION);
            }
        },2000);
    }


    private void replaceFragment(Fragment fragment){
        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.setCustomAnimations(R.animator.fragment_slide_show, R.animator.fragment_slide_hide);
        fragmentTransaction.replace(R.id.main_frame_layout, fragment);
        fragmentTransaction.commit();
    }

    private static Shell createRootShell() {
        Shell.enableVerboseLogging = BuildConfig.DEBUG;
        Shell.Builder builder = Shell.Builder.create();
        try {
            return builder.build("/system/bin/su");
        } catch (Throwable e) {
            e.printStackTrace();
            return builder.build("sh");
        }
    }

    private void openService() throws IOException {
        WaitDialog.show(MainActivity.this,"正常启动服务...");
        Intent intent = new Intent(this, UpdateService.class);
        Shell.Task task = UpdateService.bindOrTask(intent, Shell.EXECUTOR, new RootConnect());
        if (task != null)
            SHELL.execTask(task);
    }

    class RootConnect implements ServiceConnection {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            IUpdateService mUpdateService = IUpdateService.Stub.asInterface(service);
            if (mUpdateService != null) {
                uUpdateServiceManager = new UpdateServiceManager(mUpdateService);
                WaitDialog.dismiss(MainActivity.this);
            } else {
                TipDialog.show(MainActivity.this,"启动服务失败!", WaitDialog.TYPE.ERROR);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    }




}
