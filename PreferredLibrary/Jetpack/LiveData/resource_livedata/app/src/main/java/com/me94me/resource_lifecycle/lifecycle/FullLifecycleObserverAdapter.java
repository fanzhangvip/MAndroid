

package com.me94me.resource_lifecycle.lifecycle;

class FullLifecycleObserverAdapter implements GenericLifecycleObserver {

    private final FullLifecycleObserver mObserver;

    FullLifecycleObserverAdapter(FullLifecycleObserver observer) {
        mObserver = observer;
    }

    @Override
    public void onStateChanged(LifecycleOwner source, Lifecycle.Event event) {
        switch (event) {
            case ON_CREATE:
                mObserver.onCreate(source);
                break;
            case ON_START:
                mObserver.onStart(source);
                break;
            case ON_RESUME:
                mObserver.onResume(source);
                break;
            case ON_PAUSE:
                mObserver.onPause(source);
                break;
            case ON_STOP:
                mObserver.onStop(source);
                break;
            case ON_DESTROY:
                mObserver.onDestroy(source);
                break;
            case ON_ANY:
                throw new IllegalArgumentException("ON_ANY must not been send by anybody");
        }
    }
}
