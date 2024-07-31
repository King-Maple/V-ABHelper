package me.yowal.updatehelper.service;

import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.net.Uri;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.os.Process;
import android.os.RemoteException;
import android.os.UpdateEngineCallback;

import androidx.annotation.NonNull;

import com.topjohnwu.superuser.ipc.RootService;
import com.topjohnwu.superuser.nio.FileSystemManager;

import java.io.File;
import java.io.IOException;

import me.yowal.updatehelper.Natives;
import me.yowal.updatehelper.bean.UpdateInfo;
import me.yowal.updatehelper.interfaces.IUpdateCallback;
import me.yowal.updatehelper.proxy.UpdateEngineProxy;
import me.yowal.updatehelper.utils.LogUtils;
import me.yowal.updatehelper.utils.UpdateUtils;

public class UpdateService extends RootService {
    static {
        if (Process.myUid() == 0)
            System.loadLibrary("kernelsu");
    }

    private static final String TAG = "UpdateService";


    @Override
    public IBinder onBind(@NonNull Intent intent) {
        return new UpdateServiceIPC();
    }

    static class UpdateServiceIPC extends IUpdateService.Stub {
        private final UpdateEngineProxy mUpdateEngine = new UpdateEngineProxy();
        private AssetFileDescriptor mAssetFileDescriptor;

        @Override
        public boolean startUpdateSystem(UpdateInfo info, IUpdateCallback listener) {
            try {
                if (info.getHeaderKeyValuePairs() == null || info.getHeaderKeyValuePairs().length == 0)
                    return false;
                boolean bind_status = mUpdateEngine.bind(new UpdateEngineCallback() {
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
                if (!bind_status)
                    return false;
                //mUpdateEngine.resetStatus();
                if (info.getUrl().startsWith("file:///data/ota_package")) {
                    mUpdateEngine.applyPayload(info.getUrl(), info.getOffset(), info.getSize(), info.getHeaderKeyValuePairs());
                    return true;
                }
                Uri uriFile = Uri.parse(info.getUrl());
                if (uriFile.getPath() == null)
                    return false;
                mAssetFileDescriptor = new AssetFileDescriptor(ParcelFileDescriptor.open(new File(uriFile.getPath()), ParcelFileDescriptor.parseMode("r")), info.getOffset(), info.getSize());
                if (!mAssetFileDescriptor.getFileDescriptor().valid())
                    return false;
                mUpdateEngine.applyPayload(mAssetFileDescriptor, info.getHeaderKeyValuePairs());
                return true;
            } catch (Exception e) {
                String ExceptionString = e.getLocalizedMessage();
                LogUtils.e(TAG, ExceptionString);
                return ExceptionString.contains("Already processing an update") || ExceptionString.contains("waiting for reboot");
            }
        }

        @Override
        public void cancel() {
            this.mUpdateEngine.cancel();
        }


        @Override
        public boolean isValid() {
            return this.mUpdateEngine.isValid();
        }

        @Override
        public boolean closeAssetFileDescriptor() {
            if (mAssetFileDescriptor != null) {
                try {
                    mAssetFileDescriptor.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return true;
        }

        @Override
        public IBinder getFileSystemService() {
            return FileSystemManager.getService();
        }

        @Override
        public int GetKsuVersion() {
            return Natives.getVersion();
        }

        @Override
        public boolean KsuisSafeMode() {
            return Natives.isSafeMode();
        }

        @Override
        public boolean KsuIsLkmMode() {
            return Natives.isLkmMode();
        }

        @Override
        public int findValidateSourceHash() {
            return Natives.findValidateSourceHash();
        }

        @Override
        public void passValidateSourceHash(String lib, int offset)  {
            Natives.passValidateSourceHash(lib, offset);
        }

        @Override
        public UpdateInfo parseZip(String file)  {
            return UpdateUtils.parse(file);
        }
    }
}