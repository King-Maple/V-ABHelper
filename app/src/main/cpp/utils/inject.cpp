#include <malloc.h>
#include <android/dlext.h>
#include "pass.h"
#include "LogUtils.h"

InjectTools *InjectTools::sInjectTools = NULL;

extern const char* libc_path;
extern const char* linker_path;
extern const char* libdl_path;

InjectTools *InjectTools::getInstance(pid_t pid) {
    if(!sInjectTools){
        sInjectTools = new InjectTools(pid);
    }
	
    return sInjectTools;
}


bool InjectTools::injectStart( ) {
    // attach到目标进程
    if (ptrace_attach(pid) != 0) {
        return false;
    }
    // CurrentRegs 当前寄存器
    // OriginalRegs 保存注入前寄存器
    if (ptrace_getregs(pid, &CurrentRegs) != 0) {
        //进程不存在
        return false;
    }
    // 保存原始寄存器
    memcpy(&OriginalRegs, &CurrentRegs, sizeof(CurrentRegs));
	
	syscall_addr = get_remote_func_addr (pid,(void*)syscall);
	ftruncate_addr = get_remote_func_addr (pid,(void*)ftruncate);
	write_addr = get_remote_func_addr (pid,(void*)write);
	fcntl_addr = get_remote_func_addr (pid,(void*)fcntl);
	close_addr = get_remote_func_addr (pid,(void*)close);
	mmap_addr = get_remote_func_addr (pid,(void*)mmap);
	munmap_addr = get_remote_func_addr (pid,(void*)munmap);
	dlopen_addr = get_remote_func_addr (pid,(void*)android_dlopen_ext);
	dlclose_addr = get_remote_func_addr (pid,(void*)dlclose);
	dlsym_addr = get_remote_func_addr (pid,(void*)dlsym);
	
	
	
    return true;
}

bool InjectTools::injectEnd( ) {
    //设置规则
    if (ptrace_setregs(pid, &OriginalRegs) == -1) {
        //设置规则错误
        return false;
    }
    // 解除attach
    ptrace_detach(pid);
    return true;
}

bool InjectTools::emptyRemoteMemory (uintptr_t remoteAddr, size_t size) {
	void* tmpMemory = malloc(size);
	memset(tmpMemory, 0, size);
	if (ptrace_writedata(pid, (uint8_t *) remoteAddr, (uint8_t *) tmpMemory, size) == -1) {
		free(tmpMemory);
		return false;
	}
	free(tmpMemory);
	return true;
}



bool InjectTools::readRemoteMemory (uintptr_t remoteAddr, void* buffer, size_t size) {
	if (ptrace_readdata(pid, (uint8_t *) buffer, (uint8_t *) remoteAddr, size) == -1) {
		return false;
	}
	return true;
}

bool InjectTools::writeRemoteMemory(uintptr_t remoteAddr, void* buffer, size_t size) {
	if (ptrace_writedata(pid, (uint8_t *) remoteAddr, (uint8_t *) buffer, size) == -1) {
		return false;
	}
	return true;
}

long InjectTools::callSyscall (std::vector<uintptr_t> &args) {
	if (ptrace_call(pid, (uintptr_t) syscall_addr, &args[0], args.size(), &CurrentRegs) == -1) {
		return 0;
	}
	return ptrace_getret(&CurrentRegs);
}

int InjectTools::callFtruncate (int fd, off_t length) {
	std::vector<uintptr_t> args;
	args.push_back(fd);
	args.push_back(length);
	if (ptrace_call(pid, (uintptr_t) ftruncate_addr, &args[0], args.size(), &CurrentRegs) == -1) {
		return 0;
	}
	return ptrace_getret(&CurrentRegs);
}

ssize_t InjectTools::callWrite (int fd,uintptr_t buf, size_t count) {
	std::vector<uintptr_t> args;
	args.push_back(fd);
	args.push_back( buf);
	args.push_back(count);
	if (ptrace_call(pid, (uintptr_t) write_addr, &args[0], args.size(), &CurrentRegs) == -1) {
		return 0;
	}
	return ptrace_getret(&CurrentRegs);
}

int InjectTools::callFcntl (int fd, int cmd, int arg) {
	std::vector<uintptr_t> args;
	args.push_back(fd);
	args.push_back(cmd);
	args.push_back(arg);
	if (ptrace_call(pid, (uintptr_t) fcntl_addr, &args[0], args.size(), &CurrentRegs) == -1) {
		return 0;
	}
	return ptrace_getret(&CurrentRegs);
}



int InjectTools::callClose (int fd) {
	
	std::vector<uintptr_t> args;
	args.push_back(fd);
	if (ptrace_call(pid, (uintptr_t) close_addr, &args[0], args.size(), &CurrentRegs) == -1) {
		return 0;
	}
	return ptrace_getret(&CurrentRegs);
}

uintptr_t InjectTools::callMmap (uintptr_t addr, size_t length, int prot, int flags, int fd, off_t offset) {
	std::vector<uintptr_t> args;
	args.push_back(addr);
	args.push_back(length);
	args.push_back(prot);
	args.push_back(flags);
	args.push_back(fd);
	args.push_back(offset);
	if (ptrace_call(pid, (uintptr_t) mmap_addr, &args[0], args.size(), &CurrentRegs) == -1) {
		return 0;
	}
	return ptrace_getret(&CurrentRegs);
}

int InjectTools::callMunmap (uintptr_t addr, size_t length) {
	std::vector<uintptr_t> args;
	args.push_back(addr);
	args.push_back(length);
	if (ptrace_call(pid, (uintptr_t) munmap_addr, &args[0], args.size(), &CurrentRegs) == -1) {
		return 0;
	}
	return ptrace_getret(&CurrentRegs);
}

uintptr_t InjectTools::callDlopenExt (uintptr_t filename, int flags, uintptr_t extinfo) {
	std::vector<uintptr_t> args;
	args.push_back(filename);
	args.push_back(flags);
	args.push_back(extinfo);
	if (ptrace_call(pid, (uintptr_t) dlopen_addr, &args[0], args.size(), &CurrentRegs) == -1) {
		return 0;
	}
	return ptrace_getret(&CurrentRegs);
}

int InjectTools::callDlclose (uintptr_t handle) {
	std::vector<uintptr_t> args;
	args.push_back((uintptr_t) handle);
	if (ptrace_call(pid, (uintptr_t) dlclose_addr, &args[0], args.size(), &CurrentRegs) == -1) {
		return 0;
	}
	return ptrace_getret(&CurrentRegs);
}

uintptr_t InjectTools::callDlsym (uintptr_t handle, uintptr_t symbol) {
	std::vector<uintptr_t> args;
	args.push_back(handle);
	args.push_back(symbol);
	if (ptrace_call(pid, (uintptr_t) dlsym_addr, &args[0], args.size(), &CurrentRegs) == -1) {
		return 0;
	}
	return ptrace_getret(&CurrentRegs);
}

InjectTools::~InjectTools ( ) {
	this->injectEnd ();
	
}

uintptr_t InjectTools::call (uintptr_t remoteFuncitonAddr, std::vector<uintptr_t> &args) {
	if (ptrace_call(pid,  remoteFuncitonAddr, &args[0], args.size (), &CurrentRegs) == -1) {
		return NULL;
	}
	return ptrace_getret(&CurrentRegs);
}
