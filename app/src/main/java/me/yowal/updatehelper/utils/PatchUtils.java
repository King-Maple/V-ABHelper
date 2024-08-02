package me.yowal.updatehelper.utils;

import android.content.Context;
import android.os.Build;
import android.text.TextUtils;

import com.ejlchina.okhttps.OkHttps;
import com.topjohnwu.superuser.Shell;
import com.topjohnwu.superuser.ShellUtils;
import com.topjohnwu.superuser.nio.ExtendedFile;
import com.topjohnwu.superuser.nio.FileSystemManager;

import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import me.yowal.updatehelper.Config;
import me.yowal.updatehelper.manager.SuFileManager;
import me.yowal.updatehelper.manager.UpdateServiceManager;

public class PatchUtils {

    private final Context aContext;

    private final String aInstallDir;

    private final String aApatchManagerDir;

    private final FileSystemManager aFileSystemManager;

    private final Shell aShell;

    public PatchUtils(Context context, String installDir) {
        this.aContext = context;
        this.aInstallDir = installDir;
        this.aFileSystemManager = SuFileManager.getInstance().getRemote();
        this.aShell = Shell.getShell();
        this.aApatchManagerDir = UpdateServiceManager.getInstance().GetAPKInstallPath("me.bmax.apatch");
    }

    public Result patchMagisk() {
        String[] envList = new String[]{"busybox", "magiskboot", "magiskinit", "util_functions.sh", "boot_patch.sh"};
        for (String file: envList) {
            if (!aFileSystemManager.getFile("/data/adb/magisk/" + file).exists()) {
                return new Result(ErrorCode.EVEN_ERROR, "Magisk 环境不全，请自行操作");
            }
        }

        try {
            ExtendedFile stub = aFileSystemManager.getFile("/data/adb/magisk/stub.apk");
            if (!stub.exists()) {
                OutputStream out = stub.newOutputStream();
                IOUtils.copy(aContext.getAssets().open("stub.apk"), out);
            }
        } catch (IOException e) {
            return new Result(ErrorCode.EVEN_ERROR, "面具环境不全，请自行操作");
        }

        // 读取当前分区，用来判断第二分区
        String next_slot = Config.currentSlot.equals("_a") ? "_b" : "_a";

        // 提取第二分区的boot镜像
        String srcBoot = "/dev/block/bootdevice/by-name/init_boot" + next_slot;
        if (!aFileSystemManager.getFile(srcBoot).exists() || Build.MODEL.equals("PHP110")) {
            srcBoot = "/dev/block/bootdevice/by-name/boot" + next_slot;
        }

        FileUtils.delete(aInstallDir + "/boot.img");
        FileUtils.delete(aInstallDir + "/magisk_patch.img");

        if (!FlashUtils.extract_image(srcBoot, aInstallDir + "/boot.img")) {
            return new Result(ErrorCode.EXTRACT_ERROR, "镜像分区提取错误，请自行操作");
        }
        List<String> stdout = new ArrayList<>();
        // 使用面具自带的脚本进行修补
        boolean isSuccess = aShell.newJob()
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
            return new Result(ErrorCode.EXEC_ERROR, String.join("\n", stdout));
        }

        // 如果存在sha1值，则进行备份操作
        String sha1 = ShellUtils.fastCmd(aShell, "echo $SHA1");
        if (!TextUtils.isEmpty(sha1)) {

        }


        aShell.newJob().add("./magiskboot cleanup", "mv ./new-boot.img " + aInstallDir + "/magisk_patch.img", "rm ./stock_boot.img", "cd /").exec();
        if (!FlashUtils.flash_image(aInstallDir + "/magisk_patch.img", srcBoot)) {
            return new Result(ErrorCode.FLASH_ERROR, "刷入镜像失败，请自行操作");
        }
        return new Result(ErrorCode.SUCCESS, "安装到未使用卡槽完成");
    }

    public Result patchKernelSU() {

        boolean isLkmMode = UpdateServiceManager.getInstance().KsuIsLkmMode();

        // 读取当前分区，用来判断第二分区
        String next_slot = Config.currentSlot.equals("_a") ? "_b" : "_a";

        // 非 LKM 模式直接提取当前分区刷入第二分区
        if (!isLkmMode && !FlashUtils.flash_image("/dev/block/bootdevice/by-name/boot" + Config.currentSlot, "/dev/block/bootdevice/by-name/boot" + next_slot)) {
            return new Result(ErrorCode.FLASH_ERROR, "刷入镜像失败，请自行操作");
        }


        //检查 ksud 文件是否存在
        if (!aFileSystemManager.getFile("/data/adb/ksud").exists()) {
            return new Result(ErrorCode.EVEN_ERROR, "KernelSu 环境不全，请自行操作");
        }

        //ksud boot-patch -b <boot.img> --kmi android13-5.10

        // 提取第二分区的boot镜像
        String srcBoot = "/dev/block/bootdevice/by-name/init_boot" + next_slot;
        if (!aFileSystemManager.getFile(srcBoot).exists() || Build.MODEL.equals("PHP110")) {
            srcBoot = "/dev/block/bootdevice/by-name/boot" + next_slot;
        }

        ShellUtils.fastCmd("rm -r " + aInstallDir + "/*.img");

        if (!FlashUtils.extract_image(srcBoot, aInstallDir + "/boot.img")) {
            return new Result(ErrorCode.EXTRACT_ERROR, "镜像分区提取错误，请自行操作");
        }

        List<String> stdout = new ArrayList<>();
        boolean isSuccess = aShell.newJob()
                .add("cd " + aInstallDir)
                .add("/data/adb/ksud boot-patch --magiskboot " + aInstallDir +  "/magiskboot -b " + aInstallDir + "/boot.img")
                .to(stdout, stdout)
                .exec()
                .isSuccess();
        if (!isSuccess) {
            return new Result(ErrorCode.EXEC_ERROR, String.join("\n", stdout));
        }

        String patch_img = ShellUtils.fastCmd("cd " + aInstallDir + " & ls kernelsu_*.img");
        if (TextUtils.isEmpty(patch_img)) {
            return new Result(ErrorCode.OTHER_ERROR, "获取修补文件错误，请自行操作");
        }

        if (!FlashUtils.flash_image(aInstallDir + "/" + patch_img, srcBoot)) {
            return new Result(ErrorCode.FLASH_ERROR, "刷入镜像失败，请自行操作");
        }

        return new Result(ErrorCode.SUCCESS, "安装到未使用卡槽完成");
    }

    public Result patchAPatch(String SuperKey) {

        if (TextUtils.isEmpty(aApatchManagerDir))
            return new PatchUtils.Result(PatchUtils.ErrorCode.OTHER_ERROR, "APatch 获取失败，请自行操作");

        FileUtils.delete(aInstallDir + "/kpimg");
        Utils.unLibrary(aApatchManagerDir, "assets/kpimg", aInstallDir + "/kpimg");

        FileUtils.delete(aInstallDir + "/kptools");
        Utils.unLibrary(aApatchManagerDir, "lib/arm64-v8a/libkptools.so", aInstallDir + "/kptools");

        FileUtils.delete(aInstallDir + "/kpatch");
        Utils.unLibrary(aApatchManagerDir, "lib/arm64-v8a/libkpatch.so", aInstallDir + "/kpatch");

        ShellUtils.fastCmd("chmod -R 777 " + aInstallDir);

        String[] envList = new String[]{"kptools", "magiskboot", "kpimg", "kpatch", "apatch_patch.sh"};
        for (String file: envList) {
            if (!new File(aInstallDir + "/" + file).exists()) {
                return new Result(ErrorCode.EVEN_ERROR, file + " 文件不存在，请自行修补");
            }
        }

        // 读取当前分区，用来判断第二分区
        String slot = Config.currentSlot.equals("_a") ? "_b" : "_a";

        // 提取第二分区的boot镜像
        String srcBoot = "/dev/block/bootdevice/by-name/boot" + slot;

        FileUtils.delete(aInstallDir + "/boot.img");
        FileUtils.delete(aInstallDir + "/apatch_patch.img");

        if (!FlashUtils.extract_image(srcBoot,aInstallDir + "/boot.img")) {
            return new Result(ErrorCode.EXTRACT_ERROR, "镜像分区提取错误，请自行操作");
        }

        List<String> stdout = new ArrayList<>();
        // 使用面具自带的脚本进行修补
        boolean isSuccess = aShell.newJob()
                .add("cd " + aInstallDir)
                .add("sh apatch_patch.sh " + SuperKey + " " + aInstallDir + "/boot.img -K kpatch")
                .to(stdout, stdout)
                .exec()
                .isSuccess();
        if (!isSuccess) {
            return new Result(ErrorCode.EXEC_ERROR, String.join("\n", stdout));
        }

        aShell.newJob().add("./magiskboot cleanup", "mv ./new-boot.img " + aInstallDir + "/apatch_patch.img", "rm ./stock_boot.img", "cd /").exec();

        if (!FlashUtils.flash_image(aInstallDir + "/apatch_patch.img", srcBoot)) {
            return new Result(ErrorCode.FLASH_ERROR, "刷入镜像失败，请自行操作");
        }
        return new Result(ErrorCode.SUCCESS, "安装到未使用卡槽完成");
    }

    public static class Result {
        public int errorCode;
        public String errorMessage;

        public Result(int errorCode, String errorMessage) {
            this.errorCode = errorCode;
            this.errorMessage = errorMessage;
        }
    }

    public static final class ErrorCode {
        public static final int SUCCESS = 0;
        public static final int EXEC_ERROR = 1;
        public static final int EXTRACT_ERROR = 2;
        public static final int FLASH_ERROR = 3;
        public static final int EVEN_ERROR = 4;
        public static final int OTHER_ERROR = 4;
    }
}
