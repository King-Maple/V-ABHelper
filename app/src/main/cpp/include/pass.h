// system lib
#include <asm/ptrace.h>
#include <sys/ptrace.h>
#include <sys/wait.h>
#include <cerrno>
#include <cstring>
#include <sys/mman.h>
#include <dlfcn.h>
#include <dirent.h>
#include <elf.h>
#include <sys/uio.h>
#include <unistd.h>
#include <fcntl.h>
#include <vector>

// user lib
#include "ptrace_utils.h"

class InjectTools {
private:
    pid_t pid;
    
    struct pt_regs CurrentRegs, OriginalRegs;

	void* syscall_addr;
	void* ftruncate_addr;
	void* write_addr;
	void* fcntl_addr;
	void* close_addr;
	void* mmap_addr;
	void* munmap_addr;
	void* dlopen_addr;
	void* dlclose_addr;
	void* dlsym_addr;
	
    size_t lastSize = 0;

    static InjectTools *sInjectTools;
public:
    InjectTools(pid_t pid) {
        if(pid == -1){
            pid = getpid();
        }
        this->pid = pid;
    }
	
	

    static InjectTools *getInstance(pid_t pid);

    bool injectStart( );

    bool injectEnd( );
	
	bool emptyRemoteMemory(uintptr_t remoteAddr, size_t size);
	
	bool writeRemoteMemory(uintptr_t remoteAddr, void *buffer, size_t size);
	
	bool readRemoteMemory(uintptr_t remoteAddr, void *buffer, size_t size);
	
	long callSyscall(std::vector<uintptr_t>& args);
	
	int callFtruncate(int fd, off_t length);
	
	ssize_t callWrite(int fd, uintptr_t buf, size_t count);
	
	int callFcntl(int fd, int cmd, int arg);
	
	int callClose(int fd);
	
	uintptr_t callMmap(uintptr_t addr, size_t length, int prot, int flags, int fd, off_t offset);
	
	int callMunmap(uintptr_t addr, size_t length);
	
	uintptr_t callDlopenExt(uintptr_t filename, int flags,uintptr_t extinfo);
	
	int callDlclose(uintptr_t handle);
	
	uintptr_t callDlsym(uintptr_t handle, uintptr_t symbol);
	
	uintptr_t call(uintptr_t remoteFuncitonAddr, std::vector<uintptr_t>& args);
	
	~InjectTools();

};

