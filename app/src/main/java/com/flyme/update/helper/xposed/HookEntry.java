package com.flyme.update.helper.xposed;

import android.app.Activity;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import io.luckypray.dexkit.DexKitBridge;
import io.luckypray.dexkit.builder.BatchFindArgs;
import io.luckypray.dexkit.descriptor.member.DexMethodDescriptor;
import io.luckypray.dexkit.enums.MatchType;

public class HookEntry implements IXposedHookLoadPackage  {

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (!lpparam.packageName.equals("com.meizu.flyme.update"))
            return;
        System.loadLibrary("dexkit");

        try {
            DexKitBridge bridge = DexKitBridge.create(lpparam.appInfo.sourceDir);
            if (bridge == null) {
                return;
            }

            Map<String, List<DexMethodDescriptor>> resultMap =
                    bridge.batchFindMethodsUsingStrings(
                            BatchFindArgs.builder()
                                    .addQuery("UpdateVerify", Collections.singletonList("start verify for update"))
                                    .matchType(MatchType.CONTAINS)
                                    .build()
                    );
            List<DexMethodDescriptor> result = Objects.requireNonNull(resultMap.get("UpdateVerify"));

            if (result.size() != 1) {
                return;
            }

            DexMethodDescriptor descriptor = result.get(0);
            Class<?> mClassImpl = XposedHelpers.findClass(descriptor.getDeclaringClassName(), lpparam.classLoader);
            Method[] methods = mClassImpl.getDeclaredMethods();
            Method HookMethod = null;
            for (Method method : methods) {
                String ReturnTypeName = method.getReturnType().getName();
                if (ReturnTypeName.equals("int")) {
                    HookMethod = method;
                    break;
                }
            }
            if (HookMethod == null) {
                return;
            }
            // 更新包校验
            XposedBridge.hookMethod(HookMethod, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable{
                    super.afterHookedMethod(param);
                    int result = (int)param.getResult();
                    //XposedBridge.log("校验值：" + result);
                    if (result == -14) {
                        XposedBridge.log("检测到其他类型包，修改校验");
                        param.setResult(1);
                    }
                }
            });

            //弹窗改为通知
            //com.meizu.flyme.update.dialog.SystemRebootDialog
            mClassImpl = XposedHelpers.findClass("android.app.Activity", lpparam.classLoader);
            XposedBridge.hookAllMethods(mClassImpl, "onCreate", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    Activity mActivity = (Activity) param.thisObject;
                    XposedBridge.log("ClassName：" + mActivity.getClass().getName());

                }
            });
        } catch (XposedHelpers.ClassNotFoundError e) {
            e.printStackTrace();
        }
    }
}
