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

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

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
    private Intent RootIntent;
    private static final int MSG_WHAT_REQUEST_ROOT_PERMISSION = 0;
    private static final int MSG_WHAT_NO_ROOT_PERMISSON = 1;


    @SuppressLint("HandlerLeak")
    private final Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            if (msg.what == MSG_WHAT_REQUEST_ROOT_PERMISSION) {
                openService();
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
        fragments.add(HomeFragment.newInstance());
        fragments.add(AboutFragment.newInstance());
        replaceFragment(fragments.get(0));
        NavigationBar navigationBar = findViewById(R.id.main_navigation);
        navigationBar.bindData(new String[]{ "开始", "关于" }, new int[] { R.mipmap.ic_home, R.mipmap.ic_settings });
        navigationBar.setPositionListener((view, position) -> replaceFragment(fragments.get(position)));
        WaitDialog.show(MainActivity.this,"正在检测超级权限...");
        getASynHandler().postDelayed(() -> {
            if (Boolean.FALSE.equals(Shell.isAppGrantedRoot())) {
                mHandler.sendEmptyMessage(MSG_WHAT_NO_ROOT_PERMISSON);
            } else {
                try {
                    ShellUtils.fastCmd("rm -r " + getFilesDir().toString());
                    ShellUtils.fastCmd("mkdir " + getFilesDir().toString());
                    FileUtils.copyToFile(getAssets().open("M2391"), new File(getFilesDir(),"M2391.img"));
                    FileUtils.copyToFile(getAssets().open("M2381"), new File(getFilesDir(),"M2381.img"));
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


    private void openService() {
        WaitDialog.show(MainActivity.this,"正常启动服务...");
        RootIntent = new Intent(this, UpdateService.class);
        RootService.bind(RootIntent, new RootConnect());
    }

    class RootConnect implements ServiceConnection {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            IUpdateService mUpdateService = IUpdateService.Stub.asInterface(service);
            if (mUpdateService != null) {
                uUpdateServiceManager = new UpdateServiceManager(mUpdateService);
                WaitDialog.dismiss(MainActivity.this);
                //TipDialog.show(MainActivity.this,"启动服务完成!", WaitDialog.TYPE.SUCCESS);
            } else {
                TipDialog.show(MainActivity.this,"启动服务失败!", WaitDialog.TYPE.SUCCESS);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    }




}
