package me.yowal.updatehelper.activity;

import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.ejlchina.okhttps.OkHttps;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.kongzue.dialogx.dialogs.BottomDialog;
import com.kongzue.dialogx.dialogs.BottomMenu;
import com.kongzue.dialogx.dialogs.FullScreenDialog;
import com.kongzue.dialogx.interfaces.OnBindView;
import com.kongzue.dialogx.interfaces.OnDialogButtonClickListener;
import com.kongzue.dialogx.interfaces.OnIconChangeCallBack;
import com.kongzue.filedialog.interfaces.FileSelectCallBack;
import com.topjohnwu.superuser.Shell;
import com.topjohnwu.superuser.ShellUtils;
import com.topjohnwu.superuser.ipc.RootService;
import com.topjohnwu.superuser.nio.ExtendedFile;

import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import me.yowal.updatehelper.Config;
import me.yowal.updatehelper.R;
import me.yowal.updatehelper.bean.UpdateInfo;
import me.yowal.updatehelper.databinding.ActivityMainBinding;
import me.yowal.updatehelper.databinding.DialogInputBinding;
import me.yowal.updatehelper.interfaces.IUpdateCallback;
import me.yowal.updatehelper.manager.SuFileManager;
import me.yowal.updatehelper.manager.UpdateServiceManager;
import me.yowal.updatehelper.proxy.UpdateEngineProxy;
import me.yowal.updatehelper.service.IUpdateService;
import me.yowal.updatehelper.service.UpdateService;
import me.yowal.updatehelper.utils.FileDialogUtils;
import me.yowal.updatehelper.utils.FileUtils;
import me.yowal.updatehelper.utils.FlashUtils;
import me.yowal.updatehelper.utils.HtmlUtils;
import me.yowal.updatehelper.utils.IOUtils;
import me.yowal.updatehelper.utils.InputDialogUtils;
import me.yowal.updatehelper.utils.LogUtils;
import me.yowal.updatehelper.utils.NotificationUtils;
import me.yowal.updatehelper.utils.TouchFeedUtils;
import me.yowal.updatehelper.utils.Utils;
import me.yowal.updatehelper.widget.ProgressButton;

public class MainActivity extends BaseActivity {

    private ActivityMainBinding binding;

    private TouchFeedUtils aTouchFeedUtils;

    private NotificationManager aNotificationManager;

    private int ksuVersion = -1;

    private String aInstallDir;

    private UpdateInfo aUpdateInfo;

    private final TouchFeedUtils.OnFeedBackListener onFeedBackListener = new TouchFeedUtils.OnFeedBackListener() {
        @Override
        public void onClick(View view) {
            int id = view.getId();
            if (id == R.id.about_blog) {
                Utils.openUrl(aContext, "https://www.yowal.cn");
            } else if (id == R.id.about_email) {
                Utils.sendEmail(aContext, "i@yowal.cn" );
            } else if (id == R.id.about_developer) {
                Utils.openUrl(aContext, "http://www.coolapk.com/u/1010404");
            } else if (id == R.id.about_xiaoqian) {
                Utils.openUrl(aContext, "http://www.coolapk.com/u/621284");
            } else if (id == R.id.about_shiqi) {
                Utils.openUrl(aContext, "http://www.coolapk.com/u/27248277");
            } else if (id == R.id.about_lumyuan) {
                Utils.openUrl(aContext, "http://www.coolapk.com/u/2073264");
            } else if (id == R.id.about_yege) {
                Utils.openUrl(aContext, "http://www.coolapk.com/u/377020");
            }
        }
        @Override
        public void onLongClick(View view) {}
    };

