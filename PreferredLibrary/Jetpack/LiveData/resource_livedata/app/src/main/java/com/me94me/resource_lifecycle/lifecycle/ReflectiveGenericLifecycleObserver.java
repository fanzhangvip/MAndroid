

package com.me94me.resource_lifecycle.lifecycle;


/**
 * An internal implementation of {@link GenericLifecycleObserver} that relies on reflection.
 */
class ReflectiveGenericLifecycleObserver implements GenericLifecycleObserver {
    private final Object mWrapped;
    private final ClassesInfoCache.CallbackInfo mInfo;

    ReflectiveGenericLifecycleObserver(Object wrapped) {
        mWrapped = wrapped;
        mInfo = ClassesInfoCache.sInstance.getInfo(mWrapped.getClass());
    }

    @Override
    public void onStateChanged(LifecycleOwner source, Lifecycle.Event event) {
        mInfo.invokeCallbacks(source, event, mWrapped);
    }
}
