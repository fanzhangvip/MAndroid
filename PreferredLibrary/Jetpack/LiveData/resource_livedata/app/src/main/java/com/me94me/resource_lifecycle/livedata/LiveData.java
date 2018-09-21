package com.me94me.resource_lifecycle.livedata;

import com.me94me.resource_lifecycle.executor.ArchTaskExecutor;
import com.me94me.resource_lifecycle.lifecycle.GenericLifecycleObserver;
import com.me94me.resource_lifecycle.lifecycle.Lifecycle;
import com.me94me.resource_lifecycle.lifecycle.LifecycleOwner;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.Observer;

import java.util.Iterator;
import java.util.Map;

import static com.me94me.resource_lifecycle.lifecycle.Lifecycle.State.DESTROYED;
import static com.me94me.resource_lifecycle.lifecycle.Lifecycle.State.STARTED;


public abstract class LiveData<T> {

    //数据锁
    final Object mDataLock = new Object();

    static final int START_VERSION = -1;

    static final Object NOT_SET = new Object();

    //Google工程师编写
    private SafeIterableMap<Observer<? super T>, ObserverWrapper> mObservers = new SafeIterableMap<>();

    //处于active状态的observers
    int mActiveCount = 0;

    //该LiveData的数据
    private volatile Object mData = NOT_SET;

    // 当调用setData时，我们设置等待数据，并在主线程上进行实际数据交换
    volatile Object mPendingData = NOT_SET;

    //记录setValue()调用的次数(从0开始计算)
    private int mVersion = START_VERSION;

    //是否正在改变数据
    private boolean mDispatchingValue;

    //分发的数据是否已经更新了
    private boolean mDispatchInvalidated;


    /**
     * {@link #postValue(Object)}中调用，从后台线程发往主线程执行
     */
    private final Runnable mPostValueRunnable = new Runnable() {
        @Override
        public void run() {
            //将mPendingData设置为newValue并将mPendingData设为未设置
            Object newValue;
            synchronized (mDataLock) {
                newValue = mPendingData;
                //只有当mPendingData设置为NOT_SET才会处理下一次事件
                mPendingData = NOT_SET;
            }
            //无需检查
            setValue((T) newValue);
        }
    };

    /**
     * 考虑通知观察者
     */
    private void considerNotify(ObserverWrapper observer) {
        //如果处于非激活状态返回
        if (!observer.mActive) {
            return;
        }
        //检查最新状态
        if (!observer.shouldBeActive()) {
            observer.activeStateChanged(false);
            return;
        }
        if (observer.mLastVersion >= mVersion) {
            return;
        }
        observer.mLastVersion = mVersion;
        //执行数据更新事件
        observer.mObserver.onChanged((T) mData);
    }

    /**
     * 分发事件
     * @param initiator
     */
    void dispatchingValue(@Nullable ObserverWrapper initiator) {

        if (mDispatchingValue) {
            mDispatchInvalidated = true;
            return;
        }
        //是否正在分发value
        mDispatchingValue = true;
        do {
            mDispatchInvalidated = false;
            //分发单个事件
            if (initiator != null) {
                considerNotify(initiator);
                initiator = null;
                //分发所有观察者
            } else {
                for (Iterator<Map.Entry<Observer<? super T>, ObserverWrapper>> iterator =
                        mObservers.iteratorWithAdditions(); iterator.hasNext(); ) {
                    considerNotify(iterator.next().getValue());
                    if (mDispatchInvalidated) {
                        break;
                    }
                }
            }
        } while (mDispatchInvalidated);
        mDispatchingValue = false;
    }


    @MainThread
    public void observe(@NonNull LifecycleOwner owner, @NonNull Observer<? super T> observer) {
        //确保在主线程
        assertMainThread("observe");
        //如果生命周期状态在DESTROYED返回
        if (owner.getLifecycle().getCurrentState() == DESTROYED) {
            return;
        }
        //新建一个拥有生命周期持有者和观察者的observer
        LifecycleBoundObserver wrapper = new LifecycleBoundObserver(owner, observer);
        //如果已经存在抛出异常
        ObserverWrapper existing = mObservers.putIfAbsent(observer, wrapper);
        if (existing != null && !existing.isAttachedTo(owner)) {
            throw new IllegalArgumentException("Cannot add the same observer"
                    + " with different lifecycles");
        }
        if (existing != null) {
            return;
        }
        owner.getLifecycle().addObserver(wrapper);
    }

