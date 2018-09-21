package com.me94me.resource_lifecycle.viewmodel;

import androidx.annotation.NonNull;

/**
 * A scope that owns {@link ViewModelStore}.
 * <p>
 * A responsibility of an implementation of this interface is to retain owned ViewModelStore
 * during the configuration changes and call {@link ViewModelStore#clear()}, when this scope is
 * going to be destroyed.
 */
public interface ViewModelStoreOwner {
    @NonNull
    ViewModelStore getViewModelStore();
}
