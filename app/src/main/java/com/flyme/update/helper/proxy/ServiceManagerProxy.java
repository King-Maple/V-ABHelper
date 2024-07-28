package com.flyme.update.helper.proxy;

import android.os.IBinder;

import com.flyme.update.helper.utils.Reflection;

public class ServiceManagerProxy {
    private static final Reflection<?> Ref = Reflection.on("android.os.ServiceManager");
    private static final Reflection.MethodWrapper METHOD_GET_SERVICE = Ref.method("getService", String.class);
    private static final Reflection.MethodWrapper METHOD_GET_SERVICE_OR_THROW = Ref.method("getServiceOrThrow", String.class);

    public static IBinder getService(String name) {
        return METHOD_GET_SERVICE.callStatic(name);
    }

    public static IBinder getServiceOrThrow(String name) {
        return METHOD_GET_SERVICE_OR_THROW.callStatic(name);
    }
}
