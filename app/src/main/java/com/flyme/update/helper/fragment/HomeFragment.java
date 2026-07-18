package com.flyme.update.helper.fragment;

import static com.flyme.update.helper.R.layout;

import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.URLSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.NotificationManagerCompat;
import androidx.fragment.app.Fragment;

import com.ejlchina.okhttps.OkHttps;
import com.flyme.update.helper.R;
import com.flyme.update.helper.activity.BaseActivity;
import com.flyme.update.helper.interfaces.IUpdateCallback;
import com.flyme.update.helper.manager.UpdateServiceManager;
import com.flyme.update.helper.utils.AndroidInfo;
import com.flyme.update.helper.utils.ColorChangeUtils;
import com.flyme.update.helper.utils.Config;
import com.flyme.update.helper.utils.FileDialogUtils;
import com.flyme.update.helper.utils.LogUtils;
import com.flyme.update.helper.utils.NotificationUtils;
import com.flyme.update.helper.manager.SuFileManager;
import com.flyme.update.helper.proxy.UpdateEngineProxy;
import com.flyme.update.helper.bean.UpdateInfo;
import com.flyme.update.helper.utils.Utils;
import com.flyme.update.helper.widget.TouchFeedback;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.kongzue.dialogx.dialogs.BottomDialog;
import com.kongzue.dialogx.dialogs.BottomMenu;
import com.kongzue.dialogx.dialogs.InputDialog;
import com.kongzue.dialogx.dialogs.MessageDialog;
import com.kongzue.dialogx.dialogs.PopTip;
import com.kongzue.dialogx.dialogs.TipDialog;
import com.kongzue.dialogx.dialogs.WaitDialog;
import com.kongzue.dialogx.interfaces.OnDialogButtonClickListener;
import com.kongzue.dialogx.interfaces.OnIconChangeCallBack;
import com.kongzue.filedialog.interfaces.FileSelectCallBack;
import com.topjohnwu.superuser.Shell;
import com.topjohnwu.superuser.ShellUtils;
import com.topjohnwu.superuser.nio.ExtendedFile;
import org.apache.commons.io.IOUtils;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import me.itangqi.waveloadingview.WaveLoadingView;

/** @noinspection BooleanMethodIsAlwaysInverted*/
public class HomeFragment extends Fragment implements TouchFeedback.OnFeedBackListener {

    private NotificationManager mNotificationManager;

    private String installDir;

    private WaitDialog mWaitDialog;

    private UpdateInfo uUpdateInfo;

    private boolean isDowngradeUpdate;

    private final IUpdateCallback engineCallback = new IUpdateCallback.Stub() {
        @Override
        public void onPayloadApplicationComplete(int error_code) {
            //activity.uUpdateServiceManager.closeAssetFileDescriptor();
            if (error_code == UpdateEngineProxy.ErrorCodeConstants.SUCCESS) {
                boolean hasDisplayid = !TextUtils.isEmpty(uUpdateInfo.getDisplayid());
                mNotificationManager.notify(1, NotificationUtils.notifyMsg(activity, hasDisplayid ? uUpdateInfo.getDisplayid() : "重启手机即可完成更新",  hasDisplayid ? "重启手机即可完成更新" : "恭喜你，更新成功了"));
            } else {
                activity.getMainHandler().post(() -> showErrorDialog("更新失败", getUpdateErrorMessage(error_code)));
                mNotificationManager.notify(1, NotificationUtils.notifyMsg(activity,"请稍后重试，或联系开发者反馈","哎呀，开了个小差，更新失败了，错误代号：" + error_code));
            }
        }

        @Override
        public void onStatusUpdate(int status_code, float percentage) {
            if (status_code == UpdateEngineProxy.UpdateStatusConstants.DOWNLOADING) {
                int progress = (int)(percentage * 100.f);
                mWaitDialog.show("正在更新 " + progress + " %", percentage);
                if (TextUtils.isEmpty(uUpdateInfo.getDisplayid())) {
                    mNotificationManager.notify(1, NotificationUtils.notifyProgress(activity,"正在更新系统","系统更新",100, progress,false));
                } else {
                    mNotificationManager.notify(1, NotificationUtils.notifyProgress(activity, uUpdateInfo.getDisplayid(),"系统正在更新",100, progress,false));
                }
            } else if (status_code == UpdateEngineProxy.UpdateStatusConstants.VERIFYING || status_code == UpdateEngineProxy.UpdateStatusConstants.FINALIZING) {
                mWaitDialog.show("正在校验分区数据");
                mNotificationManager.notify(1, NotificationUtils.notifyProgress(activity,"正在校验分区数据","系统正在更新",0, 0,true));
            } else if (status_code == UpdateEngineProxy.UpdateStatusConstants.UPDATED_NEED_REBOOT) {
                mWaitDialog.dismiss();
                if (!modifyPrivate()) {
                    LogUtils.e("onStatusUpdate", "modifyPrivate fail");
                    UpdateServiceManager.getInstance().cancel();
                    TipDialog.show("更新失败!", WaitDialog.TYPE.ERROR);
                    mNotificationManager.notify(1, NotificationUtils.notifyMsg(activity,"请稍后重试，或联系开发者反馈","哎呀，开了个小差，更新失败了"));
                    return;
                }
                LogUtils.d("onStatusUpdate", "ShowUpdateSuccessDialog");
                updateSuccess();
            }
        }
    };



