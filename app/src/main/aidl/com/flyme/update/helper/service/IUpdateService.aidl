// IUpdateService.aidl
package com.flyme.update.helper.service;

import com.flyme.update.helper.interfaces.IUpdateCallback;
import com.flyme.update.helper.bean.UpdateInfo;

interface IUpdateService {

    boolean isValid();

    boolean startUpdateSystem(in UpdateInfo info, IUpdateCallback callback);

    boolean closeAssetFileDescriptor();

    void cancel();

    IBinder getFileSystemService();

    int GetKsuVersion();

    boolean KsuisSafeMode();

    boolean KsuIsLkmMode();
}