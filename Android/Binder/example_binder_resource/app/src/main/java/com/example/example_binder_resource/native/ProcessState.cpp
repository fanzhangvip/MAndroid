/*
 * Copyright (C) 2005 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#define LOG_TAG "ProcessState"

#include <cutils/process_name.h>

#include <binder/ProcessState.h>

#include <utils/Atomic.h>
#include <binder/BpBinder.h>
#include <binder/IPCThreadState.h>
#include <utils/Log.h>
#include <utils/String8.h>
#include <binder/IServiceManager.h>
#include <utils/String8.h>
#include <utils/threads.h>

#include <private/binder/binder_module.h>
#include <private/binder/Static.h>

#include <errno.h>
#include <fcntl.h>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <sys/ioctl.h>
#include <sys/mman.h>
#include <sys/stat.h>

#define BINDER_VM_SIZE ((1*1024*1024) - (4096 *2))

static bool gSingleProcess = false;


// ---------------------------------------------------------------------------

namespace android {
 
// Global variables
int                 mArgC;
const char* const*  mArgV;
int                 mArgLen;


class PoolThread : public Thread
{
public:
    PoolThread(bool isMain)
        : mIsMain(isMain)
    {
    }
protected:
    virtual bool threadLoop()
    {
        //线程函数如此简单，不过是在这个线程中又创建了一个IPCThreadState
        IPCThreadState::self()->joinThreadPool(mIsMain);
        return false;
    }
    const bool mIsMain;
};


//创建一个ProcessState
//每个进程只有一个ProcessState对象
sp<ProcessState> ProcessState::self()
{
    //static.cpp中定义的一个全局变量
    if (gProcess != NULL) return gProcess;
    AutoMutex _l(gProcessMutex);
    //创建一个ProcessState并赋值给gProgress
    if (gProcess == NULL) gProcess = new ProcessState;
    return gProcess;
}


void ProcessState::setSingleProcess(bool singleProcess)
{
    gSingleProcess = singleProcess;
}


void ProcessState::setContextObject(const sp<IBinder>& object)
{
    setContextObject(object, String16("default"));
}


sp<IBinder> ProcessState::getContextObject(const sp<IBinder>& caller)
{
    //根据open_driver()判断是否打开设备成功以判断是否支持进程
    //真实设备肯定是支持进程的
    if (supportsProcesses()) {
        //参数传为0，返回的是IBinder对象
        //handle表示该资源项在数组中的索引
        return getStrongProxyForHandle(0);
    } else {
        return getContextObject(String16("default"), caller);
    }
}


void ProcessState::setContextObject(const sp<IBinder>& object, const String16& name)
{
    AutoMutex _l(mLock);
    mContexts.add(name, object);
}

sp<IBinder> ProcessState::getContextObject(const String16& name, const sp<IBinder>& caller)
{
    mLock.lock();
    sp<IBinder> object(
        mContexts.indexOfKey(name) >= 0 ? mContexts.valueFor(name) : NULL);
    mLock.unlock();
    
    //printf("Getting context object %s for %p\n", String8(name).string(), caller.get());
    
    if (object != NULL) return object;

    // Don't attempt to retrieve contexts if we manage them
    if (mManagesContexts) {
        LOGE("getContextObject(%s) failed, but we manage the contexts!\n",
            String8(name).string());
        return NULL;
    }
    
    IPCThreadState* ipc = IPCThreadState::self();
    {
        Parcel data, reply;
        // no interface token on this magic transaction
        data.writeString16(name);
        data.writeStrongBinder(caller);
        status_t result = ipc->transact(0 /*magic*/, 0, data, &reply, 0);
        if (result == NO_ERROR) {
            object = reply.readStrongBinder();
        }
    }
    
    ipc->flushCommands();
    
    if (object != NULL) setContextObject(object, name);
    return object;
}

bool ProcessState::supportsProcesses() const
{
    return mDriverFD >= 0;
}

void ProcessState::startThreadPool()
{
    AutoMutex _l(mLock);
    //如果线程池已经开启了，这个函数就没有意义了
    if (!mThreadPoolStarted) {
        mThreadPoolStarted = true;
        //传入的是true
        spawnPooledThread(true);
    }
}

bool ProcessState::isContextManager(void) const
{
    return mManagesContexts;
}

