package com.flyme.update.helper.utils;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.os.Handler;
import android.view.View;

import com.flyme.update.helper.activity.BaseActivity;


public class ColorChangeUtils {

    private final int[] colors;
    private long duration = 2000;
    private long delay = 5000;
    private int index = -1;

    private final Handler mHandler;
    private final Runnable mRunnable;

    public ColorChangeUtils(BaseActivity activity, int[] colors, View mView) {
        this.colors = colors;
        mHandler = activity.getMainHandler();
        mRunnable = new Runnable() {
            @Override
            public void run() {
                @SuppressLint("ObjectAnimatorBinding")
                ObjectAnimator backgroundColor = ObjectAnimator.ofInt(mView, "backgroundColor", getColorTemp(), nextColor());
                backgroundColor.setDuration(duration);
                backgroundColor.setEvaluator(new ArgbEvaluator());
                backgroundColor.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        mHandler.postDelayed(mRunnable, delay);
                    }
                });
                backgroundColor.start();
            }
        };
        ObjectAnimator backgroundColor = ObjectAnimator.ofInt(mView, "backgroundColor", getColorTemp(), nextColor());
        backgroundColor.setDuration(0);
        backgroundColor.start();
    }


    private int nextColor(){
        int i = index + 1;
        if (i >= colors.length) i = 0;
        return colors[i];
    }

    private int getColorTemp(){
        index++;
        if (index >= colors.length) index = 0;
        return colors[index];
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public long getDelay() {
        return delay;
    }

    public void setDelay(long delay) {
        this.delay = delay;
    }

    public void startAnimation(){
        mHandler.postDelayed(mRunnable, delay);
    }

    public void stopAnimation(){
        mHandler.removeCallbacks(mRunnable);
    }
}

