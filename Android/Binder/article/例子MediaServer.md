# 庖丁解MediaServer

之所以选择MediaServer作为切入点，是因为这个Server是系统诸多重要Service的栖息地，它们包括：

* AudioFinger：音频系统中的核心服务。
* AudioPolicyService:音频系统中关于音频策略的重要服务。
* MeidaPlayerService：多媒体系统中的重要服务。
* CameraService：有关摄像/照相的重要服务。

## MediaServer的入口函数

MS是一个可执行程序，入口函数是main

```Java
int main(int argc,char** argv){
    //获得一个ProcessState实例。
    sp<ProcessState> proc(ProcesState::self());
    //MediaServer作为ServiceManager的客户端，需要想ServiceManager注册服务。
    //调用defaultServiceManager,得到一个IServiceManager。
    sp<IServiceManager> sm = defaultServiceManager();
    //初始化音频系统的AudioFlinger服务
    AudioFlinger::instantiate();
    //多媒体系统的MediaPlayer服务，我们将以它作为主切入点。
    MediaPlayerService::instantiate();
    //CameraService服务
    CameraService::instantiate();
    //音频系统的AudioPolicy服务。
    AudioPolicyService::instantiate();
    //根据名称来推断，难道是要创建一个线程池吗？
    ProcessState::self()-<startThreadPool();
    //下面的操作是要将自己加入到刚才的线程池中吗？
    IPCThraedState::self()->joinThreadPool();
}
```
## 独一无二的ProcessState

我们在main函数的开始处碰见了ProcessState。由于每个进程只有一个ProcessState,所以它是独一无二。

```Java
//获得一个ProcessState实例。
sp<ProcessState> proc(ProcesState::self());
```
​	1.单例的ProcessState

```Java
sp<ProcessState> ProcessState::self(){
	//程序刚开始执行，gProcess一定为null
    if(gProcess != null){
        return gProcess;
    }
    AutoMutex _l(gProcessMutex);
    //创建一个ProcessState对象，并赋值给gProcess
    if(gProcess == NULL){
        gProcess = new ProcessState;
    }
    return gProcess;
}
```

self函数采用了单例模式，根据这个以及Process State的名字这很明确地告诉了我们一个信息：每个进程只有一个ProcessState对象。

​	2.ProcessState的构造

再来看看ProcessState的构造函数。

```Java
ProcessState::ProcessState()
	//Android中有很多代码都是这么写的，稍不留神就容易忽略这里调用了一个很重要的函数。
	:mDricerFD(open_driver())
	,mVMStart(MAP_FAILED)//映射内存的起始地址。
	,mManagesContexts(false)
	,mBinderContextCheckFunc(NULL)
	,mBinderContextUserData(NULL)
	,mThreadPoolStarted(false)
	,mThreadPollSeq(l)
        /**
        BINDER_VM_SIZE定义为（1*1024*1024）-（4096*2）=1M-8k
        mmap的用法希望读者man一下，不过这个函数真正的实现和驱动有关系，而Binder驱动会分配一块内幕才能来接受数据。
        */
        if(mDricerFD >= 0){
            mVMStart = mmap(0,BINDER_VM_SIZE,PROT_READ,MAP_PRIVATE|MAP_NORESERVE,MdRICERFD,0);
        }
		......
```

​	3.打开binder设备

open_driver的作用就是打开/dev/binder这个设备，它是Android在内核中为完成进程间通信而专门设置的一个虚拟设备

```Java
static int open_driver(){
    //打开/dev/binder设备
    int fd = open("/dev/binder",O_RDWR);
    if(fd>=0){
        ......
        size_t maxThreads = 15;
        //通过ioctl方式告诉binder驱动，这个fd支持的最大线程数是15个。
        result = ioctl(fd,BINDER_SET_MAX_THREADS,&maxThreads);
    }
    return fd;
    ......
}
```
至此，Process:self函数就分析完了。它到底干了什么呢？

* 打开/dev/binder设备，这就相当于与内核的Binder驱动有了交互的通道。
* 对返回的fd使用mmap，这样Binder驱动就会分配一块内存来接受数据。
* 由于ProcessState具有唯一性，因此一个进程只打开设备一次。

分析完ProcessState，接下来将要分析第二个关键函数defaulfServiceManager。

## 时空穿越模式——defaultServiceManager

defaultServiceManager函数的实现在IServiceManager.cpp中完成。它会返回一个IServiceManager对象，通过这个对象，我们可以神奇地与另一个进程ServiceManager进行交互。

​	1.魔术前的准备工作

先来看看defaultServiceManager都调用了哪些函数。返回的这个IServiceManager到底又是什么？

```Java
sp<IServiceManager> defaultServiceManager(){
    //看样子又是一个单例，英文名叫Single,Andorid是一个优秀的源码库，大量使用了设计模式
    //建议读者以此为契机学习设计模式，首推《设计模式：可复用面向对象软件的基础》。
    if(gDefaultServiceManager != NULL)return gDefaultServiceManager;
    {
        AutoMutex _l(gDefaultServiceManagerLock);
        if(gDefaultServiceManager == NULL){
            gDefaultServiceManager = interface_cast<IServiceManager>(
            	ProcessState::self() -> getontextObject(NULL)
            );
        }
    }
    return gDefaultServiceManager;
}
```

调用了ProcessState的getContextObject函数！

注意：传给它的参数是NULL，即0

```Java
sp<IBinder> ProcessState:getContextObject(const sp<IBinder>& caller){
	/**
	* caller的值为0！注意，该函数返回的是IBinder。它是什么？后面说。
	supportsProcess函数根据openDriver函数是否可成功打开设备来判断它是否支持process。
	真实设备肯定支持proces。
	*/
    if(supportsProcesses()){
    //真实设备上肯定是支持进程的，所以会调用下面这个函数。
        return getStrongProxyForHandle(0);
    }else{
        return getContextObject(String16("default"),caller);
    }
}
```

getStrongProxyForHandle这个函数名怪怪的，可能会让人感到些许困惑。请注意它的调用参数名叫handle，在windows编程中经常使用这个名称，它是对资源的一种标识。说白了其实就是有一个资源项，保存在一个资源数组（也可以是别的组织结构）中，handle的值正是该资源项在数组中的索引。

```Java
sp<IBinder> ProcessState::getStrongProxyForHandle(int32_t handle){
    sp<IBinder> result;
    AutoMutex _l(mLock);
    /**
    * 根据索引查找对应的资源。如果lookupHandleLocked发现没有对应的资源项，则会创建一个新的项并返回。
    这个新项的内容需要填充。
    */
    handle_entry* e = lookupHandleLocked(handle);
    if(e != NULL){
        IBindler* b = e->binder;
        if(b == NULL || ！e->refs->attemptIncWeak(this)){
            //对于新创建的资源项，它的binder为空，所以走这个分支。注意，handle的值为0.
            b = new BpBinder(handle);//创建一个BpBinder.
            e -> binder = b;//填充entry的内容。
            if(b) e->refs = b->getWeakRefs();
            result = b;
        }else{
            result.force_set(b);
            e->refs->decWeak(this);
        }
    }
    return result;//返回BpBindler(handle),注意，handle的值为0.
}
```

​	2.魔术表演的道具——BpBinder

这个穿越魔术的道具就是BpBinder。

BpBinder和BBinder都是Android中与Binder通信的相关的代表，它们都是从IBinder类中派生而来

* BpBindler是客户端用来与Server交互的代理类，p即Proxy的意思
* BBinder则是与proxy相对的一端，它是proxy交互的目的端。如果说Proxy代表客户端，那么BBinder则代表服务器。这里的BpBinder和BBinder是一一对应的。即某个BpBinder只能和对应的BBinder交互。我们当然不希望通过BpBinderA发送的请求，却由BBinderB来处理。

刚才我们在defaultServiceManager()函数中创建了BpBinder，这里有两个问题

* 为什么创建的不是BBinder？因为我们是ServiceManager的客户端，当然得使用代理端来与ServiceManager进行交互。
* 前面说了，BpBinder和BBinder是一一对应的，那么BpBinder如何标识它所对应的BBinder端呢？答案是Binder系统通过handler来标识对应的BBinder。以后我们会确认这个Handle值的作用。

> 我们给BpBinder构造函数传的参数handle的值是0。这个0在整个Bindler系统中有重要含义——因为0代表的就是ServiceManager所对应的BBinder。

BpBinder是如此重要，必须对它进行分析，

```Java
BpBinder::BpBinder(int32_t handle)
	:mHandle(handle)//handle是0
	,mAlive(1)
	,mObitsSent(0)
	,mObituaries(NULL){
        extendObjectLifetime(OBJECT_LIFETIME_WEAK);
        //另一个重要对象是IPCThreadState,我们稍后会详细讲解
        IPCThread::self()->incWeakHandle(handle);
	}
```

BpBinder、BBinder这两个类没有任何地方操作ProcessState打开的那个/dev/binder设备，换言之，这两个Binder类没有和binder设备直接交互。

回顾一下道具出场的过程

```Java
gDefaulfServiceManger = interface_cast<IServiceManager>(ProcessState::self()->getContextObject(NULL));
```

现在这个函数调用将变成如下

```Java
gDefaulfServiceManger = interface_cast<IServiceManager>(new BpBinder(0));
```

这里出现一个interface_cast。它是什么？其实是一个障眼法！

​	3.障眼法——interface_cast

interface_cast、dynamic_cast和static_cast看起来是否非常眼熟？它们是指针类型转换的意思吗？如果是，那又是如何将BpBinder*类型强制转化成IServiceMangaer*类型的呢？BpBinder的家谱刚才也看了，它的“爸爸的爸爸的爸爸”这条线上没有任何一个与IServiceManager有任何关系。

这里看看interface_cast的具体实现

```Java
template<typename INTERFACE>
inline sp<INTERFACE> interface_cast(const sp<IBinder>& obj){
    return INTERFACE::asInterface(obj);
}
```

仅仅是一个模板函数，所以interface_cast<IServiceManager>等价于
```Java
inline sp<INTERFACE> interface_cast(const sp<IBinder>& obj){
    return IServiceManager::asInterface(obj);
}
```
又转移到IServiceManager对象中去了，这难道不是障眼法吗？

​	4.拨开浮云见月明——IServiceManager

刚才提到，IBinder家族的BpBinder和BBinder是与通信业务相关的，那么业务层的逻辑又是如何巧妙地架构在Binder机制上的呢？关于这些问题，可以用一个绝好的例子来解释它，它就是IServiceManager。

（1）定义业务逻辑

先回答第一个问题：如何表述应用的业务层逻辑。可以先分析一下IServiceManager是怎么做的。IServiceManager定义了ServiceManager所提供的服务，看它的定义可知，其中有很多有趣的内容。IServiceManager定义在IServiceManager.h中

```Java
class IServiceManager:public IInterface{
	public:
	//关键无比的宏
	DECLARE_META_INTERFACE(ServiceManager);
	//下面是ServiceManager所提供的业务函数
	virtual sp<IBinder> getService(const String16& name) const = 0;
	virtual sp<IBinder> checkService(const String16& name) const = 0;
	virtual status_t  addService(const String16& name,const sp<IBinder>& service) = 0;
	virtual Vector<String16> listService() = 0;
};
```
