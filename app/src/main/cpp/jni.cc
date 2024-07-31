#include <jni.h>

#include <sys/prctl.h>
#include <cstring>
#include <fcntl.h>
#include <cstdio>
#include <unistd.h>
#include <LogUtils.h>
#include <android/dlext.h>
#include <inject.h>
#include <asm-generic/unistd.h>
#include "ksu.h"
#include "ota_metadata.pb.h"
#include <fstream>
#include <iostream>

uintptr_t writeMemory(InjectTools* injectTools, uintptr_t *address, void* data, size_t size){
    uintptr_t ret = *address;
    injectTools->writeRemoteMemory(ret, data, size);
    *address += size;
    return ret;
}

bool injectLibraryToRemote(int pid,const char* library_path,const char* symbol_name,std::vector<uintptr_t> symbol_args){
    int fd = open (library_path, O_RDONLY);
    if (fd) {
        size_t dataSize = 0;
        char *data = nullptr;
        char buf[1024] = {0};
        size_t ret;
        while ((ret = read (fd, buf, sizeof (buf))) != 0) {
            dataSize += ret;
            if (!data) {
                data = (char *) malloc (ret);
            } else {
                data = (char *) realloc (data, dataSize);
            }
            memcpy (data + (dataSize - ret), buf, ret);
        }

        auto injectTools = new InjectTools (pid);
        //开始注入
        if (!injectTools->injectStart ()) {
            LOGE("injectStart failed");
            free(data);
            return false;
        }
        //在远程进程中申请一片内存
        uintptr_t memoryAddr = injectTools->callMmap (0, PAGE_SIZE * 2, PROT_READ | PROT_WRITE, MAP_PRIVATE | MAP_ANONYMOUS, -1, 0);
        if (!memoryAddr) {
            LOGE("mmap failed");
            delete injectTools;
            free(data);
            return false;
        }
        injectTools->emptyRemoteMemory (memoryAddr, PAGE_SIZE * 2);

        const char *memfileName = "/passValidateSourceHash";
        uintptr_t memfileNameAddr = writeMemory(injectTools, &memoryAddr, (void*)memfileName, strlen(memfileName) + 1);

        std::vector<uintptr_t> args;
        args.push_back (__NR_memfd_create);
        args.push_back ((uintptr_t)memfileNameAddr);
        args.push_back (MFD_CLOEXEC);
        int memfd = injectTools->callSyscall (args);
        if (memfd <= 0) {
            LOGE("memfd_create failed");
            injectTools->callMunmap (memoryAddr, PAGE_SIZE * 2);
            delete injectTools;
            free(data);
            return false;
        }
        LOGD("remote memfd: %d", memfd);
        injectTools->callFcntl (memfd, F_ADD_SEALS,F_SEAL_SHRINK | F_SEAL_GROW | F_SEAL_WRITE | F_SEAL_SEAL);

        if(injectTools->callFtruncate (memfd, dataSize) != 0){
            LOGE("ftruncate failed");
            injectTools->callMunmap (memoryAddr, PAGE_SIZE * 2);
            delete injectTools;
            free(data);
            return false;
        }

        auto libraryDataAddr = injectTools->callMmap(0, dataSize, PROT_READ | PROT_WRITE, MAP_SHARED, memfd, 0);
        LOGD("libraryDataAddr: %p", libraryDataAddr);
        if(!libraryDataAddr){
            LOGE("mmap failed");
            injectTools->callMunmap (memoryAddr, PAGE_SIZE * 2);
            delete injectTools;
            free(data);
            return false;
        }
        injectTools->emptyRemoteMemory (libraryDataAddr, dataSize);
        injectTools->writeRemoteMemory(libraryDataAddr, data, dataSize);

        free(data);

        android_dlextinfo extinfo;
        extinfo.flags = ANDROID_DLEXT_USE_LIBRARY_FD;
        extinfo.library_fd = memfd;
        uintptr_t extinfoAddr = writeMemory(injectTools, &memoryAddr, (void*)&extinfo, sizeof(extinfo));

        auto libraryHandle = (uintptr_t) injectTools->callDlopenExt (memfileNameAddr,RTLD_LAZY, extinfoAddr);
        if (!libraryHandle) {
            LOGE("dlopen failed");
            injectTools->callMunmap (libraryDataAddr, dataSize);
            injectTools->callMunmap (memoryAddr, PAGE_SIZE * 2);
            delete injectTools;
            return false;
        }
        injectTools->callClose (memfd);
        injectTools->callMunmap (libraryDataAddr, dataSize);


        //调用函数
        uintptr_t funcNameAddr = writeMemory(injectTools, &memoryAddr, (void*)symbol_name, strlen(symbol_name) + 1);
        auto func = (uintptr_t) injectTools->callDlsym (libraryHandle, funcNameAddr);
        if(!func){
            LOGE("dlsym failed");
            injectTools->callMunmap (memoryAddr, PAGE_SIZE * 2);
            delete injectTools;
            return false;
        }

        //args.clear();
        //args.push_back (libraryHandle);
        injectTools->call (func, symbol_args);

        injectTools->callMunmap (memoryAddr, PAGE_SIZE * 2);
        delete injectTools;

        return true;
    }
    return false;
}