    /**
     * 添加一个给定的observer到observers list
     *
     * 该调用类似与{@link LiveData#observe(LifecycleOwner, Observer)}，当生命周期持有者总是处于active状态时。
     *
     * 这意味着给定的观察者将会接受到所有事件并且不会自动移除，你应该手动调用{@link #removeObserver(Observer)}来停止对liveData的观察
     *
     * 虽然LiveData有一个这样的观察者，但它将被视为活跃的。
     * 如果observer已经添加了一个owner给liveData，liveData将会抛出一个异常{@link IllegalArgumentException}
     */
    @MainThread
    public void observeForever(@NonNull Observer<? super T> observer) {
        //确保在主线程
        assertMainThread("observeForever");
        //新建一个总处于active状态的观察者
        AlwaysActiveObserver wrapper = new AlwaysActiveObserver(observer);
        ObserverWrapper existing = mObservers.putIfAbsent(observer, wrapper);
        //已经关联了
        if (existing != null && existing instanceof LiveData.LifecycleBoundObserver) {
            throw new IllegalArgumentException("Cannot add the same observer"
                    + " with different lifecycles");
        }
        if (existing != null) {
            return;
        }
        wrapper.activeStateChanged(true);
    }

    /**
     * 从Observers list中移除给定的observer
     */
    @MainThread
    public void removeObserver(@NonNull final Observer<? super T> observer) {
        //确保在主线程中执行
        assertMainThread("removeObserver");
        //返回的是移除的ObserverWrapper
        ObserverWrapper removed = mObservers.remove(observer);
        if (removed == null) {
            return;
        }
        //解除关联，处理非active状态事件
        removed.detachObserver();
        removed.activeStateChanged(false);
    }

    /**
     * 移除{@link LifecycleOwner}的所有观察者
     */
    @MainThread
    public void removeObservers(@NonNull final LifecycleOwner owner) {
        //确保在主线程中执行
        assertMainThread("removeObservers");
        //循环移除
        for (Map.Entry<Observer<? super T>, ObserverWrapper> entry : mObservers) {
            if (entry.getValue().isAttachedTo(owner)) {
                removeObserver(entry.getKey());
            }
        }
    }


    /**
     * 发送一个任何到主线程去执行设置给定的value
     *
     * <pre class="prettyprint">
     * liveData.postValue("a");
     * liveData.setValue("b");
     * </pre>
     * "b"会先设置,"a"后设置
     *
     * 如果在主线执行已发送的任务之前，你调用都多次这个方法，仅处理分发最新的值
     *
     * @param value The new value
     */
    protected void postValue(T value) {
        //是否发送task
        boolean postTask;
        synchronized (mDataLock) {
            //如果mPendingData未设置data则发送task
            postTask = mPendingData == NOT_SET;
            mPendingData = value;
        }
        //不发送则返回
        if (!postTask) {
            return;
        }
        ArchTaskExecutor.getInstance().postToMainThread(mPostValueRunnable);
    }

    /**
     * 设置当前value
     * 若拥有处于active的观察者，该value会分发给他们
     *
     * 该方法必须在主线程调用，若需要从后台线程中调用使用{@link #postValue(Object)}
     *
     * @param value The new value
     */
    @MainThread
    protected void setValue(T value) {
        //检测是不是在主线程
        assertMainThread("setValue");
        //每设置一次版本号加1
        mVersion++;
        mData = value;
        //分发数据改变事件给所有观察者
        dispatchingValue(null);
    }

    /**
     * 返回当前的Value
     * 注意：当在后台线程中调用时，可能不会获取到最新的value
     */
    @Nullable
    public T getValue() {
        Object data = mData;
        if (data != NOT_SET) {
            return (T) data;
        }
        return null;
    }

    /**
     *
     */
    int getVersion() {
        return mVersion;
    }


