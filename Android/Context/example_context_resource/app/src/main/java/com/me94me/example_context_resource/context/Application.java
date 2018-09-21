package com.me94me.example_context_resource.context;

import android.content.ComponentCallbacks;
import android.content.ComponentCallbacks2;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.util.Log;
import android.view.autofill.AutofillManager;

import com.me94me.example_context_resource.activitythread.ActivityThread;

import java.util.ArrayList;

import androidx.annotation.CallSuper;

/**
 * 维护全局application状态的基类。
 *
 * 开发者可以通过创建子类提供自己的实现类，并且在AndroidManifest.xml中application标签中以<code>"android:name"</code>提供自己application名。
 *
 * 当APP进程创创建了，Application在任何其他类之前执行。
 *
 * 通常不需要子类实现Application,在大多数情况下，静态单例可以实现相同的作用。
 *
 * 如果单例需要全局context，可以通过{@link android.content.Context#getApplicationContext() Context.getApplicationContext()}
 */
public class Application extends ContextWrapper implements ComponentCallbacks2 {

    private static final String TAG = "Application";

    /** 系统组件回调(更好的内存管理) */
    private ArrayList<ComponentCallbacks> mComponentCallbacks = new ArrayList<ComponentCallbacks>();

    /** Activity回调 */
    private ArrayList<ActivityLifecycleCallbacks> mActivityLifecycleCallbacks = new ArrayList<ActivityLifecycleCallbacks>();

    /** 请求协助的数据监听器回调 */
    private ArrayList<OnProvideAssistDataListener> mAssistCallbacks = null;

    /** 当前加载的Apk文件维护的本地状态 */
    public LoadedApk mLoadedApk;

    /** Activity回调接口 */
    public interface ActivityLifecycleCallbacks {
        void onActivityCreated(Activity activity, Bundle savedInstanceState);
        void onActivityStarted(Activity activity);
        void onActivityResumed(Activity activity);
        void onActivityPaused(Activity activity);
        void onActivityStopped(Activity activity);
        void onActivitySaveInstanceState(Activity activity, Bundle outState);
        void onActivityDestroyed(Activity activity);
    }

    /**
     * {@link Application#registerOnProvideAssistDataListener}
     * {@link Application#unregisterOnProvideAssistDataListener}
     */
    public interface OnProvideAssistDataListener {
        /**
         * 当用户请求帮助时，调用此方法，以构建对当前应用程序的所有上下文的完整{@link Intent＃ACTION_ASSIST} Intent。
         * 可以覆盖此方法，以便将您希望出现在协助Intent的{@link Intent＃EXTRA_ASSIST_CONTEXT}部分中的任何内容放入bundle中。
         * This is called when the user is requesting an assist, to build a full
         * {@link Intent#ACTION_ASSIST} Intent with all of the context of the current
         * application.  You can override this method to place into the bundle anything
         * you would like to appear in the {@link Intent#EXTRA_ASSIST_CONTEXT} part
         * of the assist Intent.
         */
        public void onProvideAssistData(Activity activity, Bundle data);
    }

    public Application() {
        super(null);
    }

    /**
     * 当app启动时调用，在activity，service，receiver对象创建之前
     *
     * 实现类需要尽可能快（例如使用延迟实例化），在这个函数中花的时间影响打开第一个activity的速度
     *
     * 请注意，直接启动也可能会影响Android {@link android.os.Build.VERSION_CODES＃N}及更高版本设备上的回调顺序。
     *
     * 在用户解锁设备之前，只允许直接启动感知组件运行。
     *
     * 应该考虑所有直接启动未识别的组件，包括这样的{@link android.content.ContentProvider}，在用户解锁发生之前被禁用，尤其是当组件回调顺序很重要时。
     */
    @CallSuper
    public void onCreate() {
    }

    /**
     * 此方法用于模拟过程环境。
     * 永远不会在生产Android设备上调用它，只需杀死它们即可删除进程; 这样做时不会执行任何用户代码（包括此回调）。
     */
    @CallSuper
    public void onTerminate() {
    }

