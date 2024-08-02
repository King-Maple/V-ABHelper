package me.yowal.updatehelper.utils;

import android.content.Context;
import android.os.Build;
import android.text.TextUtils;

import com.topjohnwu.superuser.Shell;
import com.topjohnwu.superuser.ShellUtils;
import com.topjohnwu.superuser.nio.FileSystemManager;

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

    private final String aApatchManagerDir;

    private final FileSystemManager aFileSystemManager;

    private final Shell aShell;

    private String aBackupDir;

    public RestoreUtils(Context context, String installDir) {
        this.aContext = context;
        this.aInstallDir = installDir;
        if (!context.getCacheDir().exists())
            context.getCacheDir().mkdirs();
        this.aBackupDir = context.getCacheDir().getAbsolutePath();
        this.aFileSystemManager = SuFileManager.getInstance().getRemote();
        this.aShell = Shell.getShell();
        this.aApatchManagerDir = UpdateServiceManager.getInstance().GetAPKInstallPath("me.bmax.apatch");
    }


    public PatchUtils.Result restoreMagisk() {
        String[] envList = new String[]{"busybox", "magiskboot", "magiskinit", "util_functions.sh", "boot_patch.sh"};
        for (String file: envList) {
            if (!aFileSystemManager.getFile("/data/adb/magisk/" + file).exists()) {
                return new PatchUtils.Result(PatchUtils.ErrorCode.EVEN_ERROR, "Magisk 环境不全，请自行操作");
            }
        }

        Shell.cmd("chmod 777 /data/adb/magisk/magisk_uninstaller.sh").exec();

        if (!aFileSystemManager.getFile("/data/adb/magisk/magisk_uninstaller.sh").exists()) {
            return new PatchUtils.Result(PatchUtils.ErrorCode.EVEN_ERROR, "卸载环境不全，请自行操作");
        }

        // 提取当前分区的boot镜像
        String srcBoot = "/dev/block/bootdevice/by-name/init_boot" + Config.currentSlot;
        if (!aFileSystemManager.getFile(srcBoot).exists() || Build.MODEL.equals("PHP110")) {
            srcBoot = "/dev/block/bootdevice/by-name/boot" + Config.currentSlot;
        }

        FileUtils.delete(aInstallDir + "/boot.img");
        FileUtils.delete(aInstallDir + "/magisk_patch.img");

        if (!FlashUtils.extract_image(srcBoot, aInstallDir + "/boot.img")) {
            return new PatchUtils.Result(PatchUtils.ErrorCode.EXTRACT_ERROR, "镜像分区提取错误，请自行操作");
        }
        List<String> stdout = new ArrayList<>();
        // 使用面具自带的脚本进行修补
        boolean isSuccess = aShell.newJob()
                .add("cd /data/adb/magisk")
                .add("sh magisk_uninstaller.sh " + aInstallDir + "/boot.img " + aInstallDir +  "/magiskboot")
                .to(stdout, stdout)
                .exec()
                .isSuccess();
        if (!isSuccess) {
            return new PatchUtils.Result(PatchUtils.ErrorCode.EXEC_ERROR, String.join("\n", stdout));
        }

        LogUtils.d("restoreMagisk", String.join("\n", stdout));

        aShell.newJob().add("./magiskboot cleanup", "mv ./new-boot.img " + aInstallDir + "/magisk_patch.img", "rm ./stock_boot.img", "cd /").exec();
        if (!FlashUtils.flash_image(aInstallDir + "/magisk_patch.img", srcBoot)) {
            return new PatchUtils.Result(PatchUtils.ErrorCode.FLASH_ERROR, "刷入镜像失败，请自行操作");
        }
        ShellUtils.fastCmd("rm -r " + aBackupDir + "/*.img");
        Shell.cmd("mv " + aInstallDir + "/boot.img " + aBackupDir + "/boot.img").exec();

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

        ShellUtils.fastCmd("rm -r " + aInstallDir + "/*.img");
        if (!FlashUtils.extract_image(srcBoot, aInstallDir + "/boot.img")) {
            return new PatchUtils.Result(PatchUtils.ErrorCode.EXTRACT_ERROR, "镜像分区提取错误，请自行操作");
        }

        ShellUtils.fastCmd("cp -f " + aInstallDir + "/magiskboot /data/adb/ksu/bin/magiskboot");
        ShellUtils.fastCmd("chmod 755 /data/adb/ksu/bin/magiskboot");
        List<String> stdout = new ArrayList<>();
        boolean isSuccess = aShell.newJob()
                .add("cd " + aInstallDir)
                .add("/data/adb/ksud boot-restore --magiskboot " + aInstallDir +  "/magiskboot -b " + aInstallDir + "/boot.img")
                .to(stdout, stdout)
                .exec()
                .isSuccess();
        if (!isSuccess) {
            return new PatchUtils.Result(PatchUtils.ErrorCode.EXEC_ERROR, String.join("\n", stdout));
        }
        LogUtils.d("restoreKernelSU", String.join("\n", stdout));

        String patch_img = ShellUtils.fastCmd("cd " + aInstallDir + " & ls kernelsu_*.img");

        if (TextUtils.isEmpty(patch_img)) {
            return new PatchUtils.Result(PatchUtils.ErrorCode.OTHER_ERROR, "获取修补文件错误，请自行操作");
        }

        if (!FlashUtils.flash_image(aInstallDir + "/" + patch_img, srcBoot)) {
            return new PatchUtils.Result(PatchUtils.ErrorCode.FLASH_ERROR, "刷入镜像失败，请自行操作");
        }
        ShellUtils.fastCmd("rm -r " + aBackupDir + "/*.img");
        Shell.cmd("mv " + aInstallDir + "/boot.img " + aBackupDir + "/boot.img").exec();
        return new PatchUtils.Result(PatchUtils.ErrorCode.SUCCESS, "还原镜像完成");
    }


    public PatchUtils.Result restoreAPatch() {
        if (TextUtils.isEmpty(aApatchManagerDir))
            return new PatchUtils.Result(PatchUtils.ErrorCode.OTHER_ERROR, "APatch 获取失败，请自行操作");

        AssetsUtils.writeFile(aContext, R.raw.apatch_unpatch, new File(aInstallDir,"apatch_unpatch.sh"));

        FileUtils.delete(aInstallDir + "/kptools");
        if (!Utils.unLibrary(aApatchManagerDir, "lib/arm64-v8a/libkptools.so", aInstallDir + "/kptools"))
            return new PatchUtils.Result(PatchUtils.ErrorCode.OTHER_ERROR, "kptools 解压失败，请自行操作");

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

        FileUtils.delete(aInstallDir + "/boot.img");
        FileUtils.delete(aInstallDir + "/apatch_patch.img");

        if (!FlashUtils.extract_image(srcBoot, aInstallDir + "/boot.img")) {
            return new PatchUtils.Result(PatchUtils.ErrorCode.EXTRACT_ERROR, "镜像分区提取错误，请自行操作");
        }

        List<String> stdout = new ArrayList<>();
        boolean isSuccess = aShell.newJob()
                .add("cd " + aInstallDir)
                .add("sh apatch_unpatch.sh " + aInstallDir + "/boot.img")
                .to(stdout, stdout)
                .exec()
                .isSuccess();
        LogUtils.d("restoreAPatch", String.join("\n", stdout));
        if (!isSuccess) {
            return new PatchUtils.Result(PatchUtils.ErrorCode.EXEC_ERROR, String.join("\n", stdout));
        }

        aShell.newJob().add("./magiskboot cleanup", "mv ./new-boot.img " + aInstallDir + "/apatch_patch.img", "rm ./stock_boot.img", "cd /").exec();

        if (!FlashUtils.flash_image(aInstallDir + "/apatch_patch.img", srcBoot)) {
            return new PatchUtils.Result(PatchUtils.ErrorCode.FLASH_ERROR, "刷入镜像失败，请自行操作");
        }

        ShellUtils.fastCmd("rm -r " + aBackupDir + "/*.img");
        Shell.cmd("mv " + aInstallDir + "/boot.img " + aBackupDir + "/boot.img").exec();
        return new PatchUtils.Result(PatchUtils.ErrorCode.SUCCESS, "还原镜像完成");
    }

    public void RestoreFlash() {
        // 提取当前分区的boot镜像
        String srcBoot = "/dev/block/bootdevice/by-name/init_boot" + Config.currentSlot;
        if (!aFileSystemManager.getFile(srcBoot).exists() || Build.MODEL.equals("PHP110")) {
            srcBoot = "/dev/block/bootdevice/by-name/boot" + Config.currentSlot;
        }

        LogUtils.d("RestoreFlash", "backupBoot = " + aBackupDir + "/boot.img, flash = " + FlashUtils.flash_image(aBackupDir + "/boot.img", srcBoot));
    }
}
