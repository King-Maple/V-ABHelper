package com.flyme.update.helper.manager;

import com.topjohnwu.superuser.nio.FileSystemManager;

public class SuFileManager {
    private static volatile SuFileManager instance;
    private FileSystemManager uFileSystemManager;

    public static synchronized SuFileManager getInstance() {
        if (instance == null) {
            synchronized (SuFileManager.class) {
                if (instance == null) {
                    instance = new SuFileManager();
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