    /**
     * 在{@link ObserverWrapper#activeStateChanged(boolean)}调用
     * 当处于active状态的观察者的数量从0变为1时调用。
     */
    protected void onActive() {

    }

    /**
     * 在{@link ObserverWrapper#activeStateChanged(boolean)}调用
     * 当处于active状态的观察者的数量从1变为0时调用。
     */
    protected void onInactive() {

    }

    /**
     * 在{@link ObserverWrapper#activeStateChanged(boolean)}调用
     * 是否拥有Observer
     */
    public boolean hasObservers() {
        return mObservers.size() > 0;
    }

    /**
     * 在{@link ObserverWrapper#activeStateChanged(boolean)}调用
     * 是否拥有处于活动状态的Observer
     */
    public boolean hasActiveObservers() {
        return mActiveCount > 0;
    }



    /**
     * 拥有生命周期持有者和观察者的类
     */
    class LifecycleBoundObserver extends ObserverWrapper implements GenericLifecycleObserver {
        @NonNull
        final LifecycleOwner mOwner;

        LifecycleBoundObserver(@NonNull LifecycleOwner owner, Observer<? super T> observer) {
            super(observer);
            mOwner = owner;
        }

        @Override
        boolean shouldBeActive() {
            return mOwner.getLifecycle().getCurrentState().isAtLeast(STARTED);
        }

        @Override
        public void onStateChanged(LifecycleOwner source, Lifecycle.Event event) {
            //当前状态为Destroyed移除观察者
            if (mOwner.getLifecycle().getCurrentState() == DESTROYED) {
                removeObserver(mObserver);
                return;
            }
            //状态变化执行事件
            activeStateChanged(shouldBeActive());
        }
        //是否关联lifeCycle和observer
        @Override
        boolean isAttachedTo(LifecycleOwner owner) {
            return mOwner == owner;
        }
        //解除关联后移除观察者
        @Override
        void detachObserver() {
            mOwner.getLifecycle().removeObserver(this);
        }
    }


    /**
     * 观察者包装类
     */
    private abstract class ObserverWrapper {

        //观察者
        final Observer<? super T> mObserver;

        //是否处于Active状态
        boolean mActive;

        //最新版本
        int mLastVersion = START_VERSION;

        //构造方法
        ObserverWrapper(Observer<? super T> observer) {
            mObserver = observer;
        }

        //是否处于active状态
        abstract boolean shouldBeActive();

        //LifecycleOwner与Observer是否关联
        boolean isAttachedTo(LifecycleOwner owner) {
            return false;
        }

        //LifecycleOwner与Observer detach
        void detachObserver() {
        }

        //active的状态改变了
        void activeStateChanged(boolean newActive) {
            if (newActive == mActive) {
                return;
            }
            // 立即设置active的状态，不会分发事件给非active的observer
            mActive = newActive;
            //之前是否非Active
            boolean wasInactive = LiveData.this.mActiveCount == 0;
            //计算active状态的数量
            LiveData.this.mActiveCount += mActive ? 1 : -1;
            //执行onActive()
            //条件：1、之前处于非active状态2、现在处于active
            if (wasInactive && mActive) {
                onActive();
            }
            //执行onInactive()
            //条件：1、之前active的数量不为0   2、现在处于非active状态
            if (LiveData.this.mActiveCount == 0 && !mActive) {
                onInactive();
            }
            //如果处于active状态，分发事件给当前观察者
            if (mActive) {
                dispatchingValue(this);
            }
        }
    }

    /**
     * 始终会更新的观察者
     */
    private class AlwaysActiveObserver extends ObserverWrapper {

        AlwaysActiveObserver(Observer<? super T> observer) {
            super(observer);
        }

        @Override
        boolean shouldBeActive() {
            return true;
        }
    }

    /**
     * 判断是不是在主线程
     * @param methodName 方法名
     */
    private static void assertMainThread(String methodName) {
        if (!ArchTaskExecutor.getInstance().isMainThread()) {
            throw new IllegalStateException("Cannot invoke " + methodName + " on a background"
                    + " thread");
        }
    }
}
