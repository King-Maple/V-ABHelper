package me.yowal.updatehelper.utils;

import android.content.Context;
import android.content.DialogInterface;
import android.text.method.PasswordTransformationMethod;
import android.view.LayoutInflater;
import android.view.inputmethod.EditorInfo;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import me.yowal.updatehelper.databinding.DialogInputBinding;

public class InputDialogUtils {
    public static void show(Context context, String title, String message, OnInputClickListener callback) {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context).setTitle(title);
        DialogInputBinding bindingiput = DialogInputBinding.inflate(LayoutInflater.from(context), null, false);
        bindingiput.mEditText.setTransformationMethod(PasswordTransformationMethod.getInstance());
        builder.setMessage(message);
        builder.setCancelable(false);
        builder.setView(bindingiput.getRoot());
        builder.setPositiveButton("确定", (dialog, which) -> {
            if (callback.onClick(dialog, bindingiput, false))  {
                ReflectionUtils.on(dialog.getClass()).field("mShowing").setValue(dialog, false);
            } else {
                ReflectionUtils.on(dialog.getClass()).field("mShowing").setValue(dialog, true);
            }
        });
        builder.setNegativeButton("取消", (dialog, which) -> {
            if (callback.onClick(dialog, bindingiput, true))  {
                ReflectionUtils.on(dialog.getClass()).field("mShowing").setValue(dialog, false);
            } else {
                ReflectionUtils.on(dialog.getClass()).field("mShowing").setValue(dialog, true);
            }
        });
        builder.create().show();
    }

    public interface OnInputClickListener {
        boolean onClick(DialogInterface dialog, DialogInputBinding bindingiput, boolean isCancel);

    }
}
