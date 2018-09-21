package com.me94me.example_context_resource.activitythread;

import android.content.Context;
import android.os.IBinder;
import android.os.UserManager;


/**
 * 运行在系统进程的服务的基类。
 * 如果需要，重写和实现生命周期回调方法
 *
 * 调用构造方法可以获得 the system {@link Context} 来初始化系统service
 *
 * <li>{@link #onStart()} is called to get the service running.  The service should
 * publish its binder interface at this point using
 * {@link #publishBinderService(String, IBinder)}.  It may also publish additional
 * local interfaces that other services within the system server may use to access
 * privileged internal functions.
 * <li>Then {@link #onBootPhase(int)} is called as many times as there are boot phases
 * until {@link #PHASE_BOOT_COMPLETED} is sent, which is the last boot phase. Each phase
 * is an opportunity to do special work, like acquiring optional service dependencies,
 * waiting to see if SafeMode is enabled, or registering with a service that gets
 * started after this one.
 * </ul><p>
 * NOTE: All lifecycle methods are called from the system server's main looper thread.
 * </p>
 *
 * {@hide}
 */
public abstract class SystemService {
    /*
     * Boot Phases
     */
    public static final int PHASE_WAIT_FOR_DEFAULT_DISPLAY = 100; // maybe should be a dependency?

    /**
     * After receiving this boot phase, services can obtain lock settings data.
     */
    public static final int PHASE_LOCK_SETTINGS_READY = 480;

    /**
     * After receiving this boot phase, services can safely call into core system services
     * such as the PowerManager or PackageManager.
     */
    public static final int PHASE_SYSTEM_SERVICES_READY = 500;

    /**
     * After receiving this boot phase, services can safely call into device specific services.
     */
    public static final int PHASE_DEVICE_SPECIFIC_SERVICES_READY = 520;

    /**
     * After receiving this boot phase, services can broadcast Intents.
     */
    public static final int PHASE_ACTIVITY_MANAGER_READY = 550;

    /**
     * After receiving this boot phase, services can start/bind to third party apps.
     * Apps will be able to make Binder calls into services at this point.
     */
    public static final int PHASE_THIRD_PARTY_APPS_CAN_START = 600;

    /**
     * After receiving this boot phase, services can allow user interaction with the device.
     * This phase occurs when boot has completed and the home application has started.
     * System services may prefer to listen to this phase rather than registering a
     * broadcast receiver for ACTION_BOOT_COMPLETED to reduce overall latency.
     */
    public static final int PHASE_BOOT_COMPLETED = 1000;

    private final Context mContext;


    /**
     * 初始化系统服务
     *
     * @param context The system server context.
     */
    public SystemService(Context context) {
        mContext = context;
    }

    /**
     * Gets the system context.
     */
    public final Context getContext() {
        return mContext;
    }

    /**
     * 获取system UI context。
     * 该context用于显示UI
     * It is themable,
     * which means resources can be overridden at runtime. Do not use to retrieve properties that
     * configure the behavior of the device that is not UX related.
     */
    public final Context getUiContext() {
        // This has already been set up by the time any SystemServices are created.
        return ActivityThread.currentActivityThread().getSystemUiContext();
    }

    /**
     * Returns true if the system is running in safe mode.
     * TODO: we should define in which phase this becomes valid
     */
    public final boolean isSafeMode() {
        return getManager().isSafeMode();
    }

    /**
     * Called when the dependencies listed in the @Service class-annotation are available
     * and after the chosen start phase.
     * When this method returns, the service should be published.
     */
    public abstract void onStart();

    /**
     * Called on each phase of the boot process. Phases before the service's start phase
     * (as defined in the @Service annotation) are never received.
     *
     * @param phase The current boot phase.
     */
    public void onBootPhase(int phase) {}

    /**
     * Called when a new user is starting, for system services to initialize any per-user
     * state they maintain for running users.
     * @param userHandle The identifier of the user.
     */
    public void onStartUser(int userHandle) {}

    /**
     * Called when an existing user is in the process of being unlocked. This
     * means the credential-encrypted storage for that user is now available,
     * and encryption-aware component filtering is no longer in effect.
     * <p>
     * While dispatching this event to services, the user is in the
     * {@code STATE_RUNNING_UNLOCKING} state, and once dispatching is finished
     * the user will transition into the {@code STATE_RUNNING_UNLOCKED} state.
     * Code written inside system services should use
     * {@link UserManager#isUserUnlockingOrUnlocked(int)} to handle both of
     * these states.
     *
     * @param userHandle The identifier of the user.
     */
    public void onUnlockUser(int userHandle) {}

    /**
     * Called when switching to a different foreground user, for system services that have
     * special behavior for whichever user is currently in the foreground.  This is called
     * before any application processes are aware of the new user.
     * @param userHandle The identifier of the user.
     */
    public void onSwitchUser(int userHandle) {}

    /**
     * Called when an existing user is stopping, for system services to finalize any per-user
     * state they maintain for running users.  This is called prior to sending the SHUTDOWN
     * broadcast to the user; it is a good place to stop making use of any resources of that
     * user (such as binding to a service running in the user).
     *
     * <p>NOTE: This is the last callback where the callee may access the target user's CE storage.
     *
     * @param userHandle The identifier of the user.
     */
    public void onStopUser(int userHandle) {}

    /**
     * Called when an existing user is stopping, for system services to finalize any per-user
     * state they maintain for running users.  This is called after all application process
     * teardown of the user is complete.
     *
     * <p>NOTE: When this callback is called, the CE storage for the target user may not be
     * accessible already.  Use {@link #onStopUser} instead if you need to access the CE storage.
     *
     * @param userHandle The identifier of the user.
     */
    public void onCleanupUser(int userHandle) {}

    /**
     * Publish the service so it is accessible to other services and apps.
     *
     * @param name the name of the new service
     * @param service the service object
     */
    protected final void publishBinderService(String name, IBinder service) {
        publishBinderService(name, service, false);
    }

    /**
     * Publish the service so it is accessible to other services and apps.
     *
     * @param name the name of the new service
     * @param service the service object
     * @param allowIsolated set to true to allow isolated sandboxed processes
     * to access this service
     */
    protected final void publishBinderService(String name, IBinder service,
            boolean allowIsolated) {
        publishBinderService(name, service, allowIsolated, DUMP_FLAG_PRIORITY_DEFAULT);
    }

    /**
     * Publish the service so it is accessible to other services and apps.
     *
     * @param name the name of the new service
     * @param service the service object
     * @param allowIsolated set to true to allow isolated sandboxed processes
     * to access this service
     * @param dumpPriority supported dump priority levels as a bitmask
     */
    protected final void publishBinderService(String name, IBinder service,
            boolean allowIsolated, int dumpPriority) {
        ServiceManager.addService(name, service, allowIsolated, dumpPriority);
    }

    /**
     * Get a binder service by its name.
     */
    protected final IBinder getBinderService(String name) {
        return ServiceManager.getService(name);
    }

    /**
     * Publish the service so it is only accessible to the system process.
     */
    protected final <T> void publishLocalService(Class<T> type, T service) {
        LocalServices.addService(type, service);
    }

    /**
     * Get a local service by interface.
     */
    protected final <T> T getLocalService(Class<T> type) {
        return LocalServices.getService(type);
    }

    private SystemServiceManager getManager() {
        return LocalServices.getService(SystemServiceManager.class);
    }
}
