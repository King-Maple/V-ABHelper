// IUpdateService.aidl
package me.yowal.updatehelper.service;

import me.yowal.updatehelper.interfaces.IUpdateCallback;
import me.yowal.updatehelper.bean.UpdateInfo;

interface IUpdateService {

    UpdateInfo parseZip(String file);

    boolean isValid();

    boolean startUpdateSystem(in UpdateInfo info, IUpdateCallback callback);

    boolean closeAssetFileDescriptor();

    void cancel();

    IBinder getFileSystemService();

    int GetKsuVersion();

    boolean KsuisSafeMode();

    boolean KsuIsLkmMode();

    int findValidateSourceHash();

    void passValidateSourceHash(String lib, int offset);
}