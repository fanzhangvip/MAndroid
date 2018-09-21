/*
**
** Copyright 2008, The Android Open Source Project
**
** Licensed under the Apache License, Version 2.0 (the "License");
** you may not use this file except in compliance with the License.
** You may obtain a copy of the License at
**
**     http://www.apache.org/licenses/LICENSE-2.0
**
** Unless required by applicable law or agreed to in writing, software
** distributed under the License is distributed on an "AS IS" BASIS,
** WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
** See the License for the specific language governing permissions and
** limitations under the License.
*/

// System headers required for setgroups, etc.
#include <sys/types.h>
#include <unistd.h>
#include <grp.h>

#include <binder/IPCThreadState.h>
#include <binder/ProcessState.h>
#include <binder/IServiceManager.h>
#include <utils/Log.h>

#include <AudioFlinger.h>
#include <CameraService.h>
#include <MediaPlayerService.h>
#include <AudioPolicyService.h>
#include <private/android_filesystem_config.h>

using namespace android;

int main(int argc, char** argv)
{
    //1、获得一个ProcessState实例
    /**
    - 打开/dev/binder设备，这就相当于与内核的Binder驱动有了交互的通道。
    - 对返回的fd使用mmap，这样Binder驱动就会分配一块内存来接受数据。
    - 由于ProcessState具有唯一性，因此一个进程只打开设备一次。
    */
    sp<ProcessState> proc(ProcessState::self());
    //2、MediaServer作为ServiceManager的客户端，需要想ServiceManager注册服务
    /**
    - 有一个BpBinder对象，它的handle值是0
    - 有一个BpServiceManager对象，它的mRemote值是BpBinder
    - BpServiceManager对象实现IServiceManager的业务函数，现在又有BpBinder作为通信的代表
    */
    sp<IServiceManager> sm = defaultServiceManager();
    LOGI("ServiceManager: %p", sm.get());

    //一共注册了4个服务
    //初始化音频系统的AudioFlinger服务
    AudioFlinger::instantiate();
    //3、多媒体系统的MediaPlayer服务，我们将以它作为主切入点
    MediaPlayerService::instantiate();
    //CameraService服务
    CameraService::instantiate();
    //音频系统的AudioPolicy服务
    AudioPolicyService::instantiate();

    //有两个线程为Service服务
    //1、startThreadPool中新启动的线程通过joinThreadPool读取binder设备，查看是否有请求
    //2、主线程也调用joinThreadPool读取binder设备，查看是否有请求。binder设备是支持多线程操作的。


    //4、根据名称像是要创建一个线程池
    ProcessState::self()->startThreadPool();
    //5、加入创建的线程池
    IPCThreadState::self()->joinThreadPool();
}
