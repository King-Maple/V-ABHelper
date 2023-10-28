#include <jni.h>
#include <sys/prctl.h>
#include <android/log.h>
#include "ksu.h"

#define LOG_TAG "KernelSu"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, __VA_ARGS__)

extern "C" {
    JNICALL jint GetVersion(JNIEnv * env , jclass clazz ) {
        return get_version();
    }

    JNICALL jboolean isSafeMode(JNIEnv *env, jclass clazz ) {
        return is_safe_mode();
    }
}




static JNINativeMethod getMethods[] = {
        {"getVersion", "()I", (void *) GetVersion},
        {"isSafeMode", "()Z", (void *) isSafeMode},
};

//此函数通过调用JNI中 RegisterNatives 方法来注册我们的函数
static int registerNativeMethods(JNIEnv *env, const char *className, JNINativeMethod *Methods, int methodsNum) {
    //找到声明native方法的类
    jclass clazz = env->FindClass(className);
    if (clazz == nullptr) {
        return JNI_FALSE;
    }
    //注册函数 参数：java类 所要注册的函数数组 注册函数的个数
    if (env->RegisterNatives(clazz, Methods, methodsNum) < 0) {
        return JNI_FALSE;
    }
    return JNI_TRUE;
}

static int registerNatives(JNIEnv *env) {
    //指定类的路径，通过FindClass 方法来找到对应的类
    return registerNativeMethods(env, "com/flyme/update/helper/utils/Natives", getMethods, sizeof(getMethods) / sizeof(getMethods[0]));
}

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *jvm, void *reserved) {
    JNIEnv *env = nullptr;
    jint result = -1;
    if (jvm->GetEnv((void **) &env, JNI_VERSION_1_4) != JNI_OK) {
        LOGD("GetEnv Fail");
        return -1;
    }
    if (!registerNatives(env)) {
        LOGD("registerNatives Fail");
        return -1;
    }
    result = JNI_VERSION_1_4;
    return result;
}