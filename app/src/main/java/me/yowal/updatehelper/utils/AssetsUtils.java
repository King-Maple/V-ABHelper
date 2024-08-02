package me.yowal.updatehelper.utils;

import android.content.Context;

import androidx.core.content.ContextCompat;

import com.topjohnwu.superuser.nio.ExtendedFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;

public class AssetsUtils {

    public static boolean writeFile(Context conetext,String fileName, File outFile) {
        InputStream input = null;
        OutputStream output = null;
        try {
            input = conetext.getAssets().open(fileName);
            if (outFile instanceof ExtendedFile) {
                output = ((ExtendedFile)outFile).newOutputStream();
            } else {
                output = Files.newOutputStream(outFile.toPath());
            }
            return IOUtils.copy(input, output) > 0;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } finally {
            IOUtils.close(input);
            IOUtils.close(output);
        }
    }

    public static boolean writeFile(Context conetext, int id, File outFile) {
        InputStream input = null;
        OutputStream output = null;
        try {
            input = conetext.getResources().openRawResource(id);
            if (outFile instanceof ExtendedFile) {
                output = ((ExtendedFile)outFile).newOutputStream();
            } else {
                output = Files.newOutputStream(outFile.toPath());
            }
            return IOUtils.copy(input, output) > 0;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } finally {
            IOUtils.close(input);
            IOUtils.close(output);
        }
    }
}
