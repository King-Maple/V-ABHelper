package com.flyme.update.helper.service;

import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.net.Uri;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.os.Process;
import android.os.RemoteException;
import android.os.UpdateEngineCallback;
import androidx.annotation.NonNull;

import com.flyme.update.helper.interfaces.IUpdateCallback;
import com.flyme.update.helper.utils.BootStateUtils;
import com.flyme.update.helper.utils.KernelSuCliUtils;
import com.flyme.update.helper.utils.LogUtils;
import com.flyme.update.helper.utils.Natives;
import com.flyme.update.helper.utils.PayloadProperties;
import com.flyme.update.helper.proxy.UpdateEngineProxy;
import com.flyme.update.helper.bean.UpdateInfo;
import com.flyme.update.helper.utils.UpdateParser;
import com.topjohnwu.superuser.ipc.RootService;
import com.topjohnwu.superuser.nio.FileSystemManager;

import java.io.File;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

public class UpdateService extends RootService {

    static {
        if (Process.myUid() == 0)
            System.loadLibrary("kernelsu");
    }

    static final String TAG = "UpdateService";


    @Override
    public void onCreate() {
        /*Cursor cursor = null;
        try {
            cursor = getApplicationContext().getContentResolver().query(Uri.parse("content://com.flyme.secureservice.open.DataProvider"), null, null, new String[]{"5"}, null);
            if (cursor == null) {
                LogUtils.e(TAG, "ContentResolver = null");
                return;
            }
            if (cursor.getCount() > 0) {
                cursor.moveToFirst();
                int index = cursor.getColumnIndex("value");
                if (index >= 0) {
                    String dsid = cursor.getString(index);
                    LogUtils.d(TAG, "dsid = " + dsid);
                    cursor.close();
                    return;
                }
                LogUtils.e(TAG, "no value in cursor");
            }
            cursor.close();
        } catch (Exception e) {
            e.printStackTrace();
            LogUtils.e(TAG, e.getMessage());
        }*/

    }


    @Override
    public IBinder onBind(@NonNull Intent intent) {
        return new UpdateServiceIPC();
    }

    static class UpdateServiceIPC extends IUpdateService.Stub {
        private static final String RESET_PROP = "/data/adb/ksu/bin/resetprop";
        private static final String KSUD = "/data/adb/ksud";
        private static final String SUPER_BLOCK = "/dev/block/by-name/super";
        private static final String DEBUGGABLE_PROP = "ro.debuggable";
        private static final String ALLOW_DOWNGRADE_PROP = "ro.ota.allow_downgrade";
        private static final String VERIFIED_BOOT_STATE_PROP = "ro.boot.verifiedbootstate";
        private static final String BOOT_CONFIG = "/proc/bootconfig";

        private final UpdateEngineProxy mUpdateEngine = new UpdateEngineProxy();
        private AssetFileDescriptor mAssetFileDescriptor;
        private boolean mDowngradePrepared;
        private String mOriginalDebuggable;
        private String mOriginalAllowDowngrade;
        private String mOriginalVerifiedBootState;

        @Override
        public UpdateInfo parseUpdatePackage(String path) {
            if (path == null || path.isEmpty()) {
                return null;
            }
            return new UpdateParser(path).parse();
        }

        @Override
        public boolean isDowngradeSupported() {
            File resetProp = new File(RESET_PROP);
            File superBlock = new File(SUPER_BLOCK);
            return Process.myUid() == 0
                    && resetProp.isFile()
                    && resetProp.canExecute()
                    && superBlock.exists()
                    && superBlock.canWrite()
                    && isBootloaderActuallyUnlocked();
        }