    private final IUpdateCallback engineCallback = new IUpdateCallback.Stub() {
        @Override
        public void onPayloadApplicationComplete(int error_code) {
            //activity.uUpdateServiceManager.closeAssetFileDescriptor();
            if (error_code == UpdateEngineProxy.ErrorCodeConstants.SUCCESS) {
                boolean hasDisplayid = !TextUtils.isEmpty(aUpdateInfo.getDisplayid());
                aNotificationManager.notify(1, NotificationUtils.notifyMsg(aContext, hasDisplayid ? aUpdateInfo.getDisplayid() : "重启手机即可完成更新",  hasDisplayid ? "重启手机即可完成更新" : "恭喜你，更新成功了"));
            } else {
                showErrorDialog("更新失败!","哎呀，开了个小差，更新失败了，错误代号：" + error_code );
                aNotificationManager.notify(1, NotificationUtils.notifyMsg(aContext,"请稍后重试，或联系开发者反馈","哎呀，开了个小差，更新失败了，错误代号：" + error_code));
            }
        }

        @Override
        public void onStatusUpdate(int status_code, float percentage) {
            if (status_code == UpdateEngineProxy.UpdateStatusConstants.DOWNLOADING) {
                float progress = (percentage * 100.f);
                if (binding.buttonProgress.getState() != ProgressButton.STATE_INSTALL)
                    binding.buttonProgress.setState(ProgressButton.STATE_INSTALL);
                binding.buttonProgress.setProgressText("正在更新", progress);
                if (TextUtils.isEmpty(aUpdateInfo.getDisplayid())) {
                    aNotificationManager.notify(1, NotificationUtils.notifyProgress(aContext,"正在更新系统","系统更新",100, (int)progress,false));
                } else {
                    aNotificationManager.notify(1, NotificationUtils.notifyProgress(aContext, aUpdateInfo.getDisplayid(),"系统正在更新",100, (int)progress,false));
                }
            } else if (status_code == UpdateEngineProxy.UpdateStatusConstants.VERIFYING || status_code == UpdateEngineProxy.UpdateStatusConstants.FINALIZING) {
                if (binding.buttonProgress.getState() != ProgressButton.STATE_FINISH)
                    binding.buttonProgress.setState(ProgressButton.STATE_FINISH);
                binding.buttonProgress.setCurrentText("正在校验分区数据");
                aNotificationManager.notify(1, NotificationUtils.notifyProgress(aContext,"正在校验分区数据","系统正在更新",0, 0,true));
            } else if (status_code == UpdateEngineProxy.UpdateStatusConstants.UPDATED_NEED_REBOOT) {
                flashFinish();
                if (!FlashUtils.modifyPrivate(aInstallDir)) {
                    LogUtils.e("onStatusUpdate", "modifyPrivate fail");
                    UpdateServiceManager.getInstance().cancel();
                    showErrorDialog("温馨提示","更新失败!" );
                    aNotificationManager.notify(1, NotificationUtils.notifyMsg(aContext,"请稍后重试，或联系开发者反馈","哎呀，开了个小差，更新失败了"));
                    return;
                }
                LogUtils.d("onStatusUpdate", "ShowUpdateSuccessDialog");
                updateSuccess();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        //替换 ToolBar
        setSupportActionBar(binding.toolBar);

        //点击效果
        aTouchFeedUtils = TouchFeedUtils.getInstance(this);

        //通知管理
        aNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        //私有目录
        aInstallDir = getFilesDir().toString();

        //获取系统信息
        initSystemInfo();

        //判断Root权限
        Shell.getShell(shell -> {
            if (shell.isRoot()) {
                binding.homeNoRoot.setVisibility(View.GONE);
                binding.buttonFlash.setVisibility(View.VISIBLE);
                try {
                    Shell.cmd("rm -r " + getFilesDir().toString()).exec();
                    Shell.cmd("mkdir " + getFilesDir().toString()).exec();
                    FileUtils.copyToFile(getAssets().open("magiskboot"), new File(getFilesDir(),"magiskboot"));
                    FileUtils.copyToFile(getResources().openRawResource(R.raw.apatch), new File(getFilesDir(),"apatch.sh"));
                    Shell.cmd("chmod -R 777 " + getFilesDir().toString()).exec();
                } catch (IOException e) {
                    LogUtils.e("CheckRoot", e.getLocalizedMessage());
                }
                bindRootService();
            } else {
                binding.buttonFlash.setVisibility(View.GONE);
                binding.homeNoRoot.setVisibility(View.VISIBLE);
                binding.infoRootType.setText(String.format("Root实现：%s", "None"));
            }
        });

        //注册 关于 事件
        binding.buttonAbout.setOnClickListener(v -> FullScreenDialog.show(new OnBindView<FullScreenDialog>(R.layout.dialog_about) {
            @Override
            public void onBind(FullScreenDialog dialog, View v) {
                v.<AppCompatTextView>findViewById(R.id.about_app_name).setText(String.format("%s (%s)", ContextCompat.getString(aContext, R.string.app_name), Utils.getVerName(aContext)));

                aTouchFeedUtils.setOnFeedBackListener(onFeedBackListener, v.findViewById(R.id.about_blog));

                aTouchFeedUtils.setOnFeedBackListener(onFeedBackListener, v.findViewById(R.id.about_developer));
                aTouchFeedUtils.setOnFeedBackListener(onFeedBackListener, v.findViewById(R.id.about_email));

                aTouchFeedUtils.setOnFeedBackListener(onFeedBackListener, v.findViewById(R.id.about_shiqi));
                aTouchFeedUtils.setOnFeedBackListener(onFeedBackListener, v.findViewById(R.id.about_xiaoqian));
                aTouchFeedUtils.setOnFeedBackListener(onFeedBackListener, v.findViewById(R.id.about_yege));
                aTouchFeedUtils.setOnFeedBackListener(onFeedBackListener, v.findViewById(R.id.about_lumyuan));
            }
        }));

        NotificationManagerCompat notification = NotificationManagerCompat.from(this);

        //注册刷写按钮
        binding.buttonFlash.setOnClickListener(v -> {
            if (UpdateServiceManager.getInstance().getService() == null) {
                showErrorDialog("温馨提示","服务未成功启动，请重试~");
                return;
            }

            if (!UpdateServiceManager.getInstance().isValid()) {
                showErrorDialog("温馨提示","当前设备无法找到服务，请联系开发者尝试解决~");
                return;
            }

            //判断通知权限是否打开
            if (!notification.areNotificationsEnabled()) {
                Intent intent = new Intent();
                intent.setAction("android.settings.APP_NOTIFICATION_SETTINGS");
                intent.putExtra("android.provider.extra.APP_PACKAGE", getPackageName());
                startActivity(intent);
                return;
            }

            NotificationChannel channel = new NotificationChannel("new_version_push", "新版本推送", NotificationManager.IMPORTANCE_LOW);
            aNotificationManager.createNotificationChannel(channel);
            FileDialogUtils FileDialogBuild = FileDialogUtils.build();
            FileDialogBuild.setSuffixArray(new String[]{".zip"});
            FileDialogBuild.selectFile(new FileSelectCallBack() {
                @Override
                public void onSelect(File file, String filePath) {
                    new MaterialAlertDialogBuilder(aContext)
                            .setTitle("选择文件?")
                            .setCancelable(false)
                            .setMessage(filePath)
                            .setPositiveButton("开始更新", (dialog, which) -> {
                                flashStart(ProgressButton.STATE_FINISH, "准备更新");
                                getASynHandler().postDelayed(() -> startUpdate(filePath), 500);
                            })
                            .setNegativeButton("取消更新", null)
                            .create().show();
                }
            });
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar_memu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.memu_reboot) {
            showRebootDialog(binding.toolBar);
        }
        return true;
    }

    private void showRebootDialog(View view) {
        PopupMenu popupMenu = new PopupMenu(aContext, view);
        popupMenu.setGravity(Gravity.TOP | Gravity.END);
        popupMenu.getMenuInflater().inflate(R.menu.memu_reboot, popupMenu.getMenu());
        popupMenu.show();
        popupMenu.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.memu_reboot) {
                Utils.reboot();
            }
            else if (id == R.id.memu_reboot_recovery) {
                Utils.reboot("recovery");
            }
            else if (id == R.id.memu_reboot_bootloader) {
                Utils.reboot("bootloader");
            }
            else if (id == R.id.memu_reboot_download) {
                Utils.reboot("download");
            }
            else if (id == R.id.memu_reboot_edl) {
                Utils.reboot("edl");
            }
            return true;
        });
    }