bool ProcessState::becomeContextManager(context_check_func checkFunc, void* userData)
{
    if (!mManagesContexts) {
        AutoMutex _l(mLock);
        mBinderContextCheckFunc = checkFunc;
        mBinderContextUserData = userData;
        if (mDriverFD >= 0) {
            int dummy = 0;
#if defined(HAVE_ANDROID_OS)
            status_t result = ioctl(mDriverFD, BINDER_SET_CONTEXT_MGR, &dummy);
#else
            status_t result = INVALID_OPERATION;
#endif
            if (result == 0) {
                mManagesContexts = true;
            } else if (result == -1) {
                mBinderContextCheckFunc = NULL;
                mBinderContextUserData = NULL;
                LOGE("Binder ioctl to become context manager failed: %s\n", strerror(errno));
            }
        } else {
            // If there is no driver, our only world is the local
            // process so we can always become the context manager there.
            mManagesContexts = true;
        }
    }
    return mManagesContexts;
}

ProcessState::handle_entry* ProcessState::lookupHandleLocked(int32_t handle)
{
    const size_t N=mHandleToObject.size();
    if (N <= (size_t)handle) {
        handle_entry e;
        e.binder = NULL;
        e.refs = NULL;
        status_t err = mHandleToObject.insertAt(e, N, handle+1-N);
        if (err < NO_ERROR) return NULL;
    }
    return &mHandleToObject.editItemAt(handle);
}


//handle用于标识创建的BpBinder相对应的BBinder
sp<IBinder> ProcessState::getStrongProxyForHandle(int32_t handle)
{
    sp<IBinder> result;
    AutoMutex _l(mLock);

    //根据索引查找对应的资源
    //如果lookupHandleLocked发现没有对应的资源项，则会创建一个新的项返回
    //新项的内容需要填充
    handle_entry* e = lookupHandleLocked(handle);
    if (e != NULL) {
        // We need to create a new BpBinder if there isn't currently one, OR we
        // are unable to acquire a weak reference on this current one.  See comment
        // in getWeakProxyForHandle() for more info about this.
        IBinder* b = e->binder;
        if (b == NULL || !e->refs->attemptIncWeak(this)) {
            //对于新创建的资源项，它的binder为空，所以走这个分支，传入的handle值为0
            b = new BpBinder(handle);
            //填充entry的内容
            e->binder = b;
            if (b) e->refs = b->getWeakRefs();
            result = b;
        } else {
            // This little bit of nastyness is to allow us to add a primary
            // reference to the remote proxy when this team doesn't have one
            // but another team is sending the handle to us.
            result.force_set(b);
            e->refs->decWeak(this);
        }
    }
    //返回BpBinder(handle),注意handle的值为0
    return result;
}



wp<IBinder> ProcessState::getWeakProxyForHandle(int32_t handle)
{
    wp<IBinder> result;

    AutoMutex _l(mLock);

    handle_entry* e = lookupHandleLocked(handle);

    if (e != NULL) {        
        // We need to create a new BpBinder if there isn't currently one, OR we
        // are unable to acquire a weak reference on this current one.  The
        // attemptIncWeak() is safe because we know the BpBinder destructor will always
        // call expungeHandle(), which acquires the same lock we are holding now.
        // We need to do this because there is a race condition between someone
        // releasing a reference on this BpBinder, and a new reference on its handle
        // arriving from the driver.
        IBinder* b = e->binder;
        if (b == NULL || !e->refs->attemptIncWeak(this)) {
            b = new BpBinder(handle);
            result = b;
            e->binder = b;
            if (b) e->refs = b->getWeakRefs();
        } else {
            result = b;
            e->refs->decWeak(this);
        }
    }

    return result;
}

void ProcessState::expungeHandle(int32_t handle, IBinder* binder)
{
    AutoMutex _l(mLock);
    
    handle_entry* e = lookupHandleLocked(handle);

    // This handle may have already been replaced with a new BpBinder
    // (if someone failed the AttemptIncWeak() above); we don't want
    // to overwrite it.
    if (e && e->binder == binder) e->binder = NULL;
}