    private BaseActivity activity;

    @Override
    public void onAttach(@NonNull Activity activity) {
        super.onAttach(activity);
        this.activity = (BaseActivity) activity;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mNotificationManager = (NotificationManager) activity.getSystemService(Context.NOTIFICATION_SERVICE);
        installDir = activity.getFilesDir().toString();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        return inflater.inflate(layout.fragment_home, container, false);
    }


    @MainThread
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        TouchFeedback touchFeedback = TouchFeedback.newInstance(activity);
        WaveLoadingView waveLoadingView = view.findViewById(R.id.wave_view);
        waveLoadingView.setWaveShiftRatio(0.75f);
        View bgView = view.findViewById(R.id.bg_view);
        new ColorChangeUtils(activity, new int[] {
                Color.parseColor("#FFEF5361"),
                Color.parseColor("#FFFD6D4B"),
                Color.parseColor("#FFFFCF47"),
                Color.parseColor("#FF9FD661"),
                Color.parseColor("#FF3FD1AD"),
                Color.parseColor("#FF2CBDF4"),
                Color.parseColor("#FFAD8FEF"),
                Color.parseColor("#FFEE85C0")
        }, bgView).startAnimation();

        //初始化设备信息
        AndroidInfo androidInfo = new AndroidInfo(activity);
        view.<TextView>findViewById(R.id.main_device_info).setText(MessageFormat.format("设备型号：{0}    V-AB：{1}", androidInfo.getModel(), Config.isVab ? Config.currentSlot.replace("_", "") : "FALSE"));

        TextView mainTips = view.findViewById(R.id.main_tips);
        mainTips.setText(getClickableHtml(activity.getString(R.string.main_tips)));
        mainTips.setMovementMethod(LinkMovementMethod.getInstance());

