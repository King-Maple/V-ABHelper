package com.flyme.update.helper.service;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.net.Uri;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.os.PersistableBundle;
import android.os.Process;
import android.os.RemoteException;
import android.os.SystemUpdateManager;
import android.os.UpdateEngine;
import android.os.UpdateEngineCallback;

import androidx.annotation.NonNull;

import com.flyme.update.helper.interfaces.IUpdateCallback;
import com.flyme.update.helper.utils.Natives;
import com.flyme.update.helper.utils.UpdateInfo;
import com.topjohnwu.superuser.ipc.RootService;
import com.topjohnwu.superuser.nio.FileSystemManager;

import java.io.File;
import java.io.IOException;

public class UpdateService extends RootService {
    static {
        if (Process.myUid() == 0)
            System.loadLibrary("kernelsu");
    }

    @Override
    public IBinder onBind(@NonNull Intent intent) {
        return new UpdateServiceIPC();
    }

    class UpdateServiceIPC extends IUpdateService.Stub {
        private final UpdateEngine mUpdateEngine = new UpdateEngine();
        private AssetFileDescriptor mAssetFileDescriptor;
        private SystemUpdateManager mSystemUpdateManager;

        @Override
        public boolean startUpdateSystem(UpdateInfo info, IUpdateCallback listener) {
            try {
                if (info.getHeaderKeyValuePairs() == null || info.getHeaderKeyValuePairs().length == 0) {
                    return false;
                }
                mUpdateEngine.bind(new UpdateEngineCallback() {
                    @Override
                    public void onStatusUpdate(int status, float percent) {
                        if (listener != null) {
                            try {
                                listener.onStatusUpdate(status, percent);
                            } catch (RemoteException e) {
                                e.printStackTrace();
                            }
                        }
                    }

                    @Override
                    public void onPayloadApplicationComplete(int errorCode) {
                        if (listener != null) {
                            try {
                                listener.onPayloadApplicationComplete(errorCode);
                            } catch (RemoteException e) {
                                e.printStackTrace();
                            }
                        }
                        if (mAssetFileDescriptor != null) {
                            try {
                                mAssetFileDescriptor.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                });
                //mUpdateEngine.resetStatus();
                if (info.getUrl().startsWith("file:///data/ota_package")) {
                    mUpdateEngine.applyPayload(info.getUrl(), info.getOffset(), info.getSize(), info.getHeaderKeyValuePairs());
                    return true;
                }
                Uri uriFile = Uri.parse(info.getUrl());
                if (uriFile.getPath() == null) {
                    return false;
                }
                mAssetFileDescriptor = new AssetFileDescriptor(ParcelFileDescriptor.open(new File(uriFile.getPath()), ParcelFileDescriptor.parseMode("r")), info.getOffset(), info.getSize());
                if (!mAssetFileDescriptor.getFileDescriptor().valid()) {
                    return false;
                }
                mUpdateEngine.applyPayload(mAssetFileDescriptor, info.getHeaderKeyValuePairs());
                return true;
            } catch (Exception e) {
                String ExceptionString = e.toString();
                return ExceptionString.contains("Already processing an update");
            }
        }

        @Override
        public void cancel() {
            this.mUpdateEngine.cancel();
        }

        @Override
        public IBinder getFileSystemService() {
            return FileSystemManager.getService();
        }

        @SuppressLint("WrongConstant")
        @Override
        public void updateSystemUpdateInfo(PersistableBundle info) {
            if (mSystemUpdateManager == null) {
                mSystemUpdateManager = (SystemUpdateManager)getSystemService("system_update");
            }
            mSystemUpdateManager.updateSystemUpdateInfo(info);
        }

        @Override
        public int GetKsuVersion() {
            return Natives.getVersion();
        }

        @Override
        public boolean isSafeMode() {
            return Natives.isSafeMode();
        }
    }
}