    private void initSystemInfo() {
        binding.homeTipsMain.setText(HtmlUtils.toClickableHtml(aContext, ContextCompat.getString(this, R.string.string_home_tips_main)));
        binding.homeTipsMain.setMovementMethod(LinkMovementMethod.getInstance());

        binding.infoSystemBrand.setText(String.format("设备品牌：%s", Build.BRAND));
        binding.infoSystemModel.setText(String.format("设备型号：%s", Build.MODEL));
        binding.infoAndroidVersion.setText(String.format("Android版本：Android %s (%s)", Build.VERSION.RELEASE,Build.VERSION.SDK_INT));
        binding.infoRootType.setText(String.format("Root实现：%s", "获取中..."));
        if (Config.isVab)
            binding.infoActionSolt.setText(String.format("活动分区：%s", Config.currentSlot.replaceAll("_","").toUpperCase(Locale.ROOT)));
        else
            binding.infoActionSolt.setText(String.format("活动分区：%s", ContextCompat.getString(this, R.string.string_a_only)));
    }

    private void bindRootService() {
        Intent intent = new Intent(aContext, UpdateService.class);
        RootService.bind(intent, new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName name, IBinder service) {
                IUpdateService mUpdateService = IUpdateService.Stub.asInterface(service);
                if (mUpdateService != null) {
                    UpdateServiceManager.getInstance().init(mUpdateService);
                    SuFileManager.getInstance().init(UpdateServiceManager.getInstance().getFileSystemManager());
                    initRootType();
                    initPassFunctionOffset();
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {

            }
        });
    }

