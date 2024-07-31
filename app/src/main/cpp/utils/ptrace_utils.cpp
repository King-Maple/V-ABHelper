/**
 * 让Ptrace注入兼容多平台的主要步骤在这里
 */
#include <cstdlib>
#include <sys/system_properties.h>
#include "ptrace_utils.h"
#include "LogUtils.h"
/**
 * @brief 处理各架构预定义的库文件
 */

const char* libc_path = "/apex/com.android.runtime/lib64/bionic/libc.so";
const char* linker_path = "/apex/com.android.runtime/bin/linker64";
const char* libdl_path = "/apex/com.android.runtime/lib64/bionic/libdl.so";

pid_t get_pid_by_name(const char *task_name) {
    DIR *dir;
    struct dirent *ent;
    pid_t pid = -1;
    if ((dir = opendir("/proc")) == nullptr) {
		return -1;
	}
	while ((ent = readdir(dir)) != NULL) {
		if (atoi(ent->d_name) != 0) {
			char task_path[50] = {0};
			snprintf (task_path, sizeof (task_path), "/proc/%s/cmdline", ent->d_name);
			FILE *fp = fopen (task_path, "r");
			if (fp != NULL) {
				char task_name_buf[100] = {0};
				fgets (task_name_buf, sizeof (task_name_buf), fp);
				fclose (fp);
				task_name_buf[strcspn (task_name_buf, "\n")] = '\0';
				if (strstr (task_name_buf, task_name) != nullptr) {
					pid = atoi (ent->d_name);
					break;
				}
			}
		}
	}
	closedir (dir);
	return pid;
}

void * get_module_end(pid_t pid, const char *module_name) {
    FILE *fp;
    uintptr_t temp = 0, addr = 0;
    char filename[32], buffer[1024];
    if (pid < 0) {
        snprintf(filename, sizeof(filename), "/proc/self/maps");
    } else {
        snprintf(filename, sizeof(filename), "/proc/%d/maps", pid);
    }
    fp = fopen(filename, "rt");
    if (fp != nullptr) {
        while (fgets(buffer, sizeof(buffer), fp)) {
            if (strstr(buffer, module_name)) {
#if defined(__LP64__)
                sscanf(buffer, "%lx-%lx %*s", &temp, &addr);
#else
                sscanf(buffer, "%x-%x %*s",&temp, &addr);
#endif
            }
        }
        fclose(fp);
    }
    return(void *)  addr;
}

void *get_module_base_addr(pid_t pid, const char *ModuleName) {
    FILE *fp = NULL;
    uintptr_t ModuleBaseAddr = 0;
    char szFileName[50] = {0};
    char szMapFileLine[1024] = {0};

    // 读取"/proc/pid/maps"可以获得该进程加载的模块
    if (pid < 0) {
        //  枚举自身进程模块
        snprintf(szFileName, sizeof(szFileName), "/proc/self/maps");
    } else {
        snprintf(szFileName, sizeof(szFileName), "/proc/%d/maps", pid);
    }

    fp = fopen(szFileName, "r");

    if (fp != nullptr) {
        while (fgets(szMapFileLine, sizeof(szMapFileLine), fp)) {
            if (strstr(szMapFileLine, ModuleName)) {
                char *Addr = strtok(szMapFileLine, "-");
                ModuleBaseAddr = strtoul(Addr, nullptr, 16);
                if (ModuleBaseAddr == 0x8000) {
                    ModuleBaseAddr = 0;
                }

                break;
            }
        }

        fclose(fp);
    }

    return (void *) ModuleBaseAddr;
}

void *get_remote_func_addr(pid_t pid, void *LocalFuncAddr) {
	Dl_info dl_info;
	dladdr (LocalFuncAddr, &dl_info);
    //获取本地某个模块的起始地址
    //LocalModuleAddr = get_module_base_addr(-1, ModuleName);
    //获取远程pid的某个模块的起始地址
	void *RemoteModuleAddr = get_module_base_addr(pid, dl_info.dli_fname);
    // local_addr - local_handle的值为指定函数(如mmap)在该模块中的偏移量，然后再加上remote_handle，结果就为指定函数在目标进程的虚拟地址
	void *RemoteFuncAddr = (void *) ((uintptr_t) LocalFuncAddr - (uintptr_t) dl_info.dli_fbase + (uintptr_t) RemoteModuleAddr);

    ////printf("[+] [get_remote_func_addr] lmod=0x%lX, rmod=0x%lX, lfunc=0x%lX, rfunc=0x%lX\n", dl_info.dli_fbase, RemoteModuleAddr, LocalFuncAddr, RemoteFuncAddr);
    return RemoteFuncAddr;
}


int ptrace_attach(pid_t pid) {
    int status = 0;
    if (ptrace(PTRACE_ATTACH, pid, NULL, NULL) < 0) {
        //printf("[-] ptrace attach process error, pid:%d, err:%s\n", pid, strerror(errno));
        return -1;
    }

    //printf("[+] attach porcess success, pid:%d\n", pid);
    waitpid(pid, &status, WUNTRACED);

    return 0;
}

int ptrace_continue(pid_t pid) {
    if (ptrace(PTRACE_CONT, pid, NULL, NULL) < 0) {
        //printf("[-] ptrace continue process error, pid:%d, err:%ss\n", pid, strerror(errno));
        return -1;
    }

    //printf("[+] ptrace continue process success, pid:%d\n", pid);
    return 0;
}

int ptrace_detach(pid_t pid) {
    if (ptrace(PTRACE_DETACH, pid, NULL, 0) < 0) {
        //printf("[-] detach process error, pid:%d, err:%s\n", pid, strerror(errno));
        return -1;
    }

    //printf("[+] detach process success, pid:%d\n", pid);
    return 0;
}

int ptrace_getregs(pid_t pid, struct pt_regs *regs) {
#if defined(__aarch64__)
    int regset = NT_PRSTATUS;
    struct iovec ioVec;

    ioVec.iov_base = regs;
    ioVec.iov_len = sizeof(*regs);
    if (ptrace(PTRACE_GETREGSET, pid, (void *) regset, &ioVec) < 0) {
        //printf("[-] ptrace_getregs: Can not get register values, io %llx, %d\n", ioVec.iov_base, ioVec.iov_len);
        return -1;
    }

    return 0;
#else
    if (ptrace(PTRACE_GETREGS, pid, NULL, regs) < 0) {
        //printf("[-] Get Regs error, pid:%d, err:%s\n", pid, strerror(errno));
        return -1;
    }
#endif
    return 0;
}

int ptrace_setregs(pid_t pid, struct pt_regs *regs) {
#if defined(__aarch64__)
    int regset = NT_PRSTATUS;
    struct iovec ioVec;

    ioVec.iov_base = regs;
    ioVec.iov_len = sizeof(*regs);
    if (ptrace(PTRACE_SETREGSET, pid, (void *) regset, &ioVec) < 0) {
        perror("[-] ptrace_setregs: Can not get register values");
        return -1;
    }

    return 0;
#else
    if (ptrace(PTRACE_SETREGS, pid, NULL, regs) < 0) {
        //printf("[-] Set Regs error, pid:%d, err:%s\n", pid, strerror(errno));
        return -1;
    }
#endif
    return 0;
}

uintptr_t ptrace_getret(struct pt_regs *regs) {
#if defined(__i386__) || defined(__x86_64__) // 模拟器&x86_64
    return regs->eax;
#elif defined(__arm__) || defined(__aarch64__) // 真机
    return regs->ARM_r0;
#else
    //printf("Not supported Environment %s\n", __FUNCTION__);
#endif
}

uintptr_t ptrace_getpc(struct pt_regs *regs) {
#if defined(__i386__) || defined(__x86_64__)
    return regs->eip;
#elif defined(__arm__) || defined(__aarch64__)
    return regs->ARM_pc;
#else
    //printf("Not supported Environment %s\n", __FUNCTION__);
#endif
}

int ptrace_readdata(pid_t pid, uint8_t *pSrcBuf, uint8_t *pDestBuf, size_t size) {
    uintptr_t nReadCount = 0;
    uintptr_t nRemainCount = 0;
    uint8_t *pCurSrcBuf = pSrcBuf;
    uint8_t *pCurDestBuf = pDestBuf;
    uintptr_t lTmpBuf = 0;
    uintptr_t i = 0;

    nReadCount = size / sizeof(uintptr_t);
    nRemainCount = size % sizeof(uintptr_t);

    for (i = 0 ; i < nReadCount ; i++) {
        lTmpBuf = ptrace(PTRACE_PEEKTEXT, pid, pCurSrcBuf, 0);
        memcpy(pCurDestBuf, (char *) (&lTmpBuf), sizeof(uintptr_t));
        pCurSrcBuf += sizeof(uintptr_t);
        pCurDestBuf += sizeof(uintptr_t);
    }

    if (nRemainCount > 0) {
        lTmpBuf = ptrace(PTRACE_PEEKTEXT, pid, pCurSrcBuf, 0);
        memcpy(pCurDestBuf, (char *) (&lTmpBuf), nRemainCount);
    }

    return 0;
}

