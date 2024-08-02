package me.yowal.updatehelper.utils;

import android.util.Log;

import com.topjohnwu.superuser.Shell;
import com.topjohnwu.superuser.ShellUtils;
import com.topjohnwu.superuser.nio.ExtendedFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;

import me.yowal.updatehelper.Config;
import me.yowal.updatehelper.manager.SuFileManager;

public class FlashUtils {

    //刷入镜像
    public static boolean flash_image(String img, String block) {
        InputStream in = null;
        OutputStream out = null;
        Shell.cmd("blockdev --setrw " + block).exec();
        try {
            ExtendedFile bootBackup = SuFileManager.getInstance().getRemote().getFile(img);
            if (!bootBackup.exists()) {
                LogUtils.e("flash_image", "img file no exists");
                return false;
            }
            ExtendedFile bootBlock  = SuFileManager.getInstance().getRemote().getFile(block);
            if (!bootBlock.exists()) {
                LogUtils.e("flash_image", "block file no exists");
                return false;
            }
            in = bootBackup.newInputStream();
            out = bootBlock.newOutputStream();
            return IOUtils.copy(in, out) > 0;
        } catch (IOException e) {
            LogUtils.e("flash_image", e.getLocalizedMessage());
            return false;
        } finally {
            IOUtils.close(in);
            IOUtils.close(out);
        }
    }

    /**
     * @param block 提取保存的路径
     * @param img 需要提取分区的路径
     * @return 提取状态
     */
    public static boolean extract_image(String block, String img) {
        InputStream in = null;
        OutputStream out = null;
        try {
            ExtendedFile bootBlock  = SuFileManager.getInstance().getRemote().getFile(block);
            if (!bootBlock.exists()) {
                LogUtils.e("extract_image", "block file no exists");
                return false;
            }
            ExtendedFile bootBackup = SuFileManager.getInstance().getRemote().getFile(img);
            in = bootBlock.newInputStream();
            out = bootBackup.newOutputStream();
            return IOUtils.copy(in, out) > 0;
        } catch (IOException e) {
            LogUtils.e("extract_image", e.getLocalizedMessage());
            return false;
        } finally {
            IOUtils.close(in);
            IOUtils.close(out);
        }
    }


    private static final byte[] bootloaderFlags = {
            0x61, 0x63, 0x74, 0x6F, 0x72, 0x79, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x6D,
            0x54, 0x65, 0x73, 0x74, 0x33, 0x2E, 0x31, 0x30, 0x2E, 0x37, 0x2E, 0x30
    };

    //修改 private 镜像 将不完美解锁方案变为完美
    public static boolean modifyPrivate(String installDir) {
        if (!Config.flymemodel.equals("M2391") && !Config.flymemodel.equals("M2381"))
            return true;
        RandomAccessFile randomAccessFile = null;
        try {
            LogUtils.i("modifyPrivate", "modifyPrivate start");
            String srcImage = "/dev/block/bootdevice/by-name/private";
            ShellUtils.fastCmd("rm -r " + installDir + "/*.img");
            if (!extract_image(srcImage,installDir + "/private.img")) {
                LogUtils.e("modifyPrivate", "extract image private fail");
                return false;
            }
            ShellUtils.fastCmd("chmod 777 " + installDir + "/private.img");
            randomAccessFile = new RandomAccessFile(installDir + "/private.img", "rw");
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
            return flash_image(installDir + "/private.img", srcImage);
        } catch (IOException e) {
            Log.e("modifyPrivate", e.getLocalizedMessage());
            return false;
        } finally {
            IOUtils.close(randomAccessFile);
        }
    }

}
