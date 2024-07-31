

#ifndef _PTRACE_UTILS_H
#define _PTRACE_UTILS_H


// system lib
#include <asm/ptrace.h>
#include <sys/ptrace.h>
#include <sys/wait.h>
#include <cstdio>
#include <cerrno>
#include <cstring>
#include <sys/mman.h>
#include <dlfcn.h>
#include <dirent.h>
#include <elf.h>
#include <sys/uio.h>
#include <unistd.h>
#include <fcntl.h>

// 各构架预定义
#if defined(__aarch64__) // 真机64位
#define pt_regs user_pt_regs
#define uregs regs
#define ARM_pc pc
#define ARM_sp sp
#define ARM_cpsr pstate
#define ARM_lr regs[30]
#define ARM_r0 regs[0]
// 这两个宏定义比较有意思 意思就是在 arm64下
// 强制 PTRACE_GETREGS 为 PTRACE_GETREGSET 这种
#define PTRACE_GETREGS PTRACE_GETREGSET
#define PTRACE_SETREGS PTRACE_SETREGSET
#elif defined(__x86_64__) // ？？未知架构
#define pt_regs user_regs_struct
#define eax rax
#define esp rsp
#define eip rip
#elif defined(__i386__) // 模拟器
#define pt_regs user_regs_struct
#endif

// 其余预定义
#define CPSR_T_MASK (1u << 5)

/**
 * @brief Get the pid by pkg_name
 * 成功返回 true
 * 失败返回 false
 *
 * @param pid
 * @param task_name
 * @return true
 * @return false
 */
pid_t get_pid_by_name(const char *task_name);

/**
 * @brief 在指定进程中搜索对应模块的基址
 *
 * @param pid pid表示远程进程的ID 若为-1表示自身进程
 * @param ModuleName ModuleName表示要搜索的模块的名称
 * @return void* 返回0表示获取模块基址失败，返回非0为要搜索的模块基址
 */
void *get_module_base_addr(pid_t pid, const char *ModuleName);

/**
 * @brief 在指定进程中搜索对应模块的尾地址
 *
 * @param pid pid表示远程进程的ID 若为-1表示自身进程
 * @param ModuleName ModuleName表示要搜索的模块的名称
 * @return void* 返回0表示获取失败，返回非0为要搜索的地址
 */
void * get_module_end(pid_t pid, const char *module_name);


/**
 * @brief 获取远程进程与本进程都加载的模块中函数的地址
 *
 * @param pid pid表示远程进程的ID
 * @param ModuleName ModuleName表示模块名称
 * @param LocalFuncAddr LocalFuncAddr表示本地进程中该函数的地址
 * @return void* 返回远程进程中对应函数的地址
 */
void *get_remote_func_addr(pid_t pid, void *LocalFuncAddr);

/**
 * @brief 使用ptrace Attach附加到指定进程,发送SIGSTOP信号给指定进程让其停止下来并对其进行跟踪。
 * 但是被跟踪进程(tracee)不一定会停下来，因为同时attach和传递SIGSTOP可能会将SIGSTOP丢失。
 * 所以需要waitpid(2)等待被跟踪进程被停下
 *
 * @param pid pid表示远程进程的ID
 * @return int 返回0表示attach成功，返回-1表示失败
 */
int ptrace_attach(pid_t pid);

/**
 * @brief ptrace使远程进程继续运行
 *
 * @param pid pid表示远程进程的ID
 * @return int 返回0表示continue成功，返回-1表示失败
 */
int ptrace_continue(pid_t pid);

/**
 * @brief 使用ptrace detach指定进程,完成对指定进程的跟踪操作后，使用该参数即可解除附加
 *
 * @param pid pid表示远程进程的ID
 * @return int 返回0表示detach成功，返回-1表示失败
 */
int ptrace_detach(pid_t pid);

/**
 * @brief 使用ptrace获取远程进程的寄存器值
 *
 * @param pid pid表示远程进程的ID
 * @param regs regs为pt_regs结构，存储了寄存器值
 * @return int 返回0表示获取寄存器成功，返回-1表示失败
 */
int ptrace_getregs(pid_t pid, struct pt_regs *regs);

/**
 * @brief 使用ptrace设置远程进程的寄存器值
 *
 * @param pid pid表示远程进程的ID
 * @param regs regs为pt_regs结构 存储需要修改的寄存器值
 * @return int 返回0表示设置寄存器成功 返回-1表示失败
 */
int ptrace_setregs(pid_t pid, struct pt_regs *regs);

/**
 * @brief 获取返回值，ARM处理器中返回值存放在ARM_r0寄存器中
 * @param regs regs存储远程进程当前的寄存器值
 * @return 在ARM处理器下返回r0寄存器值
 */
uintptr_t ptrace_getret(struct pt_regs *regs);

/**
 * @brief 获取当前执行代码的地址 ARM处理器下存放在ARM_pc中
 * @param regs regs存储远程进程当前的寄存器值
 * @return 在ARM处理器下返回pc寄存器值
 */
uintptr_t ptrace_getpc(struct pt_regs *regs);
/**
 * @brief 使用ptrace从远程进程内存中读取数据
 * 这里的*_t类型是typedef定义一些基本类型的别名，用于跨平台。例如uint8_t表示无符号8位也就是无符号的char类型
 * @param pid pid表示远程进程的ID
 * @param pSrcBuf pSrcBuf表示从远程进程读取数据的内存地址
 * @param pDestBuf pDestBuf表示用于存储读取出数据的地址
 * @param size size表示读取数据的大小
 * @return 返回0表示读取数据成功
 */
int ptrace_readdata(pid_t pid, uint8_t *pSrcBuf, uint8_t *pDestBuf, size_t size);

/**
 * @brief 使用ptrace将数据写入到远程进程空间中
 *
 * @param pid pid表示远程进程的ID
 * @param pWriteAddr pWriteAddr表示写入数据到远程进程的内存地址
 * @param pWriteData pWriteData用于存储写入数据的地址
 * @param size size表示写入数据的大小
 * @return int 返回0表示写入数据成功，返回-1表示写入数据失败
 */
int ptrace_writedata(pid_t pid, uint8_t *pWriteAddr, uint8_t *pWriteData, size_t size);

/**
 * @brief 使用ptrace远程call函数
 *
 * @param pid pid表示远程进程的ID
 * @param ExecuteAddr ExecuteAddr为远程进程函数的地址
 * @param parameters parameters为函数参数的地址
 * @param num_params regs为远程进程call函数前的寄存器环境
 * @param regs
 * @return 返回0表示call函数成功，返回-1表示失败
 */
int ptrace_call(pid_t pid, uintptr_t ExecuteAddr, uintptr_t *parameters, uintptr_t num_params, struct pt_regs *regs);

#endif //终结者_PTRACE_UTILS_H
