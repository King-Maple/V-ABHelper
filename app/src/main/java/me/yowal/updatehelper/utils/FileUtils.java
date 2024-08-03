package me.yowal.updatehelper.utils;

import com.topjohnwu.superuser.nio.ExtendedFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.security.MessageDigest;
import java.util.Objects;

import me.yowal.updatehelper.manager.SuFileManager;

public class FileUtils {
    public static void copyToFile(final InputStream inputStream, final File file) throws IOException {
        try (OutputStream out = openOutputStream(file)) {
            IOUtils.copy(inputStream, out);
        }
    }

    public static FileOutputStream openOutputStream(final File file) throws IOException {
        return openOutputStream(file, false);
    }

    public static FileOutputStream openOutputStream(final File file, final boolean append) throws IOException {
        Objects.requireNonNull(file, "file");
        if (file.exists()) {
            requireFile(file, "file");
            requireCanWrite(file, "file");
        } else {
            createParentDirectories(file);
        }
        return new FileOutputStream(file, append);
    }

    private static void requireCanWrite(final File file, final String name) {
        Objects.requireNonNull(file, "file");
        if (!file.canWrite()) {
            throw new IllegalArgumentException("File parameter '" + name + " is not writable: '" + file + "'");
        }
    }

    private static File requireFile(final File file, final String name) {
        Objects.requireNonNull(file, name);
        if (!file.isFile()) {
            throw new IllegalArgumentException("Parameter '" + name + "' is not a file: " + file);
        }
        return file;
    }


    private static File mkdirs(final File directory) throws IOException {
        if ((directory != null) && (!directory.mkdirs() && !directory.isDirectory())) {
            throw new IOException("Cannot create directory '" + directory + "'.");
        }
        return directory;
    }

    public static File createParentDirectories(final File file) throws IOException {
        return mkdirs(getParentFile(file));
    }

    private static File getParentFile(final File file) {
        return file == null ? null : file.getParentFile();
    }

    public static boolean delete(String file) {
        return new File(file).delete();
    }

    public static String getName(String file) {
        return new File(file).getName();
    }

    public static String getFileMD5(String file) {
        InputStream in = null;
        try {
            byte[] buffer = new byte[1024];
            int len;
            MessageDigest digest = MessageDigest.getInstance("MD5");
            ExtendedFile extendedFile  = SuFileManager.getInstance().getRemote().getFile(file);
            in = extendedFile.newInputStream();
            while ((len = in.read(buffer, 0, 1024)) != -1) {
                digest.update(buffer, 0, len);
            }
            return Utils.bytesToHex(digest.digest());
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        } finally {
            IOUtils.close(in);
        }
    }

    public static String getFileSHA1(String file) {
        InputStream in = null;
        try {
            byte[] buffer = new byte[1024];
            int len;
            MessageDigest digest = MessageDigest.getInstance("SHA-1");
            ExtendedFile extendedFile  = SuFileManager.getInstance().getRemote().getFile(file);
            in = extendedFile.newInputStream();
            while ((len = in.read(buffer, 0, 1024)) != -1) {
                digest.update(buffer, 0, len);
            }
            return Utils.bytesToHex(digest.digest());
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        } finally {
            IOUtils.close(in);
        }

    }
}
