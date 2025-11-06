//
// Created by weishu on 2022/12/9.
//

#ifndef KERNELSU_KSU_H
#define KERNELSU_KSU_H

#include <cstdint>
#include <sys/ioctl.h>
#include <utility>

#define KSU_INSTALL_MAGIC1 0xDEADBEEF
#define KSU_INSTALL_MAGIC2 0xCAFEBABE

#define CMD_GET_VERSION 2

#define KSU_IOCTL_GET_INFO _IOC(_IOC_READ, 'K', 2, 0)

struct ksu_get_info_cmd {
    uint32_t version; // Output: KERNEL_SU_VERSION
    uint32_t flags;   // Output: flags (bit 0: MODULE mode)
    uint32_t features; // Output: max feature ID supported (KSU_FEATURE_MAX)
};

uint32_t get_version();

bool is_lkm_mode();


inline std::pair<int, int> legacy_get_info() {
    int32_t version = -1;
    int32_t flags = 0;
    int32_t result = 0;
    prctl(KSU_INSTALL_MAGIC1, CMD_GET_VERSION, &version, &flags, &result);
    return {version, flags};
}

int findValidateFunction(int pid);

template <typename T> T readMemory(int fd, uint64_t addr) {
    T value;
    pread64(fd, &value, sizeof(T), addr);
    return value;
}

template <typename T> bool writeMemory(int fd, uint64_t addr, T value) {
    return pwrite64(fd, &value, sizeof(T), addr) > 0;
}

#endif //KERNELSU_KSU_H