extern "C" {
    JNICALL jboolean isOtaZip(JNIEnv * env , jclass clazz, jbyteArray bytearray) {
        build::tools::releasetools::OtaMetadata OtaMetadata;
        jbyte * bytes = env->GetByteArrayElements(bytearray, nullptr);
        int bytes_len = env->GetArrayLength(bytearray);
        char *buf = new char[bytes_len + 1];
        memcpy(buf, bytes, bytes_len);
        env->ReleaseByteArrayElements(bytearray, bytes, 0);
        OtaMetadata.ParseFromArray(buf, bytes_len);
        if (!OtaMetadata.precondition().partition_state().empty()) {
            LOGD("ota包");
        }
        return get_version();
    }

    JNICALL jint GetVersion(JNIEnv * env , jclass clazz) {
        return get_version();
    }

    JNICALL jboolean isSafeMode(JNIEnv *env, jclass clazz) {
        return is_safe_mode();
    }

    JNICALL jboolean isLkmMode(JNIEnv *env, jclass clazz) {
        return is_lkm_mode();
    }

    JNICALL void passValidateSourceHash(JNIEnv *env, jclass clazz, jstring library_path, jint offset) {
        if (offset <=0)
            return;
        int pid =  get_pid_by_name("/system/bin/update_engine");
        LOGD("update_engine pid = %d", pid);
        if (pid <= 0)
            return;
        auto baseAddr = (uint64_t)get_module_base_addr(pid,"passValidateSourceHash");
        LOGD("update_engine baseAddr = %lx", baseAddr);
        //selinux宽容模式
        system ("setenforce 0");
        const char *libraryPathStr = env->GetStringUTFChars (library_path, nullptr);
        if (baseAddr <= 0) {
            std::vector<uintptr_t> symbol_args;
            symbol_args.push_back (offset);
            bool ret = injectLibraryToRemote(pid, libraryPathStr, "entry", symbol_args);
            LOGD("injectLibraryToRemote = %d", ret);

        }
        env->ReleaseStringUTFChars (library_path, libraryPathStr);
        system ("setenforce 1");
    }

    JNICALL int findValidateSourceHash(JNIEnv *env, jclass clazz) {
        int pid =  get_pid_by_name("/system/bin/update_engine");
        return findValidateFunction(pid);
    }


}


static JNINativeMethod getMethods[] = {
        {"getVersion", "()I", (void *) GetVersion},
        {"isSafeMode", "()Z", (void *) isSafeMode},
        {"isLkmMode", "()Z", (void *) isLkmMode},
        {"passValidateSourceHash", "(Ljava/lang/String;I)V", (void *) passValidateSourceHash},
        {"findValidateSourceHash", "()I", (void *) findValidateSourceHash},
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
    return registerNativeMethods(env, "me/yowal/updatehelper/Natives", getMethods, sizeof(getMethods) / sizeof(getMethods[0]));
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