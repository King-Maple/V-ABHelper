package com.flyme.update.helper.activity;

import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.flyme.update.helper.R;
import com.flyme.update.helper.interfaces.OnNavigationStateListener;
import com.flyme.update.helper.manager.ActivityManger;
import com.flyme.update.helper.widget.TouchFeedback;
import com.github.mmin18.widget.RealtimeBlurView;
import com.gyf.immersionbar.ImmersionBar;
import com.kongzue.dialogx.dialogs.CustomDialog;
import com.kongzue.dialogx.dialogs.PopTip;
import com.kongzue.dialogx.interfaces.OnBindView;

import java.util.ArrayList;

public abstract class BaseActivity extends AppCompatActivity{

    @SuppressLint("StaticFieldLeak")
    public static Activity mContext;

    public ArrayList<View> feedViews = new ArrayList<>();

    private TouchFeedback touchFeedback;

    private Handler mainHandler;

    private Handler aSynHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = this;
        touchFeedback = TouchFeedback.newInstance(this);
        mainHandler = new Handler(Looper.getMainLooper());
        ActivityManger.addActivity(this);

    }

    @Override
    protected void onStart() {
        super.onStart();
        ImmersionBar.with(this)
                .transparentStatusBar()
                .statusBarDarkFont(true)
                .init();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (null != mainHandler) {
            mainHandler.removeCallbacksAndMessages(null);
        }
        if (null != aSynHandler) {
            aSynHandler.removeCallbacksAndMessages(null);
        }
        ActivityManger.removeActivity(this);
    }

    public boolean isNightMode(){
        return (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
    }

    public Context getApp() {
        return getApplication();
    }


    @Override
    public void onBackPressed() {
        if (isOpenDoubleTouchClose){
            if (System.currentTimeMillis() - time > 2000) {
                PopTip.build().setMessage("再按一次退出软件").iconWarning().show();
                time = System.currentTimeMillis();
            } else {
                finish();
                super.onBackPressed();
            }
        }else {
            finish();
            super.onBackPressed();
        }
    }

    private boolean isOpenDoubleTouchClose = false;
    private long time;

    public void setDoubleTouchClose(boolean isOpenDoubleTouchClose){
        this.isOpenDoubleTouchClose = isOpenDoubleTouchClose;
    }

    private void isNavigationBarExist(Activity activity, final OnNavigationStateListener onNavigationStateListener) {
        if (activity == null) {
            return;
        }
        final int height = getNavigationHeight(activity);
        activity.getWindow().getDecorView().setOnApplyWindowInsetsListener((v, windowInsets) -> {
            boolean isShowing = false;
            int b = 0;
            if (windowInsets != null) {
                b = windowInsets.getSystemWindowInsetBottom();
                isShowing = (b == height);
            }
            if (onNavigationStateListener != null && b <= height) {
                onNavigationStateListener.onNavigationState(isShowing, b);
            }
            return windowInsets;
        });
    }

    private int getNavigationHeight(Context activity) {
        if (activity == null) {
            return 0;
        }
        Resources resources = activity.getResources();
        int resourceId = resources.getIdentifier("navigation_bar_height", "dimen", "android");
        int height = 0;
        if (resourceId > 0) {
            //获取NavigationBar的高度
            height = resources.getDimensionPixelSize(resourceId);
        }
        return height;
    }

    public Handler getASynHandler() {
        if (null == aSynHandler) {
            HandlerThread handlerThread = new HandlerThread("FlymeUpdate");
            handlerThread.start();
            aSynHandler = new Handler(handlerThread.getLooper());
        }
        return aSynHandler;
    }

    public Handler getMainHandler() {
        return mainHandler;
    }

    public void showNotificationPermissinDialog() {
        CustomDialog.build()
                .setAutoUnsafePlacePadding(false)
                .setCancelable(false)
                .setCustomView(new OnBindView<CustomDialog>(R.layout.dialog_full_base) {
                    @SuppressLint("SetTextI18n")
                    @Override
                    public void onBind(CustomDialog dialog, View v) {
                        LinearLayout relativeLayout = v.findViewById(R.id.root_view);
                        View inflate = View.inflate(mContext, R.layout.dialog_permissin_write, null);
                        relativeLayout.removeAllViews();
                        relativeLayout.addView(inflate);

                        RealtimeBlurView blurView = v.findViewById(R.id.blur_view);
                        translation(blurView);

                        inflate.findViewById(R.id.cancel_button).setOnClickListener((view)->{
                            dialog.dismiss();
                        });

                        inflate.findViewById(R.id.confirm_button).setOnClickListener((view)->{
                            Intent intent = new Intent();
                            intent.setAction("android.settings.APP_NOTIFICATION_SETTINGS");
                            intent.putExtra("android.provider.extra.APP_PACKAGE", getPackageName());
                            startActivity(intent);
                            dialog.dismiss();
                        });

                        inflate.<ImageView>findViewById(R.id.icon).setImageResource(R.mipmap.ic_notification);
                        inflate.<ImageView>findViewById(R.id.sub_icon).setImageResource(R.mipmap.ic_sub_notification);
                        inflate.<TextView>findViewById(R.id.name).setText("通知权限申请");
                        inflate.<TextView>findViewById(R.id.content).setText(getString(R.string.app_name) + "在刷机过程中会在通知栏发送刷机进度通知，但这需要系统已允许" + getString(R.string.app_name) + "发送通知。");
                        inflate.<TextView>findViewById(R.id.sub_content).setText("用于" + getString(R.string.app_name) + "的发送刷机进度功能");
                        inflate.<TextView>findViewById(R.id.sub_tips).setText("授予该权限后，软件可以在通知栏发送通知");
                    }
                })
                .show();
    }

    private void translation(View view){
        ObjectAnimator translationY = ObjectAnimator.ofFloat(view, "translationY", 0, 0.1f);
        translationY.setDuration(100);
        translationY.start();
        getMainHandler().postDelayed(()->{
            translation(view);
        }, 200);
    }
}
