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
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

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
import me.yowal.updatehelper.utils.AssetsUtils;
import me.yowal.updatehelper.utils.FileDialogUtils;
import me.yowal.updatehelper.utils.FileUtils;
import me.yowal.updatehelper.utils.FlashUtils;
import me.yowal.updatehelper.utils.HtmlUtils;
import me.yowal.updatehelper.utils.IOUtils;
import me.yowal.updatehelper.utils.InputDialogUtils;
import me.yowal.updatehelper.utils.LogUtils;
import me.yowal.updatehelper.utils.NotificationUtils;
import me.yowal.updatehelper.utils.PatchUtils;
import me.yowal.updatehelper.utils.RestoreUtils;
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

    private PatchUtils aPatchUtils;

    private RestoreUtils aRestoreUtils;

    private boolean supportOta = false;

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
                if (supportOta && aUpdateInfo.getType() == 1) {
                    aRestoreUtils.RestoreFlash();
                }
                showDialog("更新失败!","哎呀，开了个小差，更新失败了，错误代号：" + error_code);
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
                    showDialog("温馨提示","更新失败!" );
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
        aInstallDir = getFilesDir().getAbsolutePath();

        //获取系统信息
        initSystemInfo();

        //判断Root权限
        Shell.getShell(shell -> {
            if (shell.isRoot()) {
                binding.homeNoRoot.setVisibility(View.GONE);
                binding.buttonFlash.setVisibility(View.VISIBLE);
                Shell.cmd("rm -r " + aInstallDir).exec();
                Shell.cmd("mkdir " + aInstallDir).exec();

                AssetsUtils.writeFile(aContext, "magiskboot", new File(aInstallDir, "magiskboot"));
                AssetsUtils.writeFile(aContext, R.raw.apatch_patch, new File(aInstallDir,"apatch_patch.sh"));

                Shell.cmd("chmod -R 755 " + aInstallDir).exec();

                bindRootService();
            } else {
                binding.buttonFlash.setVisibility(View.GONE);
                binding.homeNoRoot.setVisibility(View.VISIBLE);
                binding.infoRootType.setText(String.format("Root实现：%s", "None"));
                binding.infoSupportOta.setText(String.format("OTA更新：%s", "None"));
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
                showDialog("温馨提示","服务未成功启动，请重试~");
                return;
            }

            if (!UpdateServiceManager.getInstance().isValid()) {
                showDialog("温馨提示","当前设备无法找到服务，请联系开发者尝试解决~");
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
                                getASynHandler().post(() -> readyUpdate(filePath));
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

        binding.infoSupportOta.setText(String.format("OTA更新：%s", "获取中..."));
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
                    initRootAndOta();
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName name) {

            }
        });
    }

    private void initRootAndOta() {
        ksuVersion = UpdateServiceManager.getInstance().GetKsuVersion();
        String magiskVersion = ShellUtils.fastCmd("magisk -V");
        String apatchVersion = ShellUtils.fastCmd("cat /data/adb/ap/version");
        if (ksuVersion > 0)
            binding.infoRootType.setText(String.format("Root实现：%s (%s)", "KernelSU", ksuVersion));
        else if (!TextUtils.isEmpty(magiskVersion))
            binding.infoRootType.setText(String.format("Root实现：%s (%s)", "Magisk", magiskVersion));
        else if (!TextUtils.isEmpty(apatchVersion))
            binding.infoRootType.setText(String.format("Root实现：%s (%s)", "APatch", apatchVersion));
        else
            binding.infoRootType.setText(String.format("Root实现：%s", "None"));

        //这里直接判断是否Lkm模式，这个只会在Ksu环境下才能读取出来
        if (UpdateServiceManager.getInstance().KsuIsLkmMode())
            supportOta = true;
        else
            supportOta = !binding.infoRootType.getText().toString().contains("None");

        if (Config.isVab && supportOta)
            binding.infoSupportOta.setText(String.format("OTA更新：%s", "支持"));
        else
            binding.infoSupportOta.setText(String.format("OTA更新：%s", "不支持"));

        aPatchUtils = new PatchUtils(aContext, aInstallDir);
        aRestoreUtils = new RestoreUtils(aContext, aInstallDir);
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
        runOnUiThread(() -> {
            binding.buttonFlash.setVisibility(View.VISIBLE);
            binding.buttonProgress.setVisibility(View.GONE);
        });
    }

    private void flashStart(int state, String text) {
        runOnUiThread(() -> {
            binding.buttonFlash.setVisibility(View.GONE);
            binding.buttonProgress.setVisibility(View.VISIBLE);
            if (state > -1)
                binding.buttonProgress.setState(state);
            if (!TextUtils.isEmpty(text))
                binding.buttonProgress.setCurrentText(text);
        });
    }

    private MaterialAlertDialogBuilder CreateMaterialAlertDialogBuilder() {
        flashFinish();
        return new MaterialAlertDialogBuilder(aContext);
    }

    private void showDialog(String title, CharSequence message) {
        runOnUiThread(() -> CreateMaterialAlertDialogBuilder()
                .setTitle(title)
                .setCancelable(false)
                .setMessage(message)
                .setPositiveButton("知道了", null)
                .create().show());
    }

    private void showPatchErrorDialog(String error) {
        runOnUiThread(() -> CreateMaterialAlertDialogBuilder()
                .setTitle("修补失败")
                .setCancelable(false)
                .setMessage("运行修补脚本失败，是否查看错误信息")
                .setPositiveButton("重启", (dialog, which) -> Utils.reboot())
                .setNegativeButton("查看", (dialog, which) -> showDialog("错误信息", error))
                .setNeutralButton("取消",null)
                .create().show());
    }

    private void showRebootDialog(String title,String message) {
        runOnUiThread(() -> CreateMaterialAlertDialogBuilder()
                .setTitle(title)
                .setCancelable(false)
                .setMessage(message)
                .setPositiveButton("重启", (dialog, which) -> Utils.reboot())
                .setNegativeButton("稍后重启", null)
                .create().show());
    }

    private void readyUpdate(String File) {
        aUpdateInfo = UpdateServiceManager.getInstance().pareZip(File);
        if (aUpdateInfo.getHeaderKeyValuePairs() == null || aUpdateInfo.getHeaderKeyValuePairs().length == 0) {
            showDialog("温馨提示","更新包不完整，请重新下载");
            return;
        }
        // 增量包
        if (aUpdateInfo.getType() == 1 && supportOta) {
            shouldResoteBoot();
            return;
        }
        getASynHandler().postDelayed(this::startUpdate, 500);
    }


    private void startUpdate() {
        flashStart(ProgressButton.STATE_FINISH, "准备更新");
        if (!UpdateServiceManager.getInstance().startUpdateSystem(aUpdateInfo, engineCallback)){
            showDialog("更新失败","更新服务错误，请重试");
        }
    }


    private void shouldResoteBoot() {
        flashStart(ProgressButton.STATE_FINISH, "准备还原");
        BottomMenu.show("还原提示", "检测到你选择的 ROM 包可能为增量包，需要还原镜像才能更新，如果需要，请在选项中选择当前 Root 环境。\n\n注意了：是选择里面的哦，不是点击按钮哦~", new String[]{"Magisk", "KernelSU", "APatch"})
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
                .setCancelable(false)
                .setOnMenuItemClickListener((dialog, text, index) -> {
                    getASynHandler().postDelayed(() -> restoreBoot(text.toString()), 500);
                    return false;
                })
                .setOkButton("不还原，继续更新", (OnDialogButtonClickListener<BottomDialog>) (dialog, v) -> {
                    getASynHandler().postDelayed(this::startUpdate, 500);
                    return false;
                })
                .setCancelButton("退出更新", (OnDialogButtonClickListener<BottomDialog>) (dialog, v) -> {
                    flashFinish();
                    return false;
                });
    }

    private void restoreBoot(String rootType) {
        flashStart(ProgressButton.STATE_FINISH, "开始还原 " + rootType);
        PatchUtils.Result reslut = new PatchUtils.Result(PatchUtils.ErrorCode.OTHER_ERROR,"错误类型");
        switch (rootType) {
            case "Magisk":
                reslut = aRestoreUtils.restoreMagisk();
                break;
            case "KernelSU":
                reslut = aRestoreUtils.restoreKernelSU();
                break;
            case "APatch":
                reslut = aRestoreUtils.restoreAPatch();
                break;
        }
        if (reslut.errorCode == PatchUtils.ErrorCode.SUCCESS) {
            startUpdate();
            return;
        }
        if (reslut.errorCode == PatchUtils.ErrorCode.EXEC_ERROR)
            showPatchErrorDialog(reslut.errorMessage);
        else
            showRebootDialog("还原失败", reslut.errorMessage);
    }

    private void updateSuccess() {
        BottomMenu.show("更新成功", "恭喜你看到我了，现在轮到你选择保留 Root 的方式了，如果需要，请在选项中选择当前 Root 环境。\n\n注意了：是选择里面的哦，不是点击按钮哦~", new String[]{"Magisk", "KernelSU", "APatch"})
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
                .setCancelable(false)
                .setOnMenuItemClickListener((dialog, text, index) -> {
                    dialog.dismiss();
                    flashStart(-1,null);
                    if (index == 2) {
                        getMainHandler().post(() -> InputDialogUtils.show(aContext, "设定超级密钥", "内核补丁的唯一密钥，密钥长度最少8位数，且最少含有一位字母", (dialog1, bindingiput, isCancel) -> {
                            if (isCancel) {
                                flashFinish();
                                return false;
                            }
                            String inputStr = bindingiput.mEditText.getText().toString();
                            if (inputStr.length() < 8) {
                                toast("输入的超级密钥长度小于8");
                                return true;
                            }
                            if (!Utils.keyChecked(inputStr)) {
                                toast("超级密钥格式错误，请检查是否只含有数字和字母");
                                return true;
                            }
                            getASynHandler().post(() -> patchBoot(text.toString(),inputStr));
                            return false;
                        }));
                    } else {
                        getASynHandler().post(() -> patchBoot(text.toString(),""));
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

    private void patchBoot(String rootType, String SuperKey) {
        binding.buttonProgress.setState(ProgressButton.STATE_FINISH);
        binding.buttonProgress.setCurrentText("开始安装 " + rootType);
        PatchUtils.Result reslut = new PatchUtils.Result(PatchUtils.ErrorCode.OTHER_ERROR,"错误类型");
        switch (rootType) {
            case "Magisk":
                reslut = aPatchUtils.patchMagisk();
                break;
            case "KernelSU":
                reslut = aPatchUtils.patchKernelSU();
                break;
            case "APatch":
                reslut = aPatchUtils.patchAPatch(SuperKey);
                break;
        }
        if (reslut.errorCode == PatchUtils.ErrorCode.SUCCESS)
            showRebootDialog("安装成功","安装到未使用卡槽完成");
        else if (reslut.errorCode == PatchUtils.ErrorCode.EXEC_ERROR)
            showPatchErrorDialog(reslut.errorMessage);
        else
            showRebootDialog("安装失败", reslut.errorMessage);
    }
}