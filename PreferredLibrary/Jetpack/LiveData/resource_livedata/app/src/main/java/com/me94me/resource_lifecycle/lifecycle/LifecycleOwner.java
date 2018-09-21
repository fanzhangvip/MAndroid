
package com.me94me.resource_lifecycle.lifecycle;

import androidx.annotation.NonNull;

/**
 * 一个Android生命周期的类
 * 可用于自定义组件处理生命周期变化的事件，而不用在Activity或者Fragment内部实现任何代码
 */
public interface LifecycleOwner {
    /**
     * 返回提供者的生命周期
     */
    @NonNull
    Lifecycle getLifecycle();
}
