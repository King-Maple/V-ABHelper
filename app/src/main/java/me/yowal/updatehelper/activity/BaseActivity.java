package me.yowal.updatehelper.activity;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.ReturnThis;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LifecycleOwner;

import com.gyf.immersionbar.ImmersionBar;

import me.yowal.updatehelper.R;

public class BaseActivity extends AppCompatActivity {

    protected String TAG = getClass().getSimpleName();

    private Handler aSynHandler;

    private Handler mainHandler;

    public Context aContext;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        aContext = this;

        mainHandler = new Handler(Looper.getMainLooper());

        ImmersionBar.with(this)
                .fitsSystemWindows(true)
                .statusBarColor(R.color.md_theme_background)
                .navigationBarColor(R.color.md_theme_background)
                .statusBarDarkFont(true)
                .navigationBarDarkIcon(true)
                .init();
    }


    public void showDialog(AlertDialog dialog) {
        mainHandler.post(dialog::show);
    }



    public void toast(String message) {
        mainHandler.post(() -> Toast.makeText(aContext, message, Toast.LENGTH_SHORT).show());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (null != aSynHandler) {
            aSynHandler.removeCallbacksAndMessages(null);
        }
        if (null != mainHandler) {
            mainHandler.removeCallbacksAndMessages(null);
        }
    }

    public Handler getMainHandler() {
        return mainHandler;
    }

    public Handler getASynHandler() {
        if (null == aSynHandler) {
            HandlerThread handlerThread = new HandlerThread(TAG);
            handlerThread.start();
            aSynHandler = new Handler(handlerThread.getLooper());
        }
        return aSynHandler;
    }

    public LifecycleOwner getLifeOwner() {
        return this;
    }

}