    /** 执行配置改变的回调 */
    @CallSuper
    public void onConfigurationChanged(Configuration newConfig) {
        Object[] callbacks = collectComponentCallbacks();
        if (callbacks != null) {
            for (int i=0; i<callbacks.length; i++) {
                ((ComponentCallbacks)callbacks[i]).onConfigurationChanged(newConfig);
            }
        }
    }

    @CallSuper
    public void onLowMemory() {
        Object[] callbacks = collectComponentCallbacks();
        if (callbacks != null) {
            for (int i=0; i<callbacks.length; i++) {
                ((ComponentCallbacks)callbacks[i]).onLowMemory();
            }
        }
    }

    @CallSuper
    public void onTrimMemory(int level) {
        Object[] callbacks = collectComponentCallbacks();
        if (callbacks != null) {
            for (int i=0; i<callbacks.length; i++) {
                Object c = callbacks[i];
                if (c instanceof ComponentCallbacks2) {
                    ((ComponentCallbacks2)c).onTrimMemory(level);
                }
            }
        }
    }

    /** 注册组件的回调 */
    public void registerComponentCallbacks(ComponentCallbacks callback) {
        synchronized (mComponentCallbacks) {
            mComponentCallbacks.add(callback);
        }
    }
    public void unregisterComponentCallbacks(ComponentCallbacks callback) {
        synchronized (mComponentCallbacks) {
            mComponentCallbacks.remove(callback);
        }
    }

    /** 注册Activity的生命周期回调 */
    public void registerActivityLifecycleCallbacks(ActivityLifecycleCallbacks callback) {
        synchronized (mActivityLifecycleCallbacks) {
            mActivityLifecycleCallbacks.add(callback);
        }
    }
    public void unregisterActivityLifecycleCallbacks(ActivityLifecycleCallbacks callback) {
        synchronized (mActivityLifecycleCallbacks) {
            mActivityLifecycleCallbacks.remove(callback);
        }
    }

    /** 助手的协助数据回调 */
    public void registerOnProvideAssistDataListener(OnProvideAssistDataListener callback) {
        synchronized (this) {
            if (mAssistCallbacks == null) {
                mAssistCallbacks = new ArrayList<OnProvideAssistDataListener>();
            }
            mAssistCallbacks.add(callback);
        }
    }
    public void unregisterOnProvideAssistDataListener(OnProvideAssistDataListener callback) {
        synchronized (this) {
            if (mAssistCallbacks != null) {
                mAssistCallbacks.remove(callback);
            }
        }
    }


    /**
     * 返回当前进程名
     * 一个package的默认进程名和packageName相同
     *
     * 非默认的进程"$PACKAGE_NAME:$NAME"，$Name响应在AndroidManifest.xml的android:process属性
     */
    public static String getProcessName() {
        return ActivityThread.currentProcessName();
    }

    // ------------------ Internal API ------------------

    /**
     * @hide
     */
    /* package */ final void attach(Context context) {
        attachBaseContext(context);
        mLoadedApk = ContextImpl.getImpl(context).mPackageInfo;
    }

    /** 分发activity生命周期 */
    /* package */ void dispatchActivityCreated(Activity activity, Bundle savedInstanceState) {
        Object[] callbacks = collectActivityLifecycleCallbacks();
        if (callbacks != null) {
            for (int i=0; i<callbacks.length; i++) {
                ((ActivityLifecycleCallbacks)callbacks[i]).onActivityCreated(activity,
                        savedInstanceState);
            }
        }
    }

    /* package */ void dispatchActivityStarted(Activity activity) {
        Object[] callbacks = collectActivityLifecycleCallbacks();
        if (callbacks != null) {
            for (int i=0; i<callbacks.length; i++) {
                ((ActivityLifecycleCallbacks)callbacks[i]).onActivityStarted(activity);
            }
        }
    }

    /* package */ void dispatchActivityResumed(Activity activity) {
        Object[] callbacks = collectActivityLifecycleCallbacks();
        if (callbacks != null) {
            for (int i=0; i<callbacks.length; i++) {
                ((ActivityLifecycleCallbacks)callbacks[i]).onActivityResumed(activity);
            }
        }
    }