void ProcessState::setArgs(int argc, const char* const argv[])
{
    mArgC = argc;
    mArgV = (const char **)argv;

    mArgLen = 0;
    for (int i=0; i<argc; i++) {
        mArgLen += strlen(argv[i]) + 1;
    }
    mArgLen--;
}

int ProcessState::getArgC() const
{
    return mArgC;
}

const char* const* ProcessState::getArgV() const
{
    return mArgV;
}

void ProcessState::setArgV0(const char* txt)
{
    if (mArgV != NULL) {
        strncpy((char*)mArgV[0], txt, mArgLen);
        set_process_name(txt);
    }
}

//isMain参数为true
void ProcessState::spawnPooledThread(bool isMain)
{
    if (mThreadPoolStarted) {
        int32_t s = android_atomic_add(1, &mThreadPoolSeq);
        char buf[32];
        sprintf(buf, "Binder Thread #%d", s);
        LOGV("Spawning new pooled thread, name=%s\n", buf);
        //PoolThread是在IPCThreadState中定义的一个Thread子类
        sp<Thread> t = new PoolThread(isMain);
        t->run(buf);
    }
}

//打开/dev/device虚拟设备
static int open_driver()
{
    if (gSingleProcess) {
        return -1;
    }
    int fd = open("/dev/binder", O_RDWR);
    if (fd >= 0) {
        fcntl(fd, F_SETFD, FD_CLOEXEC);
        int vers;
#if defined(HAVE_ANDROID_OS)
        status_t result = ioctl(fd, BINDER_VERSION, &vers);
#else
        status_t result = -1;
        errno = EPERM;
#endif
        if (result == -1) {
            LOGE("Binder ioctl to obtain version failed: %s", strerror(errno));
            close(fd);
            fd = -1;
        }
        if (result != 0 || vers != BINDER_CURRENT_PROTOCOL_VERSION) {
            LOGE("Binder driver protocol does not match user space protocol!");
            close(fd);
            fd = -1;
        }
#if defined(HAVE_ANDROID_OS)
        size_t maxThreads = 15;
        //通过ioctl方法告诉binder驱动，这个fd支持的最大线程数是15个
        result = ioctl(fd, BINDER_SET_MAX_THREADS, &maxThreads);
        if (result == -1) {
            LOGE("Binder ioctl to set max threads failed: %s", strerror(errno));
        }
#endif
    } else {
        LOGW("Opening '/dev/binder' failed: %s\n", strerror(errno));
    }
    return fd;
}



/**
- 打开/dev/binder设备，这就相当于与内核的Binder驱动有了交互的通道。
- 对返回的fd使用mmap，这样Binder驱动就会分配一块内存来接受数据。
- 由于ProcessState具有唯一性，因此一个进程只打开设备一次。
*/

//构造方法
ProcessState::ProcessState()
    //open_driver()打开/dev/binder设备(Android在内核中为完成进程间通信而专门设置的一个虚拟设备)
    : mDriverFD(open_driver())
    //映射内存的起始地址
    , mVMStart(MAP_FAILED)
    , mManagesContexts(false)
    , mBinderContextCheckFunc(NULL)
    , mBinderContextUserData(NULL)
    , mThreadPoolStarted(false)
    , mThreadPoolSeq(1)
{
    if (mDriverFD >= 0) {
        // XXX Ideally, there should be a specific define for whether we
        // have mmap (or whether we could possibly have the kernel module
        // availabla).
#if !defined(HAVE_WIN32_IPC)
        #define BINDER_VM_SIZE ((1*1024*1024) - (4096 *2))//1M - 8K
           /**
           mmap()这个函数真正的实现和驱动有关系，而Binder驱动会分配一块内存来接受数据
           */
        // mmap the binder, providing a chunk of virtual address space to receive transactions.
        mVMStart = mmap(0, BINDER_VM_SIZE, PROT_READ, MAP_PRIVATE | MAP_NORESERVE, mDriverFD, 0);
        if (mVMStart == MAP_FAILED) {
            // *sigh*
            LOGE("Using /dev/binder failed: unable to mmap transaction memory.\n");
            close(mDriverFD);
            mDriverFD = -1;
        }
#else
        mDriverFD = -1;
#endif
    }
    if (mDriverFD < 0) {
        // Need to run without the driver, starting our own thread pool.
    }
}



ProcessState::~ProcessState()
{
}
        
}; // namespace android
