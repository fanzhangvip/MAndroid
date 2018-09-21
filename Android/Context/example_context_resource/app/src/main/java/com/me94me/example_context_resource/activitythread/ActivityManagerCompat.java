package com.me94me.example_context_resource.activitythread;

import android.app.ActivityManager;
import android.os.Build;

import androidx.annotation.NonNull;

/**
 * Helper for accessing features in {@link android.app.ActivityManager} in a backwards compatible
 * fashion.
 */
public final class ActivityManagerCompat {

    private ActivityManagerCompat() {}

    /**
     * Returns true if this is a low-RAM device.  Exactly whether a device is low-RAM
     * is ultimately up to the device configuration, but currently it generally means
     * something in the class of a 512MB device with about a 800x480 or less screen.
     * This is mostly intended to be used by apps to determine whether they should turn
     * off certain features that require more RAM.
     */
    public static boolean isLowRamDevice(@NonNull ActivityManager activityManager) {
        if (Build.VERSION.SDK_INT >= 19) {
            return activityManager.isLowRamDevice();
        }
        return false;
    }
}
