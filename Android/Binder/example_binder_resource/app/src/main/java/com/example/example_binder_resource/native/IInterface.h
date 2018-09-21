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

//
#ifndef ANDROID_IINTERFACE_H
#define ANDROID_IINTERFACE_H

#include <binder/Binder.h>

namespace android {

// ----------------------------------------------------------------------

class IInterface : public virtual RefBase
{
public:
            IInterface();
            sp<IBinder>         asBinder();
            sp<const IBinder>   asBinder() const;
            
protected:
    virtual                     ~IInterface();
    virtual IBinder*            onAsBinder() = 0;
};

// ----------------------------------------------------------------------
//仅仅是一个模板函数
template<typename INTERFACE>
inline sp<INTERFACE> interface_cast(const sp<IBinder>& obj)
{
    return INTERFACE::asInterface(obj);
}
//等价于
inline sp<IServiceManager> interface_cast(const sp<IBinder>& obj)
{
    return IServiceManager::asInterface(obj);
}


// ----------------------------------------------------------------------

template<typename INTERFACE>
class BnInterface : public INTERFACE, public BBinder
{
public:
    virtual sp<IInterface>      queryLocalInterface(const String16& _descriptor);
    virtual const String16&     getInterfaceDescriptor() const;

protected:
    virtual IBinder*            onAsBinder();
};

// ----------------------------------------------------------------------

template<typename INTERFACE>
class BpInterface : public INTERFACE, public BpRefBase
{
public:
                                BpInterface(const sp<IBinder>& remote);

protected:
    virtual IBinder*            onAsBinder();
};

// ----------------------------------------------------------------------
//IServiceManager中的宏
#define DECLARE_META_INTERFACE(INTERFACE)                               \
    //定义一个描述字符串
    static const String16 descriptor;                                   \
    //定义一个asInterface
    static sp<I##INTERFACE> asInterface(const sp<IBinder>& obj);        \
    //定义一个getInterfaceDescriptor函数，估计是返回descriptor字符串
    virtual const String16& getInterfaceDescriptor() const;             \
    //定义IServiveManager的构造函数和折构函数
    I##INTERFACE();                                                     \
    virtual ~I##INTERFACE();                                            \


//IServiceManager.cpp中的宏
//参数：INTERFACE为ServiceManager
        //NAME为android.os.IServiceManager
#define IMPLEMENT_META_INTERFACE(INTERFACE, NAME)                       \
    const String16 I##INTERFACE::descriptor(NAME);                      \
    //实现getInterfaceDescriptor函数
    const String16& I##INTERFACE::getInterfaceDescriptor() const {      \
        //返回字符串descriptor，值是"android.os.IServiceManager"
        return I##INTERFACE::descriptor;                                \
    }                                                                   \
    //实现asInterface函数
    sp<I##INTERFACE> I##INTERFACE::asInterface(const sp<IBinder>& obj)  \
    {                                                                   \
        sp<I##INTERFACE> intr;                                          \
        if (obj != NULL) {                                              \
            intr = static_cast<I##INTERFACE*>(                          \
                obj->queryLocalInterface(                               \
                        I##INTERFACE::descriptor).get());               \
            if (intr == NULL) {                                         \
                //obj是刚刚创建的那个BpBinder(0)
                //intr = new BpServiceManager(obj)
                intr = new Bp##INTERFACE(obj);                          \
            }                                                           \
        }                                                               \
        return intr;                                                    \
    }                                                                   \
    //实现构造函数和折构函数
    I##INTERFACE::I##INTERFACE() { }                                    \
    I##INTERFACE::~I##INTERFACE() { }                                   \


#define CHECK_INTERFACE(interface, data, reply)                         \
    if (!data.checkInterface(this)) { return PERMISSION_DENIED; }       \


// ----------------------------------------------------------------------
// No user-serviceable parts after this...

template<typename INTERFACE>
inline sp<IInterface> BnInterface<INTERFACE>::queryLocalInterface(
        const String16& _descriptor)
{
    if (_descriptor == INTERFACE::descriptor) return this;
    return NULL;
}

template<typename INTERFACE>
inline const String16& BnInterface<INTERFACE>::getInterfaceDescriptor() const
{
    return INTERFACE::getInterfaceDescriptor();
}

template<typename INTERFACE>
IBinder* BnInterface<INTERFACE>::onAsBinder()
{
    return this;
}

template<typename INTERFACE>
inline BpInterface<INTERFACE>::BpInterface(const sp<IBinder>& remote)
    //基类构造函数
    : BpRefBase(remote)
{
}

template<typename INTERFACE>
inline IBinder* BpInterface<INTERFACE>::onAsBinder()
{
    return remote();
}
    
// ----------------------------------------------------------------------

}; // namespace android

#endif // ANDROID_IINTERFACE_H
