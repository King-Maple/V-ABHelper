package me.yowal.updatehelper;

public class Natives {
    public static native int getVersion();

    public static native boolean isSafeMode();

    public static native boolean isLkmMode();

    public static native void passValidateSourceHash(String lib, int offset);

    public static native int findValidateSourceHash();
}
