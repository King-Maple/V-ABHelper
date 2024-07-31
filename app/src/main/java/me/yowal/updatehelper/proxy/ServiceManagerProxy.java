package me.yowal.updatehelper.proxy;

import android.os.IBinder;

import me.yowal.updatehelper.utils.ReflectionUtils;


public class ServiceManagerProxy {
    private static final ReflectionUtils<?> Ref = ReflectionUtils.on("android.os.ServiceManager");
    private static final ReflectionUtils.MethodWrapper METHOD_GET_SERVICE = Ref.method("getService", String.class);
    private static final ReflectionUtils.MethodWrapper METHOD_GET_SERVICE_OR_THROW = Ref.method("getServiceOrThrow", String.class);

    public static IBinder getService(String name) {
        return METHOD_GET_SERVICE.callStatic(name);
    }

    public static IBinder getServiceOrThrow(String name) {
        return METHOD_GET_SERVICE_OR_THROW.callStatic(name);
    }
}