int ptrace_writedata(pid_t pid, uint8_t *pWriteAddr, uint8_t *pWriteData, size_t size) {

    uintptr_t nWriteCount = 0;
    uintptr_t nRemainCount = 0;
    uint8_t *pCurSrcBuf = pWriteData;
    uint8_t *pCurDestBuf = pWriteAddr;
    uintptr_t lTmpBuf = 0;
    uintptr_t i = 0;

    nWriteCount = size / sizeof(uintptr_t);
    nRemainCount = size % sizeof(uintptr_t);

    // 先讲数据以sizeof(uintptr_t)字节大小为单位写入到远程进程内存空间中
    for (i = 0 ; i < nWriteCount ; i++) {
        memcpy((void *) (&lTmpBuf), pCurSrcBuf, sizeof(uintptr_t));
        if (ptrace(PTRACE_POKETEXT, pid, (void *) pCurDestBuf, (void *) lTmpBuf) < 0) { // PTRACE_POKETEXT表示从远程内存空间写入一个sizeof(uintptr_t)大小的数据
            //printf("[-] Write Remote Memory error, MemoryAddr:0x%lx, err:%s\n", (uintptr_t) pCurDestBuf, strerror(errno));
            return -1;
        }
        pCurSrcBuf += sizeof(uintptr_t);
        pCurDestBuf += sizeof(uintptr_t);
    }
    // 将剩下的数据写入到远程进程内存空间中
    if (nRemainCount > 0) {
        lTmpBuf = ptrace(PTRACE_PEEKTEXT, pid, pCurDestBuf, NULL); //先取出原内存中的数据，然后将要写入的数据以单字节形式填充到低字节处
        memcpy((void *) (&lTmpBuf), pCurSrcBuf, nRemainCount);
        if (ptrace(PTRACE_POKETEXT, pid, pCurDestBuf, lTmpBuf) < 0) {
            //printf("[-] Write Remote Memory error, MemoryAddr:0x%lx, err:%s\n", (uintptr_t) pCurDestBuf, strerror(errno));
            return -1;
        }
    }
    return 0;
}