    private void initRootType() {
        ksuVersion = UpdateServiceManager.getInstance().GetKsuVersion();
        String magiskVersion = ShellUtils.fastCmd("magisk -V");
        String apatchVersion = ShellUtils.fastCmd("cat /data/adb/ap/version");
        if (ksuVersion > 0)
            binding.infoRootType.setText(String.format("Root实现：%s (%s)", "KernelSU", ksuVersion));
        else if (!TextUtils.isEmpty(magiskVersion))
            binding.infoRootType.setText(String.format("Root实现：%s (%s)", "Magsik", magiskVersion));
        else if (!TextUtils.isEmpty(apatchVersion))
            binding.infoRootType.setText(String.format("Root实现：%s (%s)", "APatch", apatchVersion));
        else
            binding.infoRootType.setText(String.format("Root实现：%s", "None"));
    }

    private void initPassFunctionOffset() {
        getASynHandler().post(() -> {
            SharedPreferences update_engine_data = getSharedPreferences("update_engine_data", Activity.MODE_PRIVATE);
            String fileMd5 = update_engine_data.getString("md5","");
            int offset = update_engine_data.getInt("offset",-1);
            String newMd5 = FileUtils.getFileMD5("/system/bin/update_engine");
            //判断文件是否相等
            if (!fileMd5.equals(newMd5) || offset <= 0) {
                update_engine_data.edit().putString("md5", newMd5).apply();
                LogUtils.d("initPassFunctionOffset", "fileMd5 = " + newMd5);
                offset = UpdateServiceManager.getInstance().findValidateSourceHash();
                //update_engine_data.edit().putInt("offset", offset).apply();
                LogUtils.d("initPassFunctionOffset", "offset = 0x" + Integer.toString(offset,16));
            }
            UpdateServiceManager.getInstance().passValidateSourceHash(getApplicationInfo().nativeLibraryDir + "/libinject.so", offset);
        });
    }

    private void flashFinish() {
        getMainHandler().post(() -> {
            binding.buttonFlash.setVisibility(View.VISIBLE);
            binding.buttonProgress.setVisibility(View.GONE);
        });
    }

    private void flashStart(int state, String text) {
        getMainHandler().post(() -> {
            binding.buttonFlash.setVisibility(View.GONE);
            binding.buttonProgress.setVisibility(View.VISIBLE);
            binding.buttonProgress.setState(state);
            binding.buttonProgress.setCurrentText(text);
        });
    }

    private void flashStart() {
        getMainHandler().post(() -> {
            binding.buttonFlash.setVisibility(View.GONE);
            binding.buttonProgress.setVisibility(View.VISIBLE);
        });
    }

    private void showErrorDialog(String title, CharSequence message) {
        flashFinish();
        getMainHandler().post(() -> new MaterialAlertDialogBuilder(aContext)
                .setTitle(title)
                .setCancelable(false)
                .setMessage(message)
                .setPositiveButton("知道了", null)
                .create().show());

    }

    private void showErrorDialog(String error) {
        flashFinish();
        getMainHandler().post(() -> new MaterialAlertDialogBuilder(aContext)
                .setTitle("修补失败")
                .setCancelable(false)
                .setMessage("运行修补脚本失败，是否查看错误信息")
                .setPositiveButton("重启", (dialog, which) -> Utils.reboot())
                .setNegativeButton("查看", (dialog, which) -> showErrorDialog("错误信息", error))
                .setNeutralButton("取消",null)
                .create().show());
    }

    private void showRebootDialog(String title,String message) {
        flashFinish();
        getMainHandler().post(() -> new MaterialAlertDialogBuilder(aContext)
                .setTitle(title)
                .setCancelable(false)
                .setMessage(message)
                .setPositiveButton("重启", (dialog, which) -> Utils.reboot())
                .setNegativeButton("稍后重启", null)
                .create().show());
    }

    private void startUpdate(String File) {
        aUpdateInfo = UpdateServiceManager.getInstance().pareZip(File);
        if (aUpdateInfo.getHeaderKeyValuePairs() == null || aUpdateInfo.getHeaderKeyValuePairs().length == 0) {
            showErrorDialog("更新失败","更新包不完整，请重新下载");
        } else if (aUpdateInfo.getType() != -1 && aUpdateInfo.getType() != 1) {
            showErrorDialog("更新失败","更新包非全量包，请下载全量包");
        }
        else {
            //如果是魅族的就判断下型号吧
            if (!TextUtils.isEmpty(aUpdateInfo.getFlymeid()) && !aUpdateInfo.getFlymeid().equals(Config.flymemodel)) {
                new MaterialAlertDialogBuilder(aContext)
                        .setTitle("温馨提示")
                        .setCancelable(false)
                        .setMessage("检测到选择的全量包是: " + aUpdateInfo.getBuildInfo() + ", 与您的设备不符，是否继续更新")
                        .setPositiveButton("继续", (dialog, which) -> {
                            if (!UpdateServiceManager.getInstance().startUpdateSystem(aUpdateInfo, engineCallback)){
                                showErrorDialog("更新失败","更新服务错误，请重试");
                            }
                        })
                        .setNegativeButton("取消",null)
                        .create().show();
                return;
            }

            if (!UpdateServiceManager.getInstance().startUpdateSystem(aUpdateInfo, engineCallback)){
                showErrorDialog("更新失败","更新服务错误，请重试");
            }
        }
    }

    private void updateSuccess() {
        BottomMenu.show("更新成功", "恭喜你看到我了，现在轮到你选择保留 Root 的方式了，如果需要，那就请在选项中选择一个吧。\n\n注意了：是选择里面的哦，不是点击按钮哦~", new String[]{"Magisk", "KernelSu", "APatch"})
                .setOnIconChangeCallBack(new OnIconChangeCallBack<BottomMenu>(true) {
                    @Override
                    public int getIcon(BottomMenu bottomMenu, int index, String menuText) {
                        switch (index) {
                            case 0:
                                return R.drawable.ic_launcher_magisk;
                            case 1:
                                return R.drawable.ic_launcher_kernelsu;
                            case 2:
                                return R.drawable.ic_launcher_apatch;
                        }
                        return 0;
                    }
                })
                .setOnMenuItemClickListener((dialog, text, index) -> {
                    dialog.dismiss();
                    flashStart();
                    switch (index) {
                        case 0:
                            getASynHandler().post(this::patchMagisk);
                            return false;
                        case 1:
                            getASynHandler().post(this::patchKernelSU);
                            return false;
                        case 2:
                            getMainHandler().post(() -> InputDialogUtils.show(aContext, "设定超级密钥", "内核补丁的唯一密钥，密钥长度最少8位数，且最少含有一位字母", new InputDialogUtils.OnInputClickListener() {
                                @Override
                                public boolean onClick(DialogInterface dialog, DialogInputBinding bindingiput, boolean isCancel) {
                                    if (isCancel) {
                                        flashFinish();
                                        return false;
                                    }
                                    String inputStr = bindingiput.mEditText.getText().toString();
                                    if (TextUtils.isEmpty(inputStr)) {
                                        toast("请输入超级密钥！");
                                        return true;
                                    }

                                    if (inputStr.length() < 8) {
                                        toast("输入的超级密钥长度小于8");
                                        return true;
                                    }

                                    if (!Utils.keyChecked(inputStr)) {
                                        toast("超级密钥格式错误，请检查是否只含有数字和字母");
                                        return true;
                                    }
                                    getASynHandler().post(() -> patchAPatch(inputStr));
                                    return false;
                                }
                            }));
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

    private void patchAPatch(String SuperKey) {

        binding.buttonProgress.setState(ProgressButton.STATE_FINISH);
        binding.buttonProgress.setCurrentText("开始安装 APatch");

        // 这里使用 Github 接口，获取 releases 最新版本号
        String rep = OkHttps.sync("http://kpatch.oss-cn-shenzhen.aliyuncs.com/latest.json")
                .get()
                .getBody().toString();


        String lastTag = "";
        try {
            JSONObject jsonObject = new JSONObject(rep);
            lastTag = jsonObject.getString("tag_name");
        } catch (Exception e) {
            LogUtils.e("patchAPatch", e.getLocalizedMessage());
        }
        if (TextUtils.isEmpty(lastTag)) {
            showRebootDialog("安装失败","APatch 版本获取失败，请自行操作");
            return;
        }

        FileUtils.delete(aInstallDir + "/kpimg");

        OkHttps.sync("http://kpatch.oss-cn-shenzhen.aliyuncs.com/" + lastTag + "/kpimg")
                .get()
                .getBody()
                .toFile(aInstallDir + "/kpimg")
                .start();

        FileUtils.delete(aInstallDir + "/kptools");

        OkHttps.sync("http://kpatch.oss-cn-shenzhen.aliyuncs.com/" + lastTag + "/kptools")
                .get()
                .getBody()
                .toFile(aInstallDir + "/kptools")
                .start();

        FileUtils.delete(aInstallDir + "/kpatch");
        OkHttps.sync("http://kpatch.oss-cn-shenzhen.aliyuncs.com/" + lastTag + "/kpatch")
                .get()
                .getBody()
                .toFile(aInstallDir + "/kpatch")
                .start();

        ShellUtils.fastCmd("chmod -R 777 " + aInstallDir);

        String[] envList = new String[]{"kptools", "magiskboot", "kpimg", "kpatch"};
        for (String file: envList) {
            if (!new File(aInstallDir + "/" + file).exists()) {
                showRebootDialog("修补失败",file + " 文件不存在，请自行修补");
                return;
            }
        }

        // 读取当前分区，用来判断第二分区
        String slot = Config.currentSlot.equals("_a") ? "_b" : "_a";

        // 提取第二分区的boot镜像
        String srcBoot = "/dev/block/bootdevice/by-name/boot" + slot;

        FileUtils.delete(aInstallDir + "/boot.img");
        FileUtils.delete(aInstallDir + "/apatch_patch.img");

        if (!FlashUtils.extract_image(srcBoot,aInstallDir + "/boot.img")) {
            showRebootDialog("安装失败","镜像分区提取错误，请自行操作");
            return;
        }

        List<String> stdout = new ArrayList<>();
        // 使用面具自带的脚本进行修补
        boolean isSuccess = Shell.getShell().newJob()
                .add("cd " + aInstallDir)
                .add("sh apatch.sh " + SuperKey + " " + aInstallDir + "/boot.img -K kpatch")
                .to(stdout, stdout)
                .exec()
                .isSuccess();
        if (!isSuccess) {
            showErrorDialog(String.join("\n", stdout));
            return;
        }

        Shell.getShell().newJob().add("./magiskboot cleanup", "mv ./new-boot.img " + aInstallDir + "/apatch_patch.img", "rm ./stock_boot.img", "cd /").exec();

        if (!FlashUtils.flash_image(aInstallDir + "/apatch_patch.img", srcBoot)) {
            showRebootDialog("安装失败","刷入镜像失败，请自行操作");
            return;
        }
        showRebootDialog("安装成功","安装到未使用卡槽完成");
    }

    private void patchKernelSU() {

        binding.buttonProgress.setState(ProgressButton.STATE_FINISH);
        binding.buttonProgress.setCurrentText("开始安装 KernelSU");

        boolean isLkmMode = UpdateServiceManager.getInstance().KsuIsLkmMode();

        // 读取当前分区，用来判断第二分区
        String next_slot = Config.currentSlot.equals("_a") ? "_b" : "_a";

        // 非 LKM 模式直接提取当前分区刷入第二分区
        if (!isLkmMode && !FlashUtils.flash_image("/dev/block/bootdevice/by-name/boot" + Config.currentSlot, "/dev/block/bootdevice/by-name/boot" + next_slot)) {
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

        ShellUtils.fastCmd("rm -r " + aInstallDir + "/*.img");

        if (!FlashUtils.extract_image(srcBoot, aInstallDir + "/boot.img")) {
            showRebootDialog("安装失败","镜像分区提取错误，请自行操作");
            return;
        }


        List<String> stdout = new ArrayList<>();
        boolean isSuccess = Shell.getShell().newJob()
                .add("cd " + aInstallDir)
                .add("/data/adb/ksud boot-patch --magiskboot " + aInstallDir +  "/magiskboot -b " + aInstallDir + "/boot.img")
                .to(stdout, stdout)
                .exec()
                .isSuccess();
        if (!isSuccess) {
            showErrorDialog(String.join("\n", stdout));
            return;
        }


        String patch_img = ShellUtils.fastCmd("cd " + aInstallDir + " & ls kernelsu_*.img");
        if (TextUtils.isEmpty(patch_img)) {
            showRebootDialog("安装失败","获取修补文件错误，请自行操作");
            return;
        }

        if (!FlashUtils.flash_image(aInstallDir + "/" + patch_img, srcBoot)) {
            showRebootDialog("安装失败","刷入镜像失败，请自行操作");
            return;
        }

        showRebootDialog("安装成功","安装到未使用卡槽完成");
    }

    private void patchMagisk() {

        binding.buttonProgress.setState(ProgressButton.STATE_FINISH);
        binding.buttonProgress.setCurrentText("开始安装 Magisk");

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
                IOUtils.copy(getAssets().open("stub.apk"), out);
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

        FileUtils.delete(aInstallDir + "/boot.img");
        FileUtils.delete(aInstallDir + "/magisk_patch.img");

        if (!FlashUtils.extract_image(srcBoot, aInstallDir + "/boot.img")) {
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
                        "sh boot_patch.sh " + aInstallDir + "/boot.img")
                .to(stdout, stdout)
                .exec()
                .isSuccess();
        if (!isSuccess) {
            showErrorDialog(String.join("\n", stdout));
            return;
        }
        Shell.getShell().newJob().add("./magiskboot cleanup", "mv ./new-boot.img " + aInstallDir + "/magisk_patch.img", "rm ./stock_boot.img", "cd /").exec();
        if (!FlashUtils.flash_image(aInstallDir + "/magisk_patch.img", srcBoot)) {
            showRebootDialog("安装失败","刷入镜像失败，请自行操作");
            return;
        }
        showRebootDialog("安装成功","安装到未使用卡槽完成");
    }

}