

package com.me94me.resource_lifecycle.lifecycle;


import androidx.annotation.RestrictTo;

/**
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public interface GeneratedAdapter {

    /**
     * Called when a state transition event happens.
     *
     * @param source The source of the event
     * @param event The event
     * @param onAny approveCall onAny handlers
     * @param logger if passed, used to track called methods and prevent calling the same method
     *              twice
     */
    void callMethods(LifecycleOwner source, Lifecycle.Event event, boolean onAny,
            MethodCallsLogger logger);
}