        @Override
        public boolean startUpdateSystem(UpdateInfo info, IUpdateCallback listener, boolean allowDowngrade) {
            boolean preparedForThisAttempt = false;
            try {
                if (info.getHeaderKeyValuePairs() == null || info.getHeaderKeyValuePairs().length == 0)
                    return false;
                if (allowDowngrade) {
                    long currentTimestamp = readLongProperty("ro.build.date.utc");
                    if (info.getType() != 1
                            || info.getBuildTimestamp() <= 0
                            || currentTimestamp <= 0
                            || info.getBuildTimestamp() >= currentTimestamp
                            || !prepareDowngrade()) {
                        return false;
                    }
                    preparedForThisAttempt = true;
                }
                String[] payloadProperties = allowDowngrade
                        ? PayloadProperties.withDowngrade(info.getHeaderKeyValuePairs())
                        : info.getHeaderKeyValuePairs();
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
                        if (mAssetFileDescriptor != null) {
                            try {
                                mAssetFileDescriptor.close();
                            } catch (IOException e) {
                                LogUtils.e(TAG, e.getLocalizedMessage() != null
                                        ? e.getLocalizedMessage() : e.getClass().getName());
                            } finally {
                                mAssetFileDescriptor = null;
                            }
                        }
                        // Restore temporary properties before crossing the Binder boundary.
                        // 在跨 Binder 回调前恢复临时属性，避免客户端异常导致清理被跳过。
                        restoreDowngradeProperties();
                        if (listener != null) {
                            try {
                                listener.onPayloadApplicationComplete(errorCode);
                            } catch (RemoteException | RuntimeException e) {
                                LogUtils.e(TAG, e.getLocalizedMessage() != null
                                        ? e.getLocalizedMessage() : e.getClass().getName());
                            }
                        }
                    }
                });
                if (!bind_status) {
                    restoreDowngradeProperties();
                    return false;
                }
                //mUpdateEngine.resetStatus();
                if (info.getUrl().startsWith("file:///data/ota_package")) {
                    mUpdateEngine.applyPayload(info.getUrl(), info.getOffset(), info.getSize(), payloadProperties);
                    return true;
                }
                Uri uriFile = Uri.parse(info.getUrl());
                if (uriFile.getPath() == null) {
                    restoreDowngradeProperties();
                    return false;
                }
                mAssetFileDescriptor = new AssetFileDescriptor(ParcelFileDescriptor.open(new File(uriFile.getPath()), ParcelFileDescriptor.parseMode("r")), info.getOffset(), info.getSize());
                if (!mAssetFileDescriptor.getFileDescriptor().valid()) {
                    restoreDowngradeProperties();
                    return false;
                }
                mUpdateEngine.applyPayload(mAssetFileDescriptor, payloadProperties);
                return true;
            } catch (Exception e) {
                if (preparedForThisAttempt) {
                    restoreDowngradeProperties();
                }
                String exceptionString = e.getLocalizedMessage();
                LogUtils.e(TAG, exceptionString != null ? exceptionString : e.getClass().getName());
                return exceptionString != null
                        && (exceptionString.contains("Already processing an update")
                        || exceptionString.contains("waiting for reboot"));
            }
        }

        @Override
        public void cancel() {
            try {
                this.mUpdateEngine.cancel();
            } catch (RuntimeException e) {
                LogUtils.e(TAG, e.getLocalizedMessage() != null
                        ? e.getLocalizedMessage() : e.getClass().getName());
            } finally {
                restoreDowngradeProperties();
            }
        }

        private synchronized boolean prepareDowngrade() {
            if (mDowngradePrepared) {
                return true;
            }
            if (!isDowngradeSupported()) {
                return false;
            }
            mOriginalDebuggable = readProperty(DEBUGGABLE_PROP);
            mOriginalAllowDowngrade = readProperty(ALLOW_DOWNGRADE_PROP);
            mOriginalVerifiedBootState = readProperty(VERIFIED_BOOT_STATE_PROP);
            if (!writeProperty(DEBUGGABLE_PROP, "1")
                    || !writeProperty(ALLOW_DOWNGRADE_PROP, "1")
                    || !writeProperty(VERIFIED_BOOT_STATE_PROP, "orange")) {
                restoreProperty(VERIFIED_BOOT_STATE_PROP, mOriginalVerifiedBootState);
                restoreProperty(DEBUGGABLE_PROP, mOriginalDebuggable);
                restoreProperty(ALLOW_DOWNGRADE_PROP, mOriginalAllowDowngrade);
                return false;
            }
            mDowngradePrepared = true;
            LogUtils.i(TAG, "Temporary update-engine downgrade properties enabled");
            return true;
        }

        private synchronized void restoreDowngradeProperties() {
            if (!mDowngradePrepared) {
                return;
            }
            restoreProperty(VERIFIED_BOOT_STATE_PROP, mOriginalVerifiedBootState);
            restoreProperty(DEBUGGABLE_PROP, mOriginalDebuggable);
            restoreProperty(ALLOW_DOWNGRADE_PROP, mOriginalAllowDowngrade);
            mDowngradePrepared = false;
            LogUtils.i(TAG, "Temporary update-engine downgrade properties restored");
        }

        private static boolean isBootloaderActuallyUnlocked() {
            try (BufferedReader reader = new BufferedReader(new FileReader(BOOT_CONFIG))) {
                StringBuilder bootConfig = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    bootConfig.append(line).append('\n');
                }
                return BootStateUtils.isActuallyUnlocked(bootConfig.toString());
            } catch (IOException e) {
                LogUtils.e(TAG, e.getLocalizedMessage() != null
                        ? e.getLocalizedMessage() : e.getClass().getName());
                return false;
            }
        }

        private static long readLongProperty(String name) {
            try {
                return Long.parseLong(readProperty(name));
            } catch (NumberFormatException ignored) {
                return 0;
            }
        }

        private static String readProperty(String name) {
            CommandResult result = runResetProp(name);
            return result.success ? result.output.trim() : "";
        }

        private static boolean writeProperty(String name, String value) {
            CommandResult result = runResetProp(name, value);
            return result.success && value.equals(readProperty(name));
        }

        private static void restoreProperty(String name, String originalValue) {
            if (originalValue == null || originalValue.isEmpty()) {
                runResetProp("-d", name);
            } else {
                runResetProp(name, originalValue);
            }
        }

        private static CommandResult runResetProp(String... arguments) {
            String[] command = new String[arguments.length + 1];
            command[0] = RESET_PROP;
            System.arraycopy(arguments, 0, command, 1, arguments.length);
            return runCommand(command);
        }

        private static CommandResult runCommand(String... command) {
            try {
                java.lang.Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
                String output;
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                        process.getInputStream(), StandardCharsets.UTF_8))) {
                    output = reader.lines().collect(Collectors.joining("\n"));
                }
                return new CommandResult(process.waitFor() == 0, output);
            } catch (IOException | InterruptedException e) {
                if (e instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
                LogUtils.e(TAG, e.getLocalizedMessage() != null
                        ? e.getLocalizedMessage() : e.getClass().getName());
                return new CommandResult(false, "");
            }
        }

        private static final class CommandResult {
            final boolean success;
            final String output;

            CommandResult(boolean success, String output) {
                this.success = success;
                this.output = output;
            }
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
            // Prefer the current ksud UAPI; KernelSU 3.x no longer exposes the legacy prctl ABI.
            // 优先使用新版 ksud UAPI；KernelSU 3.x 已不再暴露旧版 prctl ABI。
            CommandResult result = runCommand(KSUD, "debug", "version");
            int version = result.success ? KernelSuCliUtils.parseVersion(result.output) : -1;
            if (version > 0) {
                return version;
            }
            return Natives.getVersion();
        }

        @Override
        public boolean KsuisSafeMode() {
            return Natives.isSafeMode();
        }

        @Override
        public boolean KsuIsLkmMode() {
            CommandResult result = runCommand(KSUD, "debug", "info");
            if (result.success && KernelSuCliUtils.hasLkmField(result.output)) {
                return KernelSuCliUtils.isLkmMode(result.output);
            }
            return Natives.isLkmMode();
        }

    }
}
