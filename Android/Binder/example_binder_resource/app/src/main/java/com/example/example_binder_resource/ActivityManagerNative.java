package com.example.example_binder_resource;
import android.app.ActivityManager;
import android.app.AppOpsManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.IBinder;

/**
 * {@hide}
 * @deprecated will be removed soon. See individual methods for alternatives.
 */
@Deprecated
public abstract class ActivityManagerNative {
    /**
     * Cast a Binder object into an activity manager interface, generating
     * a proxy if needed.
     *
     * @deprecated use IActivityManager.Stub.asInterface instead.
     */
    static public IActivityManager asInterface(IBinder obj) {
        return IActivityManager.Stub.asInterface(obj);
    }

    /**
     * Retrieve the system's default/global activity manager.
     *
     * @deprecated use ActivityManager.getService instead.
     */
    static public IActivityManager getDefault() {
        return ActivityManager.getService();
    }

    /**
     * Convenience for checking whether the system is ready.  For internal use only.
     *
     * @deprecated use ActivityManagerInternal.isSystemReady instead.
     */
    static public boolean isSystemReady() {
        return ActivityManager.isSystemReady();
    }

    /**
     * @deprecated use ActivityManager.broadcastStickyIntent instead.
     */
    static public void broadcastStickyIntent(Intent intent, String permission, int userId) {
        broadcastStickyIntent(intent, permission, AppOpsManager.OP_NONE, userId);
    }

    /**
     * Convenience for sending a sticky broadcast.  For internal use only.
     * If you don't care about permission, use null.
     *
     * @deprecated use ActivityManager.broadcastStickyIntent instead.
     */
    static public void broadcastStickyIntent(Intent intent, String permission, int appOp,
            int userId) {
        ActivityManager.broadcastStickyIntent(intent, appOp, userId);
    }

    /**
     * @deprecated use ActivityManager.noteWakeupAlarm instead.
     */
    static public void noteWakeupAlarm(PendingIntent ps, int sourceUid, String sourcePkg,
                                       String tag) {
        ActivityManager.noteWakeupAlarm(ps, null, sourceUid, sourcePkg, tag);
    }

    /**
     * @deprecated use ActivityManager.noteAlarmStart instead.
     */
    static public void noteAlarmStart(PendingIntent ps, int sourceUid, String tag) {
        ActivityManager.noteAlarmStart(ps, null, sourceUid, tag);
    }

    /**
     * @deprecated use ActivityManager.noteAlarmFinish instead.
     */
    static public void noteAlarmFinish(PendingIntent ps, int sourceUid, String tag) {
        ActivityManager.noteAlarmFinish(ps, null, sourceUid, tag);
    }
}
