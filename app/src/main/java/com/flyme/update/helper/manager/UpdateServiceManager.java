package com.flyme.update.helper.manager;

import android.os.IBinder;
import android.os.RemoteException;

import com.flyme.update.helper.bean.UpdateInfo;
import com.flyme.update.helper.interfaces.IUpdateCallback;
import com.flyme.update.helper.service.IUpdateService;
import com.topjohnwu.superuser.nio.FileSystemManager;

public class UpdateServiceManager {

    private static volatile UpdateServiceManager instance;
    private IUpdateService uNativeService = null;

    public static synchronized UpdateServiceManager getInstance() {
        if (instance == null) {
            synchronized (UpdateServiceManager.class) {
                if (instance == null) {
                    instance = new UpdateServiceManager();
                }
            }
        }
        return instance;
    }

    public void init(IUpdateService service) {
        uNativeService = service;
    }

    public IUpdateService getService() {
        return uNativeService;
    }

    public boolean isDowngradeSupported() {
        try {
            return this.uNativeService.isDowngradeSupported();
        } catch (RemoteException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean startUpdateSystem(UpdateInfo info, IUpdateCallback listener, boolean allowDowngrade) {
        try {
            return this.uNativeService.startUpdateSystem(info, listener, allowDowngrade);
        } catch (RemoteException e) {
            e.printStackTrace();
            return false;
        }
    }

    public UpdateInfo parseUpdatePackage(String path) {
        try {
            return this.uNativeService.parseUpdatePackage(path);
        } catch (RemoteException e) {
            e.printStackTrace();
            return null;
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

    public int GetKsuVersion() {
        try {
            return this.uNativeService.GetKsuVersion();
        } catch (RemoteException e) {
            return -1;
        }
    }

    public boolean KsuIsLkmMode() {
        try {
            return this.uNativeService.KsuIsLkmMode();
        } catch (RemoteException e) {
            return false;
        }
    }
}
