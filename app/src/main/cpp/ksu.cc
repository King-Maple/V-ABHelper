//
// Created by weishu on 2022/12/9.
//

#include <sys/prctl.h>
#include <cstdint>
#include <cstring>
#include <cstdio>
#include <unistd.h>
#include <utility>
#include <android/log.h>
#include <dirent.h>
#include <cstdlib>

#include <unistd.h>
#include <climits>
#include <sys/syscall.h>
#include <LogUtils.h>
#include "ksu.h"
#include "ptrace_utils.h"


static int fd = -1;

static inline int scan_driver_fd() {
    const char *kName = "[ksu_driver]";
    DIR *dir = opendir("/proc/self/fd");
    if (!dir) {
        return -1;
    }
    int found = -1;
    struct dirent *de;
    char path[64];
    char target[PATH_MAX];
    while ((de = readdir(dir)) != nullptr) {
        if (de->d_name[0] == '.') {
            continue;
        }
        char *endptr = nullptr;
        long fd_long = strtol(de->d_name, &endptr, 10);
        if (!de->d_name[0] || *endptr != '\0' || fd_long < 0 || fd_long > INT_MAX) {
            continue;
        }
        snprintf(path, sizeof(path), "/proc/self/fd/%s", de->d_name);
        ssize_t n = readlink(path, target, sizeof(target) - 1);
        if (n < 0) {
            continue;
        }
        target[n] = '\0';
        const char *base = strrchr(target, '/');
        base = base ? base + 1 : target;
        if (strstr(base, kName)) {
            found = (int)fd_long;
            break;
        }
    }
    closedir(dir);
    return found;
}

// 这里需要先安装 Ksu 的Fd
int init_driver_fd() {
    int fd_ = scan_driver_fd();
    if (fd_ <= 0) {
        syscall(SYS_reboot, KSU_INSTALL_MAGIC1, KSU_INSTALL_MAGIC2, 0, &fd_);
    }
    return fd_;
}

template<typename... Args>
static int ksuctl(unsigned long op, Args &&... args) {
    if (fd < 0) {
        fd = init_driver_fd();
    }
    static_assert(sizeof...(Args) <= 1, "ioctl expects at most one extra argument");
    return ioctl(fd, op, std::forward<Args>(args)...);
}


static struct ksu_get_info_cmd g_version {};

struct ksu_get_info_cmd get_info() {
    if (!g_version.version) {
        ksuctl(KSU_IOCTL_GET_INFO, &g_version);
    }
    return g_version;
}

uint32_t get_version() {
    auto info = get_info();
    return info.version;
}


bool is_lkm_mode() {
    auto info = get_info();
    if (info.version > 0) {
        return (info.flags & 0x1) != 0;
    }
    return (legacy_get_info().second & 0x1) != 0;
}


// 查找子字符串的函数
unsigned char *findBytes (unsigned char *data, size_t dataSize, const unsigned char *subData, size_t subDataSize) {
    if (subDataSize == 0) { return data; }
    for (size_t i = 0; i <= dataSize - subDataSize; i++) {
        size_t j;
        for (j = 0; j < subDataSize; j++) {
            if (data[i + j] != subData[j]) {
                break;
            }
        }
        if (j == subDataSize) {
            return data + i;
        }
    }
    return nullptr;
}

#define X0 0
#define SP 31

static bool operates (uint32_t instruction,uint32_t reg) {
    uint32_t rd_mask = 0x1F;
    uint32_t rd = instruction & rd_mask;
    return rd == reg;
}

static uint32_t getAdrpValue (uint32_t instruction, uint64_t file_offset) {
    uint32_t immhi = (instruction >> 5) & 0x7FFFF;
    uint32_t immlo = (instruction >> 29) & 0x3;
    uint32_t imm = (immhi << 2) | immlo;
    uint64_t address = imm << 12;
    uint64_t final_address = address + (file_offset & ~0xFFF);
    return final_address;
}

static bool isAdrp (uint32_t instruction) {
    uint32_t opcode_mask = 0x9F000000;
    uint32_t adrp_pattern = 0x90000000;
    return (instruction & opcode_mask) == adrp_pattern;
}