        touchFeedback.setOnFeedBackListener(this, view.findViewById(R.id.help_button));
        touchFeedback.setOnFeedBackListener(this, view.findViewById(R.id.waring_button));
        touchFeedback.setOnFeedBackListener(this, view.findViewById(R.id.start_button));
        touchFeedback.setOnFeedBackListener(this, view.findViewById(R.id.download_button));
        touchFeedback.setOnFeedBackListener(this, view.findViewById(R.id.home_tips_card));
    }


    public void setLinkClickable(SpannableStringBuilder clickableHtml, URLSpan urlSpan) {
        int start = clickableHtml.getSpanStart(urlSpan);
        int end = clickableHtml.getSpanEnd(urlSpan);
        int flags = clickableHtml.getSpanFlags(urlSpan);
        ClickableSpan clickableSpan = new ClickableSpan() {
            @Override
            public void onClick(@NonNull View widget) {
                if (urlSpan.getURL()!= null) {
                    Intent intent = new Intent();
                    intent.setAction("android.intent.action.VIEW");
                    intent.setData(Uri.parse(urlSpan.getURL()));
                    activity.startActivity(intent);
                }
            }
        };
        clickableHtml.setSpan(clickableSpan, start, end, flags);
    }

    public CharSequence getClickableHtml(String text) {
        Spanned spannedHtml = Html.fromHtml(text, Html.FROM_HTML_MODE_LEGACY);
        SpannableStringBuilder clickableHtmlBuilder = new SpannableStringBuilder(spannedHtml);
        URLSpan[] urls = clickableHtmlBuilder.getSpans(0, spannedHtml.length(), URLSpan.class);
        for (URLSpan span : urls){
            setLinkClickable(clickableHtmlBuilder, span);
        }
        return clickableHtmlBuilder;
    }

    private boolean flash_image(String img, String block) {
        // OTA leaves boot partitions read-only; make only the exact target writable temporarily.
        // OTA 完成后启动分区为只读；仅临时解除目标分区只读标志。
        if (!Shell.cmd("blockdev --setrw '" + block + "'").exec().isSuccess()) {
            LogUtils.e("flash_image", "failed to set target block writable");
            return false;
        }
        try {
            ExtendedFile bootBlock  = SuFileManager.getInstance().getRemote().getFile(block);
            if (!bootBlock.exists()) {
                LogUtils.e("flash_image", "block file no exists");
                return false;
            }
            ExtendedFile sourceImage = SuFileManager.getInstance().getRemote().getFile(img);
            long expectedBytes = sourceImage.length();
            try (InputStream in = sourceImage.newInputStream();
                 OutputStream out = bootBlock.newOutputStream()) {
                long copiedBytes = IOUtils.copyLarge(in, out);
                out.flush();
                return expectedBytes > 0 && copiedBytes == expectedBytes;
            }
        } catch (IOException e) {
            LogUtils.e("flash_image", e.getLocalizedMessage());
            return false;
        } finally {
            Shell.cmd("sync", "blockdev --setro '" + block + "'").exec();
        }

        /*List<String> out = Shell.getShell().newJob()
                .add("flash_image '" + img + "' '" + block + "'")
                .add("echo $?")
                .to(new ArrayList<>(), null)
                .exec()
                .getOut();
        String result = ShellUtils.isValidOutput(out) ? out.get(out.size() - 1) : "";
        return result.equals("0");*/
    }

    private boolean extract_image(String img, String block) {
        try {
            ExtendedFile bootBlock  = SuFileManager.getInstance().getRemote().getFile(img);
            if (!bootBlock.exists()) {
                LogUtils.e("extract_image", "img file no exists");
                return false;
            }
            ExtendedFile bootBackup = SuFileManager.getInstance().getRemote().getFile(block);
            try (InputStream in = bootBlock.newInputStream();
                 OutputStream out = bootBackup.newOutputStream()) {
                long copiedBytes = IOUtils.copyLarge(in, out);
                out.flush();
                return copiedBytes > 0;
            }
        } catch (IOException e) {
            LogUtils.e("extract_image", e.getLocalizedMessage());
            return false;
        }
    }

   private static final byte[] bootloaderFlags = {
            0x61, 0x63, 0x74, 0x6F, 0x72, 0x79, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x6D,
            0x54, 0x65, 0x73, 0x74, 0x33, 0x2E, 0x31, 0x30, 0x2E, 0x37, 0x2E, 0x30
    };

    //修改 private 镜像 将不完美解锁方案变为完美
    private boolean modifyPrivate() {
        if (!Config.flymemodel.equals("M2391") && !Config.flymemodel.equals("M2381"))
            return true;
        try {
            LogUtils.i("modifyPrivate", "modifyPrivate start");
            String srcImage = "/dev/block/bootdevice/by-name/private";
            ShellUtils.fastCmd("rm -r " + installDir + "/*.img");
            if (!extract_image(srcImage,installDir + "/private.img")) {
                LogUtils.e("modifyPrivate", "extract image private fail");
                return false;
            }
            ShellUtils.fastCmd("chmod 777 " + installDir + "/private.img");
            RandomAccessFile randomAccessFile = new RandomAccessFile(installDir + "/private.img", "rw");
            int keySize = 0x14120 - 0x14000;
            byte[] bootlodaerKey = new byte[keySize];
            randomAccessFile.seek(0x14000);
            randomAccessFile.read(bootlodaerKey);
            if (bootlodaerKey[0] == 'L' && bootlodaerKey[1] == 'O' && bootlodaerKey[2] == 'C' && bootlodaerKey[3] == 'K') {
                LogUtils.i("modifyPrivate", "modify BL data");
                randomAccessFile.seek(0x14000);
                randomAccessFile.write(new byte[keySize]);//填充 0
                randomAccessFile.seek(0x11000);
                randomAccessFile.write(bootlodaerKey);//把解锁数据移动到这里
            }
            LogUtils.i("modifyPrivate", "set mTest Flags");
            randomAccessFile.seek(0x11105);
            randomAccessFile.write(bootloaderFlags);//设置 mTest 标识，主要是这个
            randomAccessFile.close();
            return flash_image(installDir + "/private.img", srcImage);
        } catch (IOException e) {
            Log.e("modifyPrivate", e.getLocalizedMessage());
            return false;
        }

    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.download_button) {
            modifyPrivate();
        }

        else if (id == R.id.help_button) {
            showErrorDialog("使用方法","准备工作：\n下载好与本设备对应的刷机包，并且给软件授予相应的权限\n\n使用方法：\n1. 点击“选择文件更新”按钮选择下载好的刷机包\n2. 弹出窗口，点击“开始更新”并等待结束\n3. 选择是否保留 Root \n4. 重启手机即可");
        } else if (id == R.id.waring_button) {
            showErrorDialog("注意事项","请确保选择的更新包是全量更新包，更新后完成后可自己完成保留ROOT操作。由于不正确操作或【意外】导致的损失，需自行承担！！！");
        } else if (id == R.id.start_button) {
            if (UpdateServiceManager.getInstance().getService() == null) {
                showErrorDialog("温馨提示","服务未成功启动，请重试~");
                return;
            }

            if (!UpdateServiceManager.getInstance().isValid()) {
                showErrorDialog("温馨提示","当前设备无法找到服务，请联系开发者尝试解决~");
                return;
            }

            NotificationManagerCompat notification = NotificationManagerCompat.from(activity);
            boolean isEnabled = notification.areNotificationsEnabled();
            if (!Config.isVab){
                new MaterialAlertDialogBuilder(activity, com.google.android.material.R.style.ThemeOverlay_MaterialComponents_MaterialAlertDialog_Centered)
                        .setTitle("温馨提示")
                        .setCancelable(false)
                        .setMessage("本软件只支持V-AB分区的设备进行ROM刷写，不支持当前设备~")
                        .setPositiveButton("关闭软件", (dialog, which) -> activity.finish())
                        .create().show();
            } else if (!isEnabled) {
                activity.showNotificationPermissinDialog();
            }
            else {
                NotificationChannel channel = new NotificationChannel("new_version_push", "新版本推送", NotificationManager.IMPORTANCE_LOW);
                mNotificationManager.createNotificationChannel(channel);
                FileDialogUtils.build().setSuffixArray(new String[]{".zip"})
                        .selectFile(new FileSelectCallBack() {
                            @Override
                            public void onSelect(File file, String filePath) {
                                mWaitDialog = WaitDialog.show("正在检查更新包...");
                                activity.getASynHandler().post(() -> parseUpdatePackage(filePath));
                            }
                        });
            }
        }

    }


    @Override
    public void onLongClick(View view) { }


    private void showErrorDialog(CharSequence title, CharSequence message) {
        WaitDialog.dismiss();
        new MaterialAlertDialogBuilder(activity, com.google.android.material.R.style.ThemeOverlay_MaterialComponents_MaterialAlertDialog_Centered)
                .setTitle(title)
                .setCancelable(false)
                .setMessage(message)
                .setPositiveButton("知道了", null)
                .create().show();
    }

    private void parseUpdatePackage(String filePath) {
        UpdateInfo updateInfo = UpdateServiceManager.getInstance().parseUpdatePackage(filePath);
        activity.getMainHandler().post(() -> showUpdateConfirmation(filePath, updateInfo));
    }

    private void showUpdateConfirmation(String filePath, UpdateInfo updateInfo) {
        WaitDialog.dismiss();
        uUpdateInfo = updateInfo;
        isDowngradeUpdate = false;
        if (uUpdateInfo == null || uUpdateInfo.getHeaderKeyValuePairs() == null || uUpdateInfo.getHeaderKeyValuePairs().length == 0) {
            showErrorDialog("更新失败","更新包不完整，请重新下载");
        } else if (uUpdateInfo.getType() != -1 && uUpdateInfo.getType() != 1) {
            showErrorDialog("更新失败","更新包非全量包，请下载全量包");
        }
        else {
            if (uUpdateInfo.getBuildTimestamp() > 0
                    && Config.currentBuildTimestamp > 0
                    && uUpdateInfo.getBuildTimestamp() < Config.currentBuildTimestamp) {
                showDowngradeConfirmation(filePath);
                return;
            }
            if (!TextUtils.isEmpty(uUpdateInfo.getFlymeid()) && !uUpdateInfo.getFlymeid().equals(Config.flymemodel)) {
                new MaterialAlertDialogBuilder(activity, com.google.android.material.R.style.ThemeOverlay_MaterialComponents_MaterialAlertDialog_Centered)
                        .setTitle("温馨提示")
                        .setCancelable(false)
                        .setMessage("检测到选择的全量包是: " + uUpdateInfo.getBuildInfo() + ", 与您的设备不符，是否继续更新")
                        .setPositiveButton("继续", (dialog, which) -> startParsedUpdate(false))
                        .setNegativeButton("取消",null)
                        .create().show();
                return;
            }

            MessageDialog.build()
                    .setTitle("确认更新?")
                    .setMessage(filePath)
                    .setOkButton("开始更新", (baseDialog, v) -> {
                        startParsedUpdate(false);
                        return false;
                    })
                    .setCancelButton("取消更新")
                    .show();
        }
    }

    private void showDowngradeConfirmation(String filePath) {
        if (uUpdateInfo.getType() != 1) {
            showErrorDialog("无法降级", "降级仅支持明确标记为全量包的固件。");
            return;
        }
        if (!TextUtils.isEmpty(uUpdateInfo.getFlymeid()) && !uUpdateInfo.getFlymeid().equals(Config.flymemodel)) {
            showErrorDialog("设备不匹配", "降级包与当前设备不匹配，已禁止继续操作。");
            return;
        }
        if (!UpdateServiceManager.getInstance().isDowngradeSupported()) {
            showErrorDialog("无法降级", "未检测到 bootconfig 的真实解锁状态、可用的 KernelSU resetprop 或 super 分区写权限。");
            return;
        }
        new MaterialAlertDialogBuilder(activity, com.google.android.material.R.style.ThemeOverlay_MaterialComponents_MaterialAlertDialog_Centered)
                .setTitle("高风险降级")
                .setCancelable(false)
                .setMessage("将从 " + Config.currentDisplayId + " 降级到 " + uUpdateInfo.getDisplayid()
                        + "。成功后会强制清除全部用户数据。请确认数据已经备份、电量充足，并保持设备连接电源。\n\n更新包：" + filePath)
                .setPositiveButton("我已备份，继续", (dialog, which) -> showFinalDowngradeConfirmation())
                .setNegativeButton("取消", null)
                .create().show();
    }

    private void showFinalDowngradeConfirmation() {
        new MaterialAlertDialogBuilder(activity, com.google.android.material.R.style.ThemeOverlay_MaterialComponents_MaterialAlertDialog_Centered)
                .setTitle("最后确认")
                .setCancelable(false)
                .setMessage("降级开始后不可中断，完成后将恢复出厂设置。确认立即开始降级？")
                .setPositiveButton("清除数据并降级", (dialog, which) -> startParsedUpdate(true))
                .setNegativeButton("取消", null)
                .create().show();
    }

    private void startParsedUpdate(boolean allowDowngrade) {
        isDowngradeUpdate = allowDowngrade;
        mWaitDialog = WaitDialog.show("准备更新中...");
        activity.getASynHandler().postDelayed(() -> {
            if (!UpdateServiceManager.getInstance().startUpdateSystem(uUpdateInfo, engineCallback, allowDowngrade)) {
                activity.getMainHandler().post(() -> showErrorDialog("更新失败", "更新服务错误，请重试"));
            }
        }, 2000);
    }

    private String getUpdateErrorMessage(int errorCode) {
        switch (errorCode) {
            case UpdateEngineProxy.ErrorCodeConstants.PAYLOAD_TIMESTAMP_ERROR:
                return isDowngradeUpdate
                        ? "系统仍拒绝降级，可能存在 AVB 防回滚限制。"
                        : "更新包版本早于当前系统，Android 更新引擎禁止降级。";
            case UpdateEngineProxy.ErrorCodeConstants.NOT_ENOUGH_SPACE:
                return "存储空间不足，请清理空间后重试。";
            case UpdateEngineProxy.ErrorCodeConstants.DOWNLOAD_PAYLOAD_VERIFICATION_ERROR:
            case UpdateEngineProxy.ErrorCodeConstants.PAYLOAD_HASH_MISMATCH_ERROR:
                return "更新包校验失败，请重新下载完整包。";
            case UpdateEngineProxy.ErrorCodeConstants.DOWNLOAD_TRANSFER_ERROR:
                return "读取更新包失败，请检查文件后重试。";
            default:
                return "更新引擎返回错误代码：" + errorCode;
        }
    }



    private void patchMagisk() {
        WaitDialog.show("正在安装 Magisk ...");
        String[] envList = new String[]{"busybox", "magiskboot", "magiskinit", "util_functions.sh", "boot_patch.sh"};
        for (String file: envList) {
            if (!SuFileManager.getInstance().getRemote().getFile("/data/adb/magisk/" + file).exists()) {
                showRebootDialog("修补失败","Magisk 环境不全，请自行操作");
                return;
            }
        }

        try {
            ExtendedFile stub = SuFileManager.getInstance().getRemote().getFile("/data/adb/magisk/stub.apk");
            if (!stub.exists()) {
                OutputStream out = stub.newOutputStream();
                IOUtils.copy(activity.getAssets().open("stub.apk"), out);
            }
        } catch (IOException e) {
            showRebootDialog("修补失败","面具环境不全，请自行操作");
        }

        // 读取当前分区，用来判断第二分区
        String next_slot = Config.currentSlot.equals("_a") ? "_b" : "_a";

        // 提取第二分区的boot镜像
        String srcBoot = "/dev/block/bootdevice/by-name/init_boot" + next_slot;
        if (!SuFileManager.getInstance().getRemote().getFile(srcBoot).exists() || Build.MODEL.equals("PHP110")) {
            srcBoot = "/dev/block/bootdevice/by-name/boot" + next_slot;
        }

        new File(installDir + "/boot.img").delete();
        new File(installDir + "/magisk_patch.img").delete();

        if (!extract_image(srcBoot,installDir + "/boot.img")) {
            showRebootDialog("安装失败","镜像分区提取错误，请自行操作");
            return;
        }
        List<String> stdout = new ArrayList<>();
        // 使用面具自带的脚本进行修补
        boolean isSuccess = Shell.getShell().newJob()
                .add("cd /data/adb/magisk")
                .add("KEEPFORCEENCRYPT=" + Config.keepEnc + " " +
                        "KEEPVERITY=" + Config.keepVerity + " " +
                        "PATCHVBMETAFLAG=" + Config.patchVbmeta + " "+
                        "RECOVERYMODE=" + Config.recovery + " "+
                        "SYSTEM_ROOT=" + Config.isSAR + " " +
                        "sh boot_patch.sh " + installDir + "/boot.img")
                .to(stdout, stdout)
                .exec()
                .isSuccess();
        if (!isSuccess) {
            showErrorDialog(String.join("\n", stdout));
            return;
        }
        Shell.getShell().newJob().add("./magiskboot cleanup", "mv ./new-boot.img " + installDir + "/magisk_patch.img", "rm ./stock_boot.img", "cd /").exec();
        if (!flash_image(installDir + "/magisk_patch.img", srcBoot)) {
            showRebootDialog("安装失败","刷入镜像失败，请自行操作");
            return;
        }
        showRebootDialog("安装成功","安装到未使用卡槽完成");
    }


    private static int ksuversion = -1;

    private void patchKernelSU() {
        WaitDialog.show("正在安装 KernelSu...");
        if (ksuversion == -1)
            ksuversion = UpdateServiceManager.getInstance().GetKsuVersion();
        if (ksuversion <= 0) {
            showRebootDialog("修补失败", "无法读取 KernelSU 版本，请自行操作");
            return;
        }
        boolean isLkmMode = UpdateServiceManager.getInstance().KsuIsLkmMode();

        // 读取当前分区，用来判断第二分区
        String next_slot = Config.currentSlot.equals("_a") ? "_b" : "_a";

        // 非 LKM 模式直接提取当前分区刷入第二分区
        if (!isLkmMode && !flash_image("/dev/block/bootdevice/by-name/boot" + Config.currentSlot, "/dev/block/bootdevice/by-name/boot" + next_slot)) {
            showRebootDialog("安装失败","刷入镜像失败，请自行操作");
            return;
        }


        //检查 ksud 文件是否存在
        if (!SuFileManager.getInstance().getRemote().getFile("/data/adb/ksud").exists()) {
            showRebootDialog("修补失败","KernelSu 环境不全，请自行操作");
            return;
        }

        //ksud boot-patch -b <boot.img> --kmi android13-5.10

        // 提取第二分区的boot镜像
        String srcBoot = "/dev/block/bootdevice/by-name/init_boot" + next_slot;
        if (!SuFileManager.getInstance().getRemote().getFile(srcBoot).exists() || Build.MODEL.equals("PHP110")) {
            srcBoot = "/dev/block/bootdevice/by-name/boot" + next_slot;
        }

        String bootImage = installDir + "/boot.img";
        String patchImage = installDir + "/kernelsu_patch.img";
        ShellUtils.fastCmd("rm -f '" + bootImage + "' '" + patchImage + "'");

        if (!extract_image(srcBoot, bootImage)) {
            showRebootDialog("安装失败","镜像分区提取错误，请自行操作");
            return;
        }


        List<String> stdout = new ArrayList<>();
        boolean isSuccess = Shell.getShell().newJob()
                .add("/data/adb/ksud boot-patch -b '" + bootImage
                        + "' --out '" + installDir + "' --out-name 'kernelsu_patch.img'")
                .to(stdout, stdout)
                .exec()
                .isSuccess();
        if (!isSuccess) {
            showErrorDialog(String.join("\n", stdout));
            return;
        }


        if (!SuFileManager.getInstance().getRemote().getFile(patchImage).exists()) {
            showRebootDialog("安装失败","获取修补文件错误，请自行操作");
            return;
        }

        if (!flash_image(patchImage, srcBoot)) {
            showRebootDialog("安装失败","刷入镜像失败，请自行操作");
            return;
        }

        showRebootDialog("安装成功","安装到未使用卡槽完成");
    }

    private void patchAPatch(String SuperKey) {
        WaitDialog.show("开始安装 APatch ...");

        // 这里使用 Github 接口，获取 releases 最新版本号
        String rep = OkHttps.sync("http://kpatch.oss-cn-shenzhen.aliyuncs.com/latest.json")
                .get()
                .getBody().toString();


        String lastTag = "";
        try {
            JSONObject jsonObject = new JSONObject(rep);
            lastTag = jsonObject.getString("tag_name");
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (TextUtils.isEmpty(lastTag)) {
            showRebootDialog("安装失败","APatch 版本获取失败，请自行操作");
            return;
        }

        new File(installDir + "/kpimg").delete();
        OkHttps.sync("http://kpatch.oss-cn-shenzhen.aliyuncs.com/" + lastTag + "/kpimg")
                .get()
                .getBody()
                .toFile(installDir + "/kpimg")
                .start();

        new File(installDir + "/kptools").delete();
        OkHttps.sync("http://kpatch.oss-cn-shenzhen.aliyuncs.com/" + lastTag + "/kptools")
                .get()
                .getBody()
                .toFile(installDir + "/kptools")
                .start();

        new File(installDir + "/kpatch").delete();
        OkHttps.sync("http://kpatch.oss-cn-shenzhen.aliyuncs.com/" + lastTag + "/kpatch")
                .get()
                .getBody()
                .toFile(installDir + "/kpatch")
                .start();

        ShellUtils.fastCmd("chmod -R 777 " + installDir);

        String[] envList = new String[]{"kptools", "magiskboot", "kpimg", "kpatch"};
        for (String file: envList) {
            if (!new File(installDir + "/" + file).exists()) {
                showRebootDialog("修补失败",file + " 文件不存在，请自行修补");
                return;
            }
        }

        // 读取当前分区，用来判断第二分区
        String slot = Config.currentSlot.equals("_a") ? "_b" : "_a";

        // 提取第二分区的boot镜像
        String srcBoot = "/dev/block/bootdevice/by-name/boot" + slot;

        new File(installDir + "/boot.img").delete();
        new File(installDir + "/apatch_patch.img").delete();

        if (!extract_image(srcBoot,installDir + "/boot.img")) {
            showRebootDialog("安装失败","镜像分区提取错误，请自行操作");
            return;
        }

        List<String> stdout = new ArrayList<>();
        // 使用面具自带的脚本进行修补
        boolean isSuccess = Shell.getShell().newJob()
                .add("cd " + installDir)
                .add("sh apatch.sh " + SuperKey + " " + installDir + "/boot.img -K kpatch")
                .to(stdout, stdout)
                .exec()
                .isSuccess();
        if (!isSuccess) {
            showErrorDialog(String.join("\n", stdout));
            return;
        }

        Shell.getShell().newJob().add("./magiskboot cleanup", "mv ./new-boot.img " + installDir + "/apatch_patch.img", "rm ./stock_boot.img", "cd /").exec();

        if (!flash_image(installDir + "/apatch_patch.img", srcBoot)) {
            showRebootDialog("安装失败","刷入镜像失败，请自行操作");
            return;
        }
        showRebootDialog("安装成功","安装到未使用卡槽完成");
    }

    private boolean keyChecked(String skey) {
        boolean isDigit = false;
        boolean isLetter = false;
        for (int i = 0; i < skey.length(); i++) {
            char ch = skey.charAt(i);
            if (Character.isDigit(ch))
                isDigit = true;
            if (Character.isLetter(ch))
                isLetter = true;
        }
        return isDigit && isLetter && skey.matches("^[a-zA-Z0-9]+$");
    }

    private void updateSuccess() {
        WaitDialog.dismiss();

        BottomMenu.show("更新成功", "恭喜你看到我了，现在轮到你选择保留 Root 的方式了，如果需要，那就请在选项中选择一个吧。\n\n注意了：是选择里面的哦，不是点击按钮哦~", new String[]{"Magisk", "KernelSu", "APatch"})
                .setOnIconChangeCallBack(new OnIconChangeCallBack<BottomMenu>(true) {
                    @Override
                    public int getIcon(BottomMenu bottomMenu, int index, String menuText) {
                        switch (index) {
                            case 0:
                                return R.drawable.magisk;
                            case 1:
                                return R.drawable.kernelsu;
                            case 2:
                                return R.drawable.apatch;
                        }
                        return 0;
                    }
                })
                .setOnMenuItemClickListener((dialog, text, index) -> {
                    switch (index) {
                        case 0:
                            activity.getASynHandler().post(this::patchMagisk);
                            return false;
                        case 1:
                            activity.getASynHandler().post(this::patchKernelSU);
                            return false;
                        case 2:
                            new InputDialog("设定超级密钥", "内核补丁的唯一密钥，密钥长度最少8位数，且最少含有一位字母", "确定")
                                    .setCancelable(false)
                                    .setInputHintText("请输入超级密钥")
                                    .setOkButton((baseDialog, v, inputStr) -> {
                                        if (TextUtils.isEmpty(inputStr)) {
                                            PopTip.build().setMessage("请输入超级密钥！").iconError().show();
                                            return true;
                                        }
                                        if (inputStr.length() < 8) {
                                            PopTip.build().setMessage("输入的超级密钥长度小于8").iconError().show();
                                            return true;
                                        }

                                        if (!keyChecked(inputStr)) {
                                            PopTip.build().setMessage("超级密钥格式错误，请检查是否只含有数字和字母").iconError().show();
                                            return true;
                                        }

                                        activity.getASynHandler().post(() -> patchAPatch(inputStr));
                                        return false;
                                    })
                                    .show();
                            return false;
                    }
                    return false;
                })
                .setOkButton("重启", (OnDialogButtonClickListener<BottomDialog>) (dialog, v) -> {
                    Utils.reboot();
                    return false;
                })
                .setCancelButton("取消", (OnDialogButtonClickListener<BottomDialog>) (dialog, v) -> {
                    dialog.dismiss();
                    return false;
                });
    }

    private void showRebootDialog(String title,String message) {
        WaitDialog.dismiss();
        MessageDialog.build()
                .setTitle(title)
                .setMessage(message)
                .setOkButton("重启手机",(baseDialog, v) -> {
                    baseDialog.dismiss();
                    Utils.reboot();
                    return false;
                })
                .setCancelButton("稍后重启")
                .show();
    }

    private void showErrorDialog(String error) {
        WaitDialog.dismiss();
        MessageDialog.build()
                .setTitle("修补失败")
                .setMessage("运行修补脚本失败，是否查看错误信息")
                .setOkButton("重启",(baseDialog, v) -> {
                    Utils.reboot();
                    return false;
                })
                .setCancelButton("查看",(baseDialog, v) -> {
                    MessageDialog.build()
                            .setTitle("错误信息")
                            .setMessage(error)
                            .setOkButton("我知道了")
                            .show();
                    return false;
                })
                .setOtherButton("取消")
                .show();
    }
}
