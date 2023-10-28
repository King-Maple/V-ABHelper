package com.flyme.update.helper.widget;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.view.HapticFeedbackConstants;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

/**
 * 实现Android组件点击事件、长按事件触发时的视觉与触感效果
 */
public class TouchFeedback {

    private float scaling = 0.9f;
    private long duration = 150L;
    private final Handler longTouchHandler;
    private final Runnable longTouchRunnable;
    private static final int FEEDBACK_CANCEL = 0;
    private static final int FEEDBACK_CLICK = 1;
    private static final int FEEDBACK_LONG_CLICK = 2;
    /**
     * 私有构造函数
     */
    private TouchFeedback(Context context){
        this.longTouchHandler = new Handler(context.getMainLooper());
        this.longTouchRunnable = ()->{
            this.isLongFeed = true;
        };
    }

    public float getScaling() {
        return scaling;
    }

    public void setScaling(float scaling) {
        this.scaling = scaling;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    /**
     * 单例模式
     */
    private static TouchFeedback touchFeedback;
    public static TouchFeedback newInstance(Context context){
        if (touchFeedback == null) {
            synchronized (TouchFeedback.class) {
                if (touchFeedback == null) {
                    touchFeedback = new TouchFeedback(context);
                }
            }
        }
        return touchFeedback;
    }

    private void vibrationPress(View view){
        int flag = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
                ? HapticFeedbackConstants.GESTURE_START : Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1
                ? HapticFeedbackConstants.KEYBOARD_PRESS : HapticFeedbackConstants.VIRTUAL_KEY;
        view.performHapticFeedback(flag);
    }

    private void vibrationUp(View view){
        int flag = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R
                ? HapticFeedbackConstants.GESTURE_END : Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1
                ? HapticFeedbackConstants.KEYBOARD_RELEASE : HapticFeedbackConstants.VIRTUAL_KEY;
        view.performHapticFeedback(flag);
    }

    private void viewPress(View view){
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(view, "scaleX", scaling);
        scaleX.setDuration(duration);
        scaleX.setInterpolator(new DecelerateInterpolator());
        scaleX.start();

        ObjectAnimator scaleY = ObjectAnimator.ofFloat(view, "scaleY", scaling);
        scaleY.setDuration(duration);
        scaleY.setInterpolator(new DecelerateInterpolator());
        scaleY.start();

        vibrationPress(view);
    }

    public boolean isUp = false;
    private void viewUp(OnFeedBackListener onFeedBackListener, View view, int type){
        ObjectAnimator scaleX = ObjectAnimator.ofFloat(view, "scaleX", 1);
        scaleX.setDuration((long) (duration * 0.9));
        scaleX.setInterpolator(new DecelerateInterpolator());
        scaleX.start();

        ObjectAnimator scaleY = ObjectAnimator.ofFloat(view, "scaleY", 1);
        scaleY.setDuration((long) (duration * 0.9));
        scaleY.setInterpolator(new DecelerateInterpolator());
        scaleY.start();

        vibrationUp(view);
        if (!isUp) isUp = true;

        scaleY.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                if (isUp){
                    if (type == FEEDBACK_CLICK){
                        onFeedBackListener.onClick(view);
                    }else if (type == FEEDBACK_LONG_CLICK){
                        onFeedBackListener.onLongClick(view);
                    }
                }
                isUp = false;
            }
        });
    }

    public interface OnFeedBackListener{
        void onClick(View view);
        void onLongClick(View view);
    }

    @SuppressLint("ClickableViewAccessibility")
    public void setOnFeedBackListener(OnFeedBackListener onFeedBackListener, View view){
        view.setOnTouchListener((v, event) -> {
            int action = event.getAction();
            switch (action){
                case MotionEvent.ACTION_DOWN:
                    viewPress(v);
                    break;
                case MotionEvent.ACTION_CANCEL:
                    viewUp(onFeedBackListener, v, FEEDBACK_CANCEL);
                    break;
                case MotionEvent.ACTION_UP:
                    viewUp(onFeedBackListener, v, FEEDBACK_CLICK);
                    break;
            }
            return true;
        });
    }

    private boolean isLongFeed = false;
    @SuppressLint("ClickableViewAccessibility")
    public void setOnFeedBackListener(OnFeedBackListener onFeedBackListener, View view, boolean isLongFeed){
        if (isLongFeed){
            view.setOnTouchListener((v, event) -> {
                int action = event.getAction();
                switch (action){
                    case MotionEvent.ACTION_DOWN:
                        viewPress(v);
                        this.isLongFeed = false;
                        longTouchHandler.removeCallbacks(longTouchRunnable);
                        longTouchHandler.postDelayed(longTouchRunnable, 200);
                        break;
                    case MotionEvent.ACTION_CANCEL:
                        viewUp(onFeedBackListener, v, FEEDBACK_CANCEL);
                        break;
                    case MotionEvent.ACTION_UP:
                        viewUp(onFeedBackListener, v, this.isLongFeed ? FEEDBACK_LONG_CLICK : FEEDBACK_CLICK);
                        break;
                }
                return false;
            });
        }else {
            setOnFeedBackListener(onFeedBackListener, view);
        }
    }
}

