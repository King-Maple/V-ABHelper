// IUpdateService.aidl
package com.flyme.update.helper.service;

import com.flyme.update.helper.interfaces.IUpdateCallback;
import android.os.PersistableBundle;
import com.flyme.update.helper.utils.UpdateInfo;

interface IUpdateService {

    boolean startUpdateSystem(in UpdateInfo info, IUpdateCallback callback);

    void cancel();

    IBinder getFileSystemService();

    void updateSystemUpdateInfo(in PersistableBundle info);

    int GetKsuVersion();

    boolean isSafeMode();
}