package com.example.example_binder_resource;

import android.os.Parcel;
import android.os.RemoteException;

import java.util.ArrayList;


/**
 * Native implementation of the service manager.  Most clients will only
 * care about getDefault() and possibly asInterface().
 * @hide
 */
public abstract class ServiceManagerNative extends Binder implements IServiceManager {

    /**
     * 以一个BpProxy对象为参数构造一个和业务相关的Proxy对象，例如这里的ServiceManagerProxy
     * ServiceManagerProxy各个业务函数会将相应请求打包后交给BpProxy，最后有BpProxy对象发送给Binder驱动以完成一次通信
     *
     * (实际上BpProxy也不会直接Binder驱动交互，真正和Binder驱动交互的IPCThreadState)
     *
     * Cast a Binder object into a service manager interface, generating a proxy if needed.
     */
    static public IServiceManager asInterface(IBinder obj)
    {
        if (obj == null) {
            return null;
        }
        /** android.os.IServiceManager */
        //获取attachInterface的owner参数
        IServiceManager in = (IServiceManager)obj.queryLocalInterface(descriptor);
        if (in != null) {
            return in;
        }
        return new ServiceManagerProxy(obj);
    }

    public ServiceManagerNative() {
        attachInterface(this, descriptor);
    }


    public boolean onTransact(int code, Parcel data, Parcel reply, int flags) {
        try {
            switch (code) {
                case IServiceManager.GET_SERVICE_TRANSACTION: {
                    data.enforceInterface(IServiceManager.descriptor);
                    String name = data.readString();
                    IBinder service = getService(name);
                    reply.writeStrongBinder(service);
                    return true;
                }

                case IServiceManager.CHECK_SERVICE_TRANSACTION: {
                    data.enforceInterface(IServiceManager.descriptor);
                    String name = data.readString();
                    IBinder service = checkService(name);
                    reply.writeStrongBinder(service);
                    return true;
                }

                case IServiceManager.ADD_SERVICE_TRANSACTION: {
                    data.enforceInterface(IServiceManager.descriptor);
                    String name = data.readString();
                    IBinder service = data.readStrongBinder();
                    boolean allowIsolated = data.readInt() != 0;
                    int dumpPriority = data.readInt();
                    addService(name, service, allowIsolated, dumpPriority);
                    return true;
                }

                case IServiceManager.LIST_SERVICES_TRANSACTION: {
                    data.enforceInterface(IServiceManager.descriptor);
                    int dumpPriority = data.readInt();
                    String[] list = listServices(dumpPriority);
                    reply.writeStringArray(list);
                    return true;
                }

                case IServiceManager.SET_PERMISSION_CONTROLLER_TRANSACTION: {
                    data.enforceInterface(IServiceManager.descriptor);
                    IPermissionController controller = IPermissionController.Stub.asInterface(data.readStrongBinder());
                    setPermissionController(controller);
                    return true;
                }
            }
        } catch (RemoteException e) {
        }
        return false;
    }

    public IBinder asBinder() {
        return this;
    }
}

/**
 * ServiceManager代理类
 */
class ServiceManagerProxy implements IServiceManager {
    public ServiceManagerProxy(IBinder remote) {
        mRemote = remote;
    }

    public IBinder asBinder() {
        return mRemote;
    }

    public IBinder getService(String name) throws RemoteException {
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        data.writeInterfaceToken(IServiceManager.descriptor);
        data.writeString(name);
        mRemote.transact(GET_SERVICE_TRANSACTION, data, reply, 0);
        IBinder binder = reply.readStrongBinder();
        reply.recycle();
        data.recycle();
        return binder;
    }

    public IBinder checkService(String name) throws RemoteException {
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        data.writeInterfaceToken(IServiceManager.descriptor);
        data.writeString(name);
        mRemote.transact(CHECK_SERVICE_TRANSACTION, data, reply, 0);
        IBinder binder = reply.readStrongBinder();
        reply.recycle();
        data.recycle();
        return binder;
    }

    public void addService(String name, IBinder service, boolean allowIsolated, int dumpPriority)
            throws RemoteException {
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        data.writeInterfaceToken(IServiceManager.descriptor);
        data.writeString(name);
        //也是一个native方法
        data.writeStrongBinder(service);
        data.writeInt(allowIsolated ? 1 : 0);
        data.writeInt(dumpPriority);
        //mRemote实际上就是BinderProxy对象，调用它的transact()将请求发送出去
        mRemote.transact(ADD_SERVICE_TRANSACTION, data, reply, 0);
        reply.recycle();
        data.recycle();
    }

    public String[] listServices(int dumpPriority) throws RemoteException {
        ArrayList<String> services = new ArrayList<String>();
        int n = 0;
        while (true) {
            Parcel data = Parcel.obtain();
            Parcel reply = Parcel.obtain();
            data.writeInterfaceToken(IServiceManager.descriptor);
            data.writeInt(n);
            data.writeInt(dumpPriority);
            n++;
            try {
                boolean res = mRemote.transact(LIST_SERVICES_TRANSACTION, data, reply, 0);
                if (!res) {
                    break;
                }
            } catch (RuntimeException e) {
                // The result code that is returned by the C++ code can
                // cause the call to throw an exception back instead of
                // returning a nice result...  so eat it here and go on.
                break;
            }
            services.add(reply.readString());
            reply.recycle();
            data.recycle();
        }
        String[] array = new String[services.size()];
        services.toArray(array);
        return array;
    }

    public void setPermissionController(IPermissionController controller)
            throws RemoteException {
        Parcel data = Parcel.obtain();
        Parcel reply = Parcel.obtain();
        data.writeInterfaceToken(IServiceManager.descriptor);
        data.writeStrongBinder(controller.asBinder());
        mRemote.transact(SET_PERMISSION_CONTROLLER_TRANSACTION, data, reply, 0);
        reply.recycle();
        data.recycle();
    }

    private IBinder mRemote;
}
