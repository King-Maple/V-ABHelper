package me.yowal.updatehelper.proxy;

import android.content.res.AssetFileDescriptor;
import android.os.IBinder;
import android.os.IUpdateEngineCallback;
import android.os.UpdateEngine;
import android.os.UpdateEngineCallback;
import android.util.Log;

public class UpdateEngineProxy {
    private static final String TAG = "UpdateEngineProxy";
    private static final String UPDATE_ENGINE_SERVICE = "android.os.UpdateEngineService";
    private UpdateEngine mUpdateEngine;

    public UpdateEngineProxy() {
        IBinder mIBinder = ServiceManagerProxy.getService(UPDATE_ENGINE_SERVICE);
        if (mIBinder == null) {
            Log.e(TAG, "Failed to find update_engine");
            return;
        }
        mUpdateEngine = new UpdateEngine();
    }

    public boolean isValid() {
        return mUpdateEngine != null;
    }


    public boolean bind(UpdateEngineCallback callback) {
        return this.mUpdateEngine.bind(callback);
    }

    public void applyPayload(String url, long payload_offset, long payload_size, String[] headerKeyValuePairs) {
        this.mUpdateEngine.applyPayload(url, payload_offset, payload_size, headerKeyValuePairs);
    }

    public void applyPayload(AssetFileDescriptor fileDescriptor, String[] headerKeyValuePairs) {
        this.mUpdateEngine.applyPayload(fileDescriptor, headerKeyValuePairs);
    }

    public boolean unbind(IUpdateEngineCallback callback) {
        return this.mUpdateEngine.unbind();
    }

    public void cancel() {
        this.mUpdateEngine.cancel();
    }

    public void resetShouldSwitchSlotOnReboot() {
        this.mUpdateEngine.resetShouldSwitchSlotOnReboot();
    }

    public void resetStatus() {
        this.mUpdateEngine.resetStatus();
    }

    public void resume() {
        this.mUpdateEngine.resume();
    }

    public void setShouldSwitchSlotOnReboot(String metadataFilename) {
        this.mUpdateEngine.setShouldSwitchSlotOnReboot(metadataFilename);
    }

    public void suspend() {
        this.mUpdateEngine.suspend();
    }

    public boolean verifyPayloadMetadata(String metadataFilename) {
        return this.mUpdateEngine.verifyPayloadMetadata(metadataFilename);
    }



    public static final class ErrorCodeConstants {
        public static final int DEVICE_CORRUPTED = 61;
        public static final int DOWNLOAD_PAYLOAD_VERIFICATION_ERROR = 12;
        public static final int DOWNLOAD_TRANSFER_ERROR = 9;
        public static final int ERROR = 1;
        public static final int FILESYSTEM_COPIER_ERROR = 4;
        public static final int INSTALL_DEVICE_OPEN_ERROR = 7;
        public static final int KERNEL_DEVICE_OPEN_ERROR = 8;
        public static final int NOT_ENOUGH_SPACE = 60;
        public static final int PAYLOAD_HASH_MISMATCH_ERROR = 10;
        public static final int PAYLOAD_MISMATCHED_TYPE_ERROR = 6;
        public static final int PAYLOAD_SIZE_MISMATCH_ERROR = 11;
        public static final int PAYLOAD_TIMESTAMP_ERROR = 51;
        public static final int POST_INSTALL_RUNNER_ERROR = 5;
        public static final int SUCCESS = 0;
        public static final int UPDATED_BUT_NOT_ACTIVE = 52;

        public ErrorCodeConstants() {
        }
    }

    public static final class UpdateStatusConstants {
        public static final int ATTEMPTING_ROLLBACK = 8;
        public static final int CHECKING_FOR_UPDATE = 1;
        public static final int DISABLED = 9;
        public static final int DOWNLOADING = 3;
        public static final int FINALIZING = 5;
        public static final int IDLE = 0;
        public static final int REPORTING_ERROR_EVENT = 7;
        public static final int UPDATED_NEED_REBOOT = 6;
        public static final int UPDATE_AVAILABLE = 2;
        public static final int VERIFYING = 4;

        public UpdateStatusConstants() {
        }
    }
}
