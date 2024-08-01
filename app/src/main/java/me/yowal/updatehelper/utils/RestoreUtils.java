package me.yowal.updatehelper.utils;

import android.content.Context;
import android.os.Build;
import android.text.TextUtils;

import com.ejlchina.okhttps.OkHttps;
import com.topjohnwu.superuser.Shell;
import com.topjohnwu.superuser.ShellUtils;
import com.topjohnwu.superuser.nio.FileSystemManager;

import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import me.yowal.updatehelper.Config;
import me.yowal.updatehelper.R;
import me.yowal.updatehelper.manager.SuFileManager;
import me.yowal.updatehelper.manager.UpdateServiceManager;

public class RestoreUtils {
    private final Context aContext;

    private final String aInstallDir;

    private final FileSystemManager aFileSystemManager;

    private final Shell aShell;

    public RestoreUtils(Context context, String installDir) {
        this.aContext = context;
        this.aInstallDir = installDir;
        this.aFileSystemManager = SuFileManager.getInstance().getRemote();
        this.aShell = Shell.getShell();
    }


    public PatchUtils.Result restoreMagisk() {
        String[] envList = new String[]{"busybox", "magiskboot", "magiskinit", "util_functions.sh", "boot_patch.sh"};
        for (String file: envList) {
            if (!aFileSystemManager.getFile("/data/adb/magisk/" + file).exists()) {
                return new PatchUtils.Result(PatchUtils.ErrorCode.EVEN_ERROR, "Magisk 环境不全，请自行操作");
            }
        }

        try {
            FileUtils.copyToFile(aContext.getResources().openRawResource(R.raw.magisk_uninstaller), new File("/data/adb/magisk","magisk_uninstaller.sh"));
            Shell.cmd("chmod 777 /data/adb/magisk").exec();
        } catch (IOException e) {
            LogUtils.e("restoreMagisk", e.getLocalizedMessage());
        }

        if (!aFileSystemManager.getFile("/data/adb/magisk/magisk_uninstaller.sh").exists()) {
            return new PatchUtils.Result(PatchUtils.ErrorCode.EVEN_ERROR, "卸载环境不全，请自行操作");
        }

        // 提取当前分区的boot镜像
        String srcBoot = "/dev/block/bootdevice/by-name/init_boot" + Config.currentSlot;
        if (!aFileSystemManager.getFile(srcBoot).exists() || Build.MODEL.equals("PHP110")) {
            srcBoot = "/dev/block/bootdevice/by-name/boot" + Config.currentSlot;
        }

        // 保存的boot镜像
        String workBoot = aInstallDir + "/" + FileUtils.getName(srcBoot);
        FileUtils.delete(workBoot);
        if (!FlashUtils.extract_image(srcBoot, workBoot)) {
            return new PatchUtils.Result(PatchUtils.ErrorCode.EXTRACT_ERROR, "镜像分区提取错误，请自行操作");
        }
        List<String> stdout = new ArrayList<>();
        // 使用面具自带的脚本进行修补
        boolean isSuccess = aShell.newJob()
                .add("cd /data/adb/magisk")
                .add("sh magisk_uninstaller.sh " + workBoot)
                .to(stdout, stdout)
                .exec()
                .isSuccess();
        if (!isSuccess) {
            return new PatchUtils.Result(PatchUtils.ErrorCode.EXEC_ERROR, String.join("\n", stdout));
        }
        return new PatchUtils.Result(PatchUtils.ErrorCode.SUCCESS, "还原镜像完成");
    }

    public PatchUtils.Result restoreKernelSU() {
        boolean isLkmMode = UpdateServiceManager.getInstance().KsuIsLkmMode();
        if (!isLkmMode)
            return new PatchUtils.Result(PatchUtils.ErrorCode.OTHER_ERROR, "非 LKM 修补模式无法还原镜像");

        // 提取当前分区的boot镜像
        String srcBoot = "/dev/block/bootdevice/by-name/init_boot" + Config.currentSlot;
        if (!aFileSystemManager.getFile(srcBoot).exists() || Build.MODEL.equals("PHP110")) {
            srcBoot = "/dev/block/bootdevice/by-name/boot" + Config.currentSlot;
        }

        // 保存的boot镜像
        String workBoot = aInstallDir + "/" + FileUtils.getName(srcBoot);
        FileUtils.delete(workBoot);
        if (!FlashUtils.extract_image(srcBoot, workBoot)) {
            return new PatchUtils.Result(PatchUtils.ErrorCode.EXTRACT_ERROR, "镜像分区提取错误，请自行操作");
        }
        List<String> stdout = new ArrayList<>();
        boolean isSuccess = aShell.newJob()
                .add("cd " + aInstallDir)
                .add("/data/adb/ksud boot-restore -f --magiskboot " + aInstallDir +  "/magiskboot -b " + workBoot)
                .to(stdout, stdout)
                .exec()
                .isSuccess();
        if (!isSuccess) {
            return new PatchUtils.Result(PatchUtils.ErrorCode.EXEC_ERROR, String.join("\n", stdout));
        }
        return new PatchUtils.Result(PatchUtils.ErrorCode.SUCCESS, "还原镜像完成");
    }


    public PatchUtils.Result restoreAPatch() {
        try {
            FileUtils.copyToFile(aContext.getResources().openRawResource(R.raw.apatch_unpatch), new File(aInstallDir,"apatch_unpatch.sh"));
        } catch (IOException e) {
            LogUtils.e("restoreAPatch", e.getLocalizedMessage());
        }

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
            return new PatchUtils.Result(PatchUtils.ErrorCode.OTHER_ERROR, "APatch 版本获取失败，请自行操作");
        }

        FileUtils.delete(aInstallDir + "/kptools");
        OkHttps.sync("http://kpatch.oss-cn-shenzhen.aliyuncs.com/" + lastTag + "/kptools")
                .get()
                .getBody()
                .toFile(aInstallDir + "/kptools")
                .start();

        ShellUtils.fastCmd("chmod -R 777 " + aInstallDir);

        String[] envList = new String[]{"kptools", "magiskboot", "apatch_unpatch.sh"};
        for (String file: envList) {
            if (!new File(aInstallDir + "/" + file).exists()) {
                return new PatchUtils.Result(PatchUtils.ErrorCode.EVEN_ERROR, file + " 文件不存在，请自行还原");
            }
        }

        // 提取当前分区的boot镜像
        String srcBoot = "/dev/block/bootdevice/by-name/init_boot" + Config.currentSlot;
        if (!aFileSystemManager.getFile(srcBoot).exists() || Build.MODEL.equals("PHP110")) {
            srcBoot = "/dev/block/bootdevice/by-name/boot" + Config.currentSlot;
        }

        // 保存的boot镜像
        String workBoot = aInstallDir + "/" + FileUtils.getName(srcBoot);
        FileUtils.delete(workBoot);
        if (!FlashUtils.extract_image(srcBoot, workBoot)) {
            return new PatchUtils.Result(PatchUtils.ErrorCode.EXTRACT_ERROR, "镜像分区提取错误，请自行操作");
        }
        List<String> stdout = new ArrayList<>();
        boolean isSuccess = aShell.newJob()
                .add("cd " + aInstallDir)
                .add("sh apatch_unpatch.sh " + workBoot)
                .to(stdout, stdout)
                .exec()
                .isSuccess();
        if (!isSuccess) {
            return new PatchUtils.Result(PatchUtils.ErrorCode.EXEC_ERROR, String.join("\n", stdout));
        }
        return new PatchUtils.Result(PatchUtils.ErrorCode.SUCCESS, "还原镜像完成");
    }
}
