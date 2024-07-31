package me.yowal.updatehelper.interfaces;

public interface UpdateCallback {
    void onPayloadApplicationComplete(int errorCode);

    void onStatusUpdate(int status, float percent);
}

