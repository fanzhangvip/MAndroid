
package com.me94me.resource_lifecycle.lifecycle;

import androidx.annotation.RestrictTo;

/**
 * 通用生命周期接口
 * 接收生命周期变化并分发事件给接受者
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public interface GenericLifecycleObserver extends LifecycleObserver {
    /**
     * 当状态切换时调用
     *
     * @param source 生命周期持有者
     * @param event The event
     */
    void onStateChanged(LifecycleOwner source, Lifecycle.Event event);
}
