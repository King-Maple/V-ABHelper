package me.yowal.updatehelper.utils;

import android.app.Notification;
import android.content.Context;

import androidx.core.app.NotificationCompat;

import me.yowal.updatehelper.R;

public class NotificationUtils {

    public static Notification notifyMsg(Context context, String ContentText, String ContentTitle) {
        return new NotificationCompat.Builder(context, "new_version_push")
                .setContentText(ContentText)
                .setContentTitle(ContentTitle)
                .setAutoCancel(true)
                .setPriority(2)
                .setShowWhen(false)
                .setSmallIcon(R.mipmap.ic_launcher)
                .build();
    }

    public static Notification notifyProgress(Context context, String ContentText, String ContentTitle, int maxprogress, int progress, boolean indeterminate) {
        return  new NotificationCompat.Builder(context,"new_version_push")
                .setContentText(ContentText)
                .setContentTitle(ContentTitle)
                .setAutoCancel(false)
                .setOngoing(true)
                .setPriority(2)
                .setShowWhen(false)
                .setProgress(maxprogress, progress,indeterminate)
                .setSmallIcon(R.mipmap.ic_launcher)
                .build();
    }
}
