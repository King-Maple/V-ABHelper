package me.yowal.updatehelper.manager;

import android.os.IBinder;
import android.os.RemoteException;

import com.topjohnwu.superuser.nio.FileSystemManager;

import me.yowal.updatehelper.bean.UpdateInfo;
import me.yowal.updatehelper.interfaces.IUpdateCallback;
import me.yowal.updatehelper.service.IUpdateService;

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

    public UpdateInfo pareZip(String file) {
        try {
            return this.uNativeService.parseZip(file);
        } catch (RemoteException e) {
            return null;
        }
    }

    public boolean passValidateSourceHash(String lib, int offset) {
        try {
            this.uNativeService.passValidateSourceHash(lib, offset);
            return true;
        } catch (RemoteException e) {
            e.printStackTrace();
            return false;
        }
    }

    public int findValidateSourceHash() {
        try {
            return this.uNativeService.findValidateSourceHash();
        } catch (RemoteException e) {
            e.printStackTrace();
            return -1;
        }
    }

    public boolean isOtaZip(byte[] metadata_pb_data) {
        try {
            return this.uNativeService.isOtaZip(metadata_pb_data);
        } catch (RemoteException e) {
            e.printStackTrace();
            return false;
        }
    }

    public String GetAPKInstallPath(String pkg) {
        try {
            return this.uNativeService.GetAPKInstallPath(pkg);
        } catch (RemoteException e) {
            e.printStackTrace();
            return "";
        }
    }
}
