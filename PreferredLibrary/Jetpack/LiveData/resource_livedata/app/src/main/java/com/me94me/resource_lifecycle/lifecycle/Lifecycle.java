package com.me94me.resource_lifecycle.lifecycle;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;

/**
 * {@link androidx.fragment.app.Fragment Fragment} and {@link androidx.fragment.app.FragmentActivity FragmentActivity} classes implement
 * {@link LifecycleOwner} interface which has the {@link LifecycleOwner#getLifecycle()
 * getLifecycle} method to access the Lifecycle.
 */
public abstract class Lifecycle {

    @MainThread
    public abstract void addObserver(@NonNull LifecycleObserver observer);

    @MainThread
    public abstract void removeObserver(@NonNull LifecycleObserver observer);

    @MainThread
    @NonNull
    public abstract State getCurrentState();



    public enum Event {
        /**
         * Constant for onCreate event of the {@link LifecycleOwner}.
         */
        ON_CREATE,
        /**
         * Constant for onStart event of the {@link LifecycleOwner}.
         */
        ON_START,
        /**
         * Constant for onResume event of the {@link LifecycleOwner}.
         */
        ON_RESUME,
        /**
         * Constant for onPause event of the {@link LifecycleOwner}.
         */
        ON_PAUSE,
        /**
         * Constant for onStop event of the {@link LifecycleOwner}.
         */
        ON_STOP,
        /**
         * Constant for onDestroy event of the {@link LifecycleOwner}.
         */
        ON_DESTROY,
        /**
         * An {@link Event Event} constant that can be used to match all events.
         */
        ON_ANY
    }

    /**
     * 生命周期状态
     *
     * 可以将这些状态比作图上的结点，{@link Event}作为这些结点的边
     */
    public enum State {
        /**
         * 生命周期持有者的Destroyed状态
         * 该事件之后，该生命周期不会分发任何事件
         * 对于Activity来说
         * 在{@link android.app.Activity#onDestroy() onDestroy}之前调用
         */
        DESTROYED,

        /**
         * 生命周期持有者的Initialized状态
         * 对于Activity来说
         * 当状态已经构造但还没有接受到{@link android.app.Activity#onCreate(android.os.Bundle) onCreate}
         */
        INITIALIZED,

        /**
         * 生命周期持有者的Created状态
         * 对于Activity来说，该状态处于两种情况之间
         * <ul>
         *     <li>after {@link android.app.Activity#onCreate(android.os.Bundle) onCreate} call;
         *     <li><b>right before</b> {@link android.app.Activity#onStop() onStop} call.
         * </ul>
         */
        CREATED,

        /**
         * 生命周期持有者的Started状态
         * 对于Activity，状态处于两者情况之间
         * <ul>
         *     <li>after {@link android.app.Activity#onStart() onStart} call;
         *     <li><b>right before</b> {@link android.app.Activity#onPause() onPause} call.
         * </ul>
         */
        STARTED,

        /**
         * 生命周期持有者的Resumed状态
         * 对于Activity来说，在{@link android.app.Activity#onResume() onResume}后调用
         */
        RESUMED;


        /**
         * 比较当前状态是否大于或等于给定的{@code state}
         */
        public boolean isAtLeast(@NonNull State state) {
            return compareTo(state) >= 0;
        }
    }
}
