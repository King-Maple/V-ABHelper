#include <LogUtils.h>
#include <ptrace_utils.h>
#include <string>
#include <update_metadata.pb.h>
#include <dobby.h>

#define _u_int_val(p)               reinterpret_cast<uintptr_t>(p)
#define _ptr(p)                   (reinterpret_cast<void *>(p))
#define _align_up(x, n)           (((x) + ((n) - 1)) & ~((n) - 1))
#define _align_down(x, n)         ((x) & -(n))
#define _page_size                4096
#define _page_align(n)            _align_up(static_cast<uintptr_t>(n), _page_size)
#define _ptr_align(x)             _ptr(_align_down(reinterpret_cast<uintptr_t>(x), _page_size))
#define make_rwx(p, n)           ::mprotect(_ptr_align(p), \
                                              _page_align(_u_int_val(p) + (n)) != _page_align(_u_int_val(p)) ? _page_align(n) + _page_size : _page_align(n), \
                                              PROT_READ | PROT_WRITE | PROT_EXEC)

using DeltaArchiveManifest = chromeos_update_engine::DeltaArchiveManifest;
using PartitionUpdate = chromeos_update_engine::PartitionUpdate;
using InstallOperation = chromeos_update_engine::InstallOperation;


bool (*orig_VerifyPayloadParseManifest)(char * metadata_filename, DeltaArchiveManifest* manifest, int64_t error);
bool new_VerifyPayloadParseManifest(char * metadata_filename, DeltaArchiveManifest* manifest, int64_t error) {
    bool ret = orig_VerifyPayloadParseManifest(metadata_filename, manifest, error);
    for (PartitionUpdate partition : manifest->partitions()) {
        partition.clear_old_partition_info();
        LOGD("partition_name = %s", partition.partition_name().c_str());
    }
    LOGD("VerifyPayloadParseManifest = call");
    return ret;
}


extern "C" [[gnu::visibility("default")]] [[gnu::used]]  void entry(int offset){
    auto baseAddr = (uint64_t)get_module_base_addr(-1, "/system/bin/update_engine");
    LOGD("update_engine baseAddr = %lx offset = 0x%x", baseAddr, offset);
    if (baseAddr > 0 && offset > 0) {
        offset = 0x18C120;
        //make_rwx((void *) (baseAddr + offset), _page_size);
        //int ret = DobbyHook((void *) (baseAddr + offset), (void *) new_ParseVerityConfig,(void **) &orig_ParseVerityConfig);
        //LOGD("DobbyHook = %d", ret);
    }
}
