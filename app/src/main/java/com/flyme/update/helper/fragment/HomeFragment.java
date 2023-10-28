package com.flyme.update.helper.fragment;

import static com.flyme.update.helper.R.id;
import static com.flyme.update.helper.R.layout;
import static com.flyme.update.helper.R.string;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.UpdateEngine;
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
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationManagerCompat;
import androidx.fragment.app.Fragment;

import com.flyme.update.helper.App;
import com.flyme.update.helper.R;
import com.flyme.update.helper.activity.BaseActivity;
import com.flyme.update.helper.interfaces.IUpdateCallback;
import com.flyme.update.helper.utils.AndroidInfo;
import com.flyme.update.helper.utils.ColorChangeUtils;
import com.flyme.update.helper.utils.Config;
import com.flyme.update.helper.utils.NotificationUtils;
import com.flyme.update.helper.utils.UpdateInfo;
import com.flyme.update.helper.utils.UpdateParser;
import com.flyme.update.helper.widget.TouchFeedback;
import com.kongzue.dialogx.dialogs.MessageDialog;
import com.kongzue.dialogx.dialogs.TipDialog;
import com.kongzue.dialogx.dialogs.WaitDialog;
import com.kongzue.filedialog.FileDialog;
import com.kongzue.filedialog.interfaces.FileSelectCallBack;
import com.topjohnwu.superuser.Shell;
import com.topjohnwu.superuser.ShellUtils;
import com.topjohnwu.superuser.nio.ExtendedFile;
import com.topjohnwu.superuser.nio.FileSystemManager;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import me.itangqi.waveloadingview.WaveLoadingView;

public class HomeFragment extends Fragment implements TouchFeedback.OnFeedBackListener {

    private NotificationManager mNotificationManager;

    private String installDir;

    private final Shell shell = Shell.getShell();

    private FileSystemManager remoteFS;

    private WaitDialog mWaitDialog;

    private UpdateInfo uUpdateInfo;

    private final IUpdateCallback mUpdateCallback = new IUpdateCallback.Stub() {
        @Override
        public void onPayloadApplicationComplete(int errorCode) {
            Log.d("IUpdateService", "更新失败" + errorCode);
            if (errorCode == UpdateEngine.ErrorCodeConstants.SUCCESS) {
                if (TextUtils.isEmpty(uUpdateInfo.getDisplayid())) {
                    mNotificationManager.notify(1, NotificationUtils.notifyMsg(activity,"重启手机即可完成更新","恭喜你，更新成功了"));
                } else {
                    mNotificationManager.notify(1, NotificationUtils.notifyMsg(activity, uUpdateInfo.getDisplayid(),"重启手机即可完成更新"));
                }
            } else {
                flashAbl();
                activity.uUpdateServiceManager.cancel();
                TipDialog.show("更新失败!", WaitDialog.TYPE.ERROR);
                mNotificationManager.notify(1, NotificationUtils.notifyMsg(activity,"请稍后重试，或联系开发者反馈","哎呀，开了个小差，更新失败了"));
            }
        }

        @Override
        public void onStatusUpdate(int status, float percent) {
            Log.d("IUpdateService", "当前状态：" + status + ",当前进度：" + percent);
            if (status == UpdateEngine.UpdateStatusConstants.DOWNLOADING) {
                int progress = (int)(percent * 100.f);
                mWaitDialog.show("正在更新 " + progress + " %", percent);
                if (TextUtils.isEmpty(uUpdateInfo.getDisplayid())) {
                    mNotificationManager.notify(1, NotificationUtils.notifyProgress(activity,"正在更新系统","系统更新",100, progress,false));
                } else {
                    mNotificationManager.notify(1, NotificationUtils.notifyProgress(activity, uUpdateInfo.getDisplayid(),"系统正在更新",100, progress,false));
                }
            } else if (status == UpdateEngine.UpdateStatusConstants.VERIFYING || status == UpdateEngine.UpdateStatusConstants.FINALIZING) {
                mWaitDialog.show("正在校验分区数据");
                mNotificationManager.notify(1, NotificationUtils.notifyProgress(activity,"正在校验分区数据","系统正在更新",0, 0,true));
            } else if (status == UpdateEngine.UpdateStatusConstants.UPDATED_NEED_REBOOT) {
                mWaitDialog.dismiss();
                if (!flashAbl()) {
                    activity.uUpdateServiceManager.cancel();
                    TipDialog.show("更新失败!", WaitDialog.TYPE.ERROR);
                    mNotificationManager.notify(1, NotificationUtils.notifyMsg(activity,"请稍后重试，或联系开发者反馈","哎呀，开了个小差，更新失败了"));
                    return;
                }
                updateSuccess();
            }
        }
    };


    public HomeFragment() {

    }

    public static HomeFragment newInstance() {
        HomeFragment fragment = new HomeFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

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

    @SuppressLint("SetTextI18n")
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        TouchFeedback touchFeedback = TouchFeedback.newInstance(activity);
        View inflate = inflater.inflate(layout.fragment_home, container, false);
        WaveLoadingView waveLoadingView = inflate.findViewById(id.wave_view);
        waveLoadingView.setWaveShiftRatio(0.75f);
        View bgView = inflate.findViewById(id.bg_view);

        activity.getMainHandler().postDelayed(()->{
            bgView.setLayoutParams(new RelativeLayout.LayoutParams(-1, waveLoadingView.getHeight()));
            new ColorChangeUtils(new int[] {
                    Color.parseColor("#FFEF5361"),
                    Color.parseColor("#FFFD6D4B"),
                    Color.parseColor("#FFFFCF47"),
                    Color.parseColor("#FF9FD661"),
                    Color.parseColor("#FF3FD1AD"),
                    Color.parseColor("#FF2CBDF4"),
                    Color.parseColor("#FFAD8FEF"),
                    Color.parseColor("#FFEE85C0")
            }, bgView).startAnimation();
        },0);



        //初始化设备信息
        AndroidInfo androidInfo = new AndroidInfo(activity);
        inflate.<TextView>findViewById(id.main_device_info).setText("设备型号：" + androidInfo.getModel() + "    V-AB：" + (App.isVab ? App.currentSlot.replace("_","") : "FALSE"));

        final TextView mainTips = inflate.findViewById(id.main_tips);
        mainTips.setText(getClickableHtml(activity.getString(string.main_tips)));
        mainTips.setMovementMethod(LinkMovementMethod.getInstance());

        touchFeedback.setOnFeedBackListener(this, inflate.findViewById(id.help_button));
        touchFeedback.setOnFeedBackListener(this, inflate.findViewById(id.waring_button));
        touchFeedback.setOnFeedBackListener(this, inflate.findViewById(id.start_button));
        touchFeedback.setOnFeedBackListener(this, inflate.findViewById(id.download_button));
        touchFeedback.setOnFeedBackListener(this, inflate.findViewById(id.home_tips_card));
        return inflate;
    }