static uint32_t getAddValue (uint32_t instruction) {
    uint32_t imm_mask = 0x003FFC00;
    uint32_t imm = (instruction & imm_mask) >> 10;
    return imm;
}

static bool isAdd (uint32_t instruction) {
    uint32_t opcode_mask = 0x7F000000;
    uint32_t add_imm_pattern = 0x11000000;
    return (instruction & opcode_mask) == add_imm_pattern;
}

static bool isSub (uint32_t instruction) {
    uint32_t opcode_mask = 0x7F000000;
    uint32_t sub_imm_pattern = 0x51000000;
    return (instruction & opcode_mask) == sub_imm_pattern;
}

ssize_t read_proc(int pid, uintptr_t remote_addr, void *buf, size_t len) {
    struct iovec local{
            .iov_base = (void *) buf,
            .iov_len = len
    };
    struct iovec remote{
            .iov_base = (void *) remote_addr,
            .iov_len = len
    };
    auto l = process_vm_readv(pid, &local, 1, &remote, 1, 0);
    return l;
}

int findValidateFunction(int pid) {
    int offset = -1;
    if (pid <= 0)
        return offset;
    auto start_addr = (uintptr_t )get_module_base_addr(pid, "/system/bin/update_engine");
    auto end_addr = (uintptr_t )get_module_end(pid, "/system/bin/update_engine");
    LOGD("start_addr: %lx, end_addr: %lx", start_addr, end_addr);
    size_t libsize = (end_addr - start_addr);
    if (libsize <= 0)
        return offset;
    LOGD("Lib Size: %zu", libsize);
    auto *buffer = (uint8_t *)malloc(libsize);
    memset(buffer, '\0', libsize);
    read_proc(pid, start_addr, buffer, libsize);

    //需要搜索的字符串
    static const char *str = "Failed to read metadata and signature from";
    //在文件数据中查找字符串
    unsigned char *strInData = findBytes(buffer, libsize, (unsigned char *) str, strlen(str));
    //计算字符串在文件中的偏移
    uint32_t strOffset = strInData - buffer;
    LOGD("strOffset [ %x ]",strOffset);
    //引用字符串的指令地址
    unsigned char *citeStr = nullptr;
    //在文件中搜索字符串引用的指令
    for (unsigned char *addr = buffer; addr < buffer + libsize; addr += 4) {
        //指令
        uint32_t instruction = *(uint32_t *) addr;
        //判断指令是不是adrp,是不是操作的x0寄存器
        if (isAdrp(instruction) && operates(instruction,X0)) {
            //adrp操作x0的值
            uint32_t regValue = getAdrpValue(instruction, addr - buffer);
            //找到adrp操作x0的值后,向后查找add指令
            for (unsigned char *subAddr = addr + 0x4; subAddr < addr + 0x10; subAddr += 4) {
                //指令
                uint32_t subInstruction = *(uint32_t *) subAddr;
                //判断是不是add指令,不是add指令则跳出子循环,因为高通是adrp(x0) + add(x0),联发科是adrp(x0) + add + add(x0)
                if (isAdd(subInstruction)) {
                    //我们只需要操作x0的值,所以判断指令操作的是不是x0
                    if (operates(subInstruction,X0)) {
                        //计算x0的值
                        regValue += getAddValue(subInstruction);
                    }
                } else {
                    break;
                }
            }
            //判断x0的值是不是等于字符串的偏移,然后取出地址
            if (strOffset == regValue) {
                citeStr = addr;
                break;
            }
        }
    }

    if (citeStr) {
        unsigned char* funcHead = nullptr;
        for (unsigned char *addr = citeStr - 0x4;addr > buffer;addr -= 0x4){
            uint32_t instruction = *(uint32_t *) addr;
            if (isSub(instruction) && operates(instruction,SP)){
                if(*(uint32_t *) (addr - 0x4) == 0xD503233F){
                    funcHead = addr - 0x4;
                }else{
                    funcHead = addr;
                }
                break;
            }
        }
        offset =  funcHead - buffer;
    }
    free(buffer);
    LOGD("func offset [ %x ] ",offset);
    return offset;
}