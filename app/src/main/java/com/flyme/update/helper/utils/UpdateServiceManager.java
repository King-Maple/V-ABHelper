package com.flyme.update.helper.utils;

import android.os.IBinder;
import android.os.RemoteException;

import com.flyme.update.helper.interfaces.IUpdateCallback;
import com.flyme.update.helper.service.IUpdateService;
import com.topjohnwu.superuser.nio.FileSystemManager;

public class UpdateServiceManager {
    private IUpdateService uNativeService = null;

    public UpdateServiceManager(IUpdateService ipc) {
        this.uNativeService = ipc;
    }


    public boolean startUpdateSystem(UpdateInfo info, IUpdateCallback listener) {
        try {
            return this.uNativeService.startUpdateSystem(info, listener);
        } catch (RemoteException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean isValid() {
        try {
            return this.uNativeService.isValid();
        } catch (RemoteException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean cancel() {
        try {
            this.uNativeService.cancel();
            return true;
        } catch (RemoteException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean closeAssetFileDescriptor() {
        try {
            this.uNativeService.closeAssetFileDescriptor();
            return true;
        } catch (RemoteException e) {
            e.printStackTrace();
            return false;
        }
    }

    public FileSystemManager getFileSystemManager() {
        try {
            IBinder binder = this.uNativeService.getFileSystemService();
            return FileSystemManager.getRemote(binder);
        } catch (RemoteException e) {
            return null;
        }
    }
}