    /* package */ void dispatchActivityPaused(Activity activity) {
        Object[] callbacks = collectActivityLifecycleCallbacks();
        if (callbacks != null) {
            for (int i=0; i<callbacks.length; i++) {
                ((ActivityLifecycleCallbacks)callbacks[i]).onActivityPaused(activity);
            }
        }
    }

    /* package */ void dispatchActivityStopped(Activity activity) {
        Object[] callbacks = collectActivityLifecycleCallbacks();
        if (callbacks != null) {
            for (int i=0; i<callbacks.length; i++) {
                ((ActivityLifecycleCallbacks)callbacks[i]).onActivityStopped(activity);
            }
        }
    }

    /* package */ void dispatchActivitySaveInstanceState(Activity activity, Bundle outState) {
        Object[] callbacks = collectActivityLifecycleCallbacks();
        if (callbacks != null) {
            for (int i=0; i<callbacks.length; i++) {
                ((ActivityLifecycleCallbacks)callbacks[i]).onActivitySaveInstanceState(activity,
                        outState);
            }
        }
    }

    /* package */ void dispatchActivityDestroyed(Activity activity) {
        Object[] callbacks = collectActivityLifecycleCallbacks();
        if (callbacks != null) {
            for (int i=0; i<callbacks.length; i++) {
                ((ActivityLifecycleCallbacks)callbacks[i]).onActivityDestroyed(activity);
            }
        }
    }

    /** 收集系统组件回调 */
    private Object[] collectComponentCallbacks() {
        Object[] callbacks = null;
        synchronized (mComponentCallbacks) {
            if (mComponentCallbacks.size() > 0) {
                callbacks = mComponentCallbacks.toArray();
            }
        }
        return callbacks;
    }

    /** 收集activity生命周期回调 */
    private Object[] collectActivityLifecycleCallbacks() {
        Object[] callbacks = null;
        synchronized (mActivityLifecycleCallbacks) {
            if (mActivityLifecycleCallbacks.size() > 0) {
                callbacks = mActivityLifecycleCallbacks.toArray();
            }
        }
        return callbacks;
    }

    /** 分发助手协助数据 */
    /* package */ void dispatchOnProvideAssistData(Activity activity, Bundle data) {
        Object[] callbacks;
        synchronized (this) {
            if (mAssistCallbacks == null) {
                return;
            }
            callbacks = mAssistCallbacks.toArray();
        }
        if (callbacks != null) {
            for (int i=0; i<callbacks.length; i++) {
                ((OnProvideAssistDataListener)callbacks[i]).onProvideAssistData(activity, data);
            }
        }
    }

    /** @hide */
    @Override
    public AutofillManager.AutofillClient getAutofillClient() {
        final AutofillManager.AutofillClient client = super.getAutofillClient();
        if (client != null) {
            return client;
        }
        if (android.view.autofill.Helper.sVerbose) {
            Log.v(TAG, "getAutofillClient(): null on super, trying to find activity thread");
        }
        // Okay, ppl use the application context when they should not. This breaks
        // autofill among other things. We pick the focused activity since autofill
        // interacts only with the currently focused activity and we need the fill
        // client only if a call comes from the focused activity. Sigh...
        final ActivityThread activityThread = ActivityThread.currentActivityThread();
        if (activityThread == null) {
            return null;
        }
        final int activityCount = activityThread.mActivities.size();
        for (int i = 0; i < activityCount; i++) {
            final ActivityThread.ActivityClientRecord record =
                    activityThread.mActivities.valueAt(i);
            if (record == null) {
                continue;
            }
            final Activity activity = record.activity;
            if (activity == null) {
                continue;
            }
            if (activity.getWindow().getDecorView().hasFocus()) {
                if (android.view.autofill.Helper.sVerbose) {
                    Log.v(TAG, "getAutofillClient(): found activity for " + this + ": " + activity);
                }
                return activity;
            }
        }
        if (android.view.autofill.Helper.sVerbose) {
            Log.v(TAG, "getAutofillClient(): none of the " + activityCount + " activities on "
                    + this + " have focus");
        }
        return null;
    }
}
