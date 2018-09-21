/*
** Copyright 2010, The Android Open Source Project
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

//#define LOG_NDEBUG 0
#define LOG_TAG "IMediaDeathNotifier"
#include <utils/Log.h>

#include <binder/IServiceManager.h>
#include <binder/IPCThreadState.h>
#include <media/IMediaDeathNotifier.h>

namespace android {

// client singleton for binder interface to services
Mutex IMediaDeathNotifier::sServiceLock;
sp<IMediaPlayerService> IMediaDeathNotifier::sMediaPlayerService;
sp<IMediaDeathNotifier::DeathNotifier> IMediaDeathNotifier::sDeathNotifier;
SortedVector< wp<IMediaDeathNotifier> > IMediaDeathNotifier::sObitRecipients;


//这个函数通过与ServiceManager通信，获得一个能够与MediaPlayerService通信的BpBinder
//然后通过interface_cast转换成一个BpMediaPlayerService
// establish binder interface to MediaPlayerService
/*static*/const sp<IMediaPlayerService>&
IMediaDeathNotifier::getMediaPlayerService()
{
    LOGV("getMediaPlayerService");
    Mutex::Autolock _l(sServiceLock);
    if (sMediaPlayerService.get() == 0) {
        sp<IServiceManager> sm = defaultServiceManager();
        sp<IBinder> binder;
        do {
            //向ServiceManager查询对应服务的信息，返回BpBinder
            binder = sm->getService(String16("media.player"));
            if (binder != 0) {
                break;
             }
             //如果ServiceMananger上还没有注册对应的服务，则需要等待，直到对应服务器注册到ServiceManager为止
             LOGW("Media player service not published, waiting...");
             usleep(500000); // 0.5 s
        } while(true);

        if (sDeathNotifier == NULL) {
        sDeathNotifier = new DeathNotifier();
    }
    binder->linkToDeath(sDeathNotifier);
    //通过interface_cast，将这个binder转换成BpMediaPlayerService
    sMediaPlayerService = interface_cast<IMediaPlayerService>(binder);
    }
    LOGE_IF(sMediaPlayerService == 0, "no media player service!?");
    return sMediaPlayerService;
}

/*static*/ void
IMediaDeathNotifier::addObitRecipient(const wp<IMediaDeathNotifier>& recipient)
{
    LOGV("addObitRecipient");
    Mutex::Autolock _l(sServiceLock);
    sObitRecipients.add(recipient);
}

/*static*/ void
IMediaDeathNotifier::removeObitRecipient(const wp<IMediaDeathNotifier>& recipient)
{
    LOGV("removeObitRecipient");
    Mutex::Autolock _l(sServiceLock);
    sObitRecipients.remove(recipient);
}

void
IMediaDeathNotifier::DeathNotifier::binderDied(const wp<IBinder>& who) {
    LOGW("media server died");

    // Need to do this with the lock held
    SortedVector< wp<IMediaDeathNotifier> > list;
    {
        Mutex::Autolock _l(sServiceLock);
        sMediaPlayerService.clear();
        list = sObitRecipients;
    }

    // Notify application when media server dies.
    // Don't hold the static lock during callback in case app
    // makes a call that needs the lock.
    size_t count = list.size();
    for (size_t iter = 0; iter < count; ++iter) {
        sp<IMediaDeathNotifier> notifier = list[iter].promote();
        if (notifier != 0) {
            notifier->died();
        }
    }
}

IMediaDeathNotifier::DeathNotifier::~DeathNotifier()
{
    LOGV("DeathNotifier::~DeathNotifier");
    Mutex::Autolock _l(sServiceLock);
    sObitRecipients.clear();
    if (sMediaPlayerService != 0) {
        sMediaPlayerService->asBinder()->unlinkToDeath(this);
    }
}

}; // namespace android