int ptrace_call(pid_t pid, uintptr_t ExecuteAddr, uintptr_t *parameters, uintptr_t num_params, struct pt_regs *regs) {
#if defined(__i386__) // 模拟器
    // 写入参数到堆栈
    regs->esp -= (num_params) * sizeof(uintptr_t); // 分配栈空间，栈的方向是从高地址到低地址
    if (0 != ptrace_writedata(pid, (uint8_t *)regs->esp, (uint8_t *)parameters,(num_params) * sizeof(uintptr_t))){
        return -1;
    }

    uintptr_t tmp_addr = 0x0;
    regs->esp -= sizeof(uintptr_t);
    if (0 != ptrace_writedata(pid, (uint8_t *)regs->esp, (uint8_t *)&tmp_addr, sizeof(tmp_addr))){
        return -1;
    }

    //设置eip寄存器为需要调用的函数地址
    regs->eip = ExecuteAddr;

    // 开始执行
    if (-1 == ptrace_setregs(pid, regs) || -1 == ptrace_continue(pid)){
        //printf("[-] ptrace set regs or continue error, pid:%d\n", pid);
        return -1;
    }

    int stat = 0;
    // 对于使用ptrace_cont运行的子进程，它会在3种情况下进入暂停状态：①下一次系统调用；②子进程退出；③子进程的执行发生错误。
    // 参数WUNTRACED表示当进程进入暂停状态后，立即返回
    waitpid(pid, &stat, WUNTRACED);

    // 判断是否成功执行函数
    //printf("[+] ptrace call ret status is %d\n", stat);
    while (stat != 0xb7f){
        if (ptrace_continue(pid) == -1){
            //printf("[-] ptrace call error");
            return -1;
        }
        waitpid(pid, &stat, WUNTRACED);
    }

    // 获取远程进程的寄存器值，方便获取返回值
    if (ptrace_getregs(pid, regs) == -1){
        //printf("[-] After call getregs error");
        return -1;
    }

#elif defined(__x86_64__) // ？？
    int num_param_registers = 6;
    // x64处理器，函数传递参数，将整数和指针参数前6个参数从左到右保存在寄存器rdi,rsi,rdx,rcx,r8和r9
    // 更多的参数则按照从右到左的顺序依次压入堆栈。
    if (num_params > 0)
        regs->rdi = parameters[0];
    if (num_params > 1)
        regs->rsi = parameters[1];
    if (num_params > 2)
        regs->rdx = parameters[2];
    if (num_params > 3)
        regs->rcx = parameters[3];
    if (num_params > 4)
        regs->r8 = parameters[4];
    if (num_params > 5)
        regs->r9 = parameters[5];

    if (num_param_registers < num_params){
        regs->esp -= (num_params - num_param_registers) * sizeof(uintptr_t); // 分配栈空间，栈的方向是从高地址到低地址
        if (0 != ptrace_writedata(pid, (uint8_t *)regs->esp, (uint8_t *)&parameters[num_param_registers], (num_params - num_param_registers) * sizeof(uintptr_t))){
            return -1;
        }
    }

    uintptr_t tmp_addr = 0x0;
    regs->esp -= sizeof(uintptr_t);
    if (0 != ptrace_writedata(pid, (uint8_t *)regs->esp, (uint8_t *)&tmp_addr, sizeof(tmp_addr))){
        return -1;
    }

    //设置eip寄存器为需要调用的函数地址
    regs->eip = ExecuteAddr;

    // 开始执行
    if (-1 == ptrace_setregs(pid, regs) || -1 == ptrace_continue(pid)){
        //printf("[-] ptrace set regs or continue error, pid:%d", pid);
        return -1;
    }

    int stat = 0;
    // 对于使用ptrace_cont运行的子进程，它会在3种情况下进入暂停状态：①下一次系统调用；②子进程退出；③子进程的执行发生错误。
    // 参数WUNTRACED表示当进程进入暂停状态后，立即返回
    waitpid(pid, &stat, WUNTRACED);

    // 判断是否成功执行函数
    //printf("ptrace call ret status is %lX\n", stat);
    while (stat != 0xb7f){
        if (ptrace_continue(pid) == -1){
            //printf("[-] ptrace call error");
            return -1;
        }
        waitpid(pid, &stat, WUNTRACED);
    }

#elif defined(__arm__) || defined(__aarch64__) // 真机
#if defined(__arm__) // 32位真机
    int num_param_registers = 4;
#elif defined(__aarch64__) // 64位真机
    int num_param_registers = 8;
#endif
    int i = 0;
    // ARM处理器，函数传递参数，将前四个参数放到r0-r3，剩下的参数压入栈中
    for (i = 0 ; i < num_params && i < num_param_registers ; i++) {
        regs->uregs[i] = parameters[i];
    }

    if (i < num_params) {
        regs->ARM_sp -= (num_params - i) * sizeof(uintptr_t); // 分配栈空间，栈的方向是从高地址到低地址
        if (ptrace_writedata(pid, (uint8_t *) (regs->ARM_sp), (uint8_t *) &parameters[i], (num_params - i) * sizeof(uintptr_t)) == -1) {
            return -1;
        }
    }

    regs->ARM_pc = ExecuteAddr; //设置ARM_pc寄存器为需要调用的函数地址
    // 与BX跳转指令类似，判断跳转的地址位[0]是否为1，如果为1，则将CPST寄存器的标志T置位，解释为Thumb代码
    // 若为0，则将CPSR寄存器的标志T复位，解释为ARM代码
    if (regs->ARM_pc & 1) {
        /* thumb */
        regs->ARM_pc &= (~1u);
        regs->ARM_cpsr |= CPSR_T_MASK;
    } else {
        /* arm */
        regs->ARM_cpsr &= ~CPSR_T_MASK;
    }

    regs->ARM_lr = 0;

    // Android 7.0以上修正lr为libc.so的起始地址 getprop获取ro.build.version.sdk
    uintptr_t lr_val = 0;
    char sdk_ver[32];
    memset(sdk_ver, 0, sizeof(sdk_ver));
    __system_property_get("ro.build.version.sdk", sdk_ver);
    //    //printf("ro.build.version.sdk: %s", sdk_ver);
    if (atoi(sdk_ver) <= 23) {
        lr_val = 0;
    } else { // Android 7.0
        static uintptr_t start_ptr = 0;
        if (start_ptr == 0) {
            start_ptr = (uintptr_t) get_module_base_addr(pid, libc_path);
        }
        lr_val = start_ptr;
    }
    regs->ARM_lr = lr_val;

    if (ptrace_setregs(pid, regs) == -1 || ptrace_continue(pid) == -1) {
        //printf("[-] ptrace set regs or continue error, pid:%d\n", pid);
        return -1;
    }

    int stat = 0;
    // 对于使用ptrace_cont运行的子进程，它会在3种情况下进入暂停状态：①下一次系统调用；②子进程退出；③子进程的执行发生错误。
    // 参数WUNTRACED表示当进程进入暂停状态后，立即返回
    // 将ARM_lr（存放返回地址）设置为0，会导致子进程执行发生错误，则子进程进入暂停状态
    waitpid(pid, &stat, WUNTRACED);

    // 判断是否成功执行函数
    //printf("[+] ptrace call ret status is %d\n", stat);
    while ((stat & 0xFF) != 0x7f) {
        if (ptrace_continue(pid) == -1) {
            //printf("[-] ptrace call error\n");
            return -1;
        }
        waitpid(pid, &stat, WUNTRACED);
    }

    // 获取远程进程的寄存器值，方便获取返回值
    if (ptrace_getregs(pid, regs) == -1) {
        //printf("[-] After call getregs error\n");
        return -1;
    }

#else // 设备不符合注入器构架
    //printf("[-] Not supported Environment %s\n", __FUNCTION__);
#endif
    return 0;
}
