package com.flyme.update.helper.utils;

import com.topjohnwu.superuser.nio.FileSystemManager;

public class SuFileUtils {
    private static volatile SuFileUtils instance;
    private FileSystemManager uFileSystemManager;

    public static synchronized SuFileUtils getInstance() {
        if (instance == null) {
            synchronized (SuFileUtils.class) {
                if (instance == null) {
                    instance = new SuFileUtils();
                }
            }
        }
        return instance;
    }

    public void init(FileSystemManager manager) {
        uFileSystemManager = manager;
    }

    public FileSystemManager getRemote() {
        return uFileSystemManager;
    }

}