    public void setLinkClickable(SpannableStringBuilder clickableHtml, URLSpan urlSpan) {
        int start = clickableHtml.getSpanStart(urlSpan);
        int end = clickableHtml.getSpanEnd(urlSpan);
        int flags = clickableHtml.getSpanFlags(urlSpan);
        ClickableSpan clickableSpan = new ClickableSpan() {
            @Override
            public void onClick(@NonNull View widget) {
                if(urlSpan.getURL()!=null){
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
        for (final URLSpan span : urls){
            setLinkClickable(clickableHtmlBuilder, span);
        }
        return clickableHtmlBuilder;
    }

    private boolean flash_image(String img, String block) {
        List<String> out = shell.newJob()
                .add("flash_image '" + img + "' '" + block + "'")
                .add("echo $?")
                .to(new ArrayList<>(), null)
                .exec()
                .getOut();
        String result = ShellUtils.isValidOutput(out) ? out.get(out.size() - 1) : "";
        Log.d("IUpdateService", "flash_image = " + result);
        return result.equals("0");
    }

    private boolean extract_image(String img, String block) {
        try {
            ExtendedFile bootBlock  = remoteFS.getFile(img);
            if (!bootBlock.exists()) {
                return false;
            }
            ExtendedFile bootBackup = remoteFS.getFile(block);
            InputStream in = bootBlock.newInputStream();
            OutputStream out = bootBackup.newOutputStream();
            return IOUtils.copy(in, out) > 0;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
/*        List<String> out = shell.newJob()
                .add("dd if='" + img + "' of='" + block + "'")
                .add("echo $?")
                .to(new ArrayList<>(), null)
                .exec()
                .getOut();
        String result = ShellUtils.isValidOutput(out) ? out.get(out.size() - 1) : "";
        Log.d("IUpdateService", "extract_image = " + result);
        return result.equals("0");*/
    }

    private boolean flashAbl() {
        if (App.flymemodel.equals("M2391") || App.flymemodel.equals("M2381")) {
            if (!flash_image(activity.getFilesDir().toString() + "/" + App.flymemodel + ".img","/dev/block/by-name/abl_a")) {
                return false;
            }
            return flash_image(activity.getFilesDir().toString() + "/" + App.flymemodel + ".img","/dev/block/by-name/abl_b");
        }
        return true;
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.help_button) {
            MessageDialog.build()
                    .setTitle("使用方法")
                    .setMessage("准备工作：\n下载好与本设备对应的刷机包，并且给软件授予相应的权限\n\n使用方法：\n1. 点击”选择文件更新“按钮选择下载好的刷机包\n2. 弹出窗口，点击”开始刷机“并等待结束\n3. 选择是否保留 Root \n4. 重启手机即可")
                    .setOkButton("知道了")
                    .show();
        } else if (id == R.id.waring_button) {
            MessageDialog.build()
                    .setTitle("注意事项")
                    .setMessage("请确保选择的更新包是全量更新包，更新后完成后可自己完成保留ROOT操作。由于不正确操作或【意外】导致的损失，需自行承担！！！")
                    .setOkButton("知道了")
                    .show();
        } else if (id == R.id.start_button) {
            if (activity.uUpdateServiceManager == null) {
                MessageDialog.build()
                        .setTitle("温馨提示")
                        .setMessage("服务未成功启动，请重试~")
                        .setOkButton("知道了")
                        .show();
                return;
            }
            NotificationManagerCompat notification = NotificationManagerCompat.from(activity);
            boolean isEnabled = notification.areNotificationsEnabled();
            if (!App.isVab){
                MessageDialog.build()
                        .setTitle("温馨提示")
                        .setMessage("本软件只支持V-AB分区的设备进行ROM刷写，不支持当前设备~")
                        .setOkButton("关闭软件",(baseDialog, v) -> {
                            activity.finish();
                            return false;
                        })
                        .show();
            } else if (!activity.hasWritePermission()){
                activity.showFilePermissinDialog();
            } else if (!isEnabled) {
                activity.showNotificationPermissinDialog();
            }
            else {

                NotificationChannel channel = new NotificationChannel("new_version_push", "新版本推送", NotificationManager.IMPORTANCE_LOW);
                mNotificationManager.createNotificationChannel(channel);
                FileDialog.build().setSuffixArray(new String[]{".zip"})
                        .selectFile(new FileSelectCallBack() {
                            @Override
                            public void onSelect(File file, String filePath) {
                                MessageDialog.build()
                                        .setTitle("选择文件?")
                                        .setMessage(filePath)
                                        .setOkButton("开始更新",(baseDialog, v) -> {
                                            mWaitDialog = WaitDialog.show("准备更新中...");
                                            activity.getASynHandler().postDelayed(() -> startUpdate(filePath),2000);
                                            return false;
                                        })
                                        .setCancelButton("取消更新")
                                        .show();
                            }
                        });
            }
        }

    }


    @Override
    public void onLongClick(View view) {

    }

    private void startUpdate(String File) {
        uUpdateInfo = new UpdateParser(File).parse();
        Log.d("更新助手",uUpdateInfo.getOffset() + ":"+uUpdateInfo.getSize());
        if (uUpdateInfo.getHeaderKeyValuePairs() == null || uUpdateInfo.getHeaderKeyValuePairs().length == 0) {
            WaitDialog.dismiss();
            MessageDialog.show("更新失败","更新包不完整，请重新下载","我知道了");
        } else if (uUpdateInfo.getType() != -1 && uUpdateInfo.getType() != 1) {
            WaitDialog.dismiss();
            MessageDialog.show("更新失败","更新包非全量包，请下载全量包","我知道了");
        }
        else {
            if (!TextUtils.isEmpty(uUpdateInfo.getFlymeid())) {
                if (!uUpdateInfo.getFlymeid().equals(App.flymemodel)) {
                    MessageDialog.build()
                            .setTitle("温馨提示")
                            .setMessage("检测到选择的全量包是: " + uUpdateInfo.getBuildInfo() + ", 与您的设备不符，是否继续更新")
                            .setOkButton("继续",(baseDialog, v) -> {
                                if (!activity.uUpdateServiceManager.startUpdateSystem(uUpdateInfo, mUpdateCallback)){
                                    WaitDialog.dismiss();
                                    MessageDialog.show("更新失败","更新服务错误，请重试","我知道了");
                                }
                                return false;
                            })
                            .setCancelButton("取消")
                            .show();
                    return;
                }
            }
            if (!activity.uUpdateServiceManager.startUpdateSystem(uUpdateInfo, mUpdateCallback)){
                WaitDialog.dismiss();
                MessageDialog.show("更新失败","更新服务错误，请重试","我知道了");
            }
        }
    }

    private void initRemoteFS() {
        if (remoteFS != null)
            return;
        remoteFS = activity.uUpdateServiceManager.getFileSystemManager();
    }

    private void reboot() {
        ShellUtils.fastCmd("/system/bin/svc power reboot || /system/bin/reboot");
    }

    private void patchMagisk() {
        WaitDialog.show("正在安装 Magisk ...");
        initRemoteFS();
        String[] envList = new String[]{"busybox", "magiskboot", "magiskinit", "util_functions.sh", "boot_patch.sh"};
        for (String file: envList) {
            if (!remoteFS.getFile("/data/adb/magisk/" + file).exists()) {
                showRebootDialog("修补失败","面具环境不全，请自行操作");
                return;
            }
        }


        try {
            ExtendedFile stub = remoteFS.getFile("/data/adb/magisk/stub.apk");
            if (!stub.exists()) {
                OutputStream out = stub.newOutputStream();
                IOUtils.copy(activity.getAssets().open("stub.apk"), out);
            }
        } catch (IOException e) {
            e.printStackTrace();
            showRebootDialog("修补失败","面具环境不全，请自行操作");
        }

        // 读取当前分区，用来判断第二分区
        String slot = App.currentSlot.equals("_a") ? "_b" : "_a";

        // 提取第二分区的boot镜像
        String srcBoot = "/dev/block/bootdevice/by-name/init_boot" + slot;
        if (!remoteFS.getFile(srcBoot).exists() || Build.MODEL.equals("PHP110")) {
            srcBoot = "/dev/block/bootdevice/by-name/boot" + slot;
        }

        new File(installDir + "/boot.img").delete();
        new File(installDir + "/magisk_patch.img").delete();

        if (!extract_image(srcBoot,installDir + "/boot.img")) {
            showRebootDialog("安装失败","镜像分区提取错误，请自行操作");
            return;
        }
        List<String> stdout = new ArrayList<>();
        // 使用面具自带的脚本进行修补
        boolean isSuccess = shell.newJob()
                .add("cd /data/adb/magisk")
                .add("KEEPFORCEENCRYPT=" + Config.keepEnc + " " +
                        "KEEPVERITY=" + Config.keepVerity + " " +
                        "PATCHVBMETAFLAG=" + Config.patchVbmeta + " "+
                        "RECOVERYMODE=" + Config.recovery + " "+
                        "SYSTEM_ROOT=" + Config.isSAR + " " +
                        "sh boot_patch.sh " + installDir + "/boot.img")
                .to(stdout, null)
                .exec()
                .isSuccess();
        if (!isSuccess) {
            showErrorDialog(String.join("\n", stdout));
            return;
        }
        shell.newJob().add("./magiskboot cleanup", "mv ./new-boot.img " + installDir + "/magisk_patch.img", "rm ./stock_boot.img", "cd /").exec();
        if (!flash_image(installDir + "/magisk_patch.img", srcBoot)) {
            showRebootDialog("安装失败","刷入镜像失败，请自行操作");
            return;
        }
        showRebootDialog("安装成功","安装到未使用卡槽完成");
    }


    private void patchKernelSU() {
        WaitDialog.show("正在安装 KernelSu...");

        // 读取当前分区，用来判断第二分区
        String slot = App.currentSlot.equals("_a") ? "_b" : "_a";

        if (!flash_image("/dev/block/bootdevice/by-name/boot" + App.currentSlot, "/dev/block/bootdevice/by-name/boot" + slot)) {
            showRebootDialog("安装失败","刷入镜像失败，请自行操作");
            return;
        }
        showRebootDialog("安装成功","安装到未使用卡槽完成");
    }

    private void updateSuccess() {
        if (Config.hasMagisk) {
            MessageDialog.build()
                    .setTitle("更新完成")
                    .setMessage("检测到当前环境是 Magisk ,是否安装到未使用的卡槽(保留 Magisk)")
                    .setOkButton("重启",(baseDialog, v) -> {
                        reboot();
                        return false;
                    })
                    .setCancelButton("安装",(baseDialog, v) -> {
                        activity.getASynHandler().post(this::patchMagisk);
                        return false;
                    })
                    .setOtherButton("取消")
                    .show();
        } else if (activity.uUpdateServiceManager.GetKsuVersion() > 0) {
            MessageDialog.build()
                    .setTitle("更新完成")
                    .setMessage("检测到当前环境是 KernelSu ,是否安装到未使用的卡槽(保留 KernelSu)\n\n注意：因为版本的更新，可能内核的变动很大，可能保留更新会无法开机。")
                    .setOkButton("重启",(baseDialog, v) -> {
                        reboot();
                        return false;
                    })
                    .setCancelButton("安装",(baseDialog, v) -> {
                        activity.getASynHandler().post(this::patchKernelSU);
                        return false;
                    })
                    .setOtherButton("取消")
                    .show();
        } else {
            showRebootDialog("更新完成","更新完成，是否立即重启手机");
        }

    }

    private void showRebootDialog(String title,String message) {
        WaitDialog.dismiss();
        MessageDialog.build()
                .setTitle(title)
                .setMessage(message)
                .setOkButton("重启手机",(baseDialog, v) -> {
                    baseDialog.dismiss();
                    reboot();
                    return false;
                })
                .setCancelButton("稍后重启")
                .show();
    }

    private void showErrorDialog(String error) {
        WaitDialog.dismiss();
        MessageDialog.build()
                .setTitle("修补失败")
                .setMessage("运行本机内的修补脚本失败，是否查看错误信息")
                .setOkButton("重启",(baseDialog, v) -> {
                    reboot();
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
