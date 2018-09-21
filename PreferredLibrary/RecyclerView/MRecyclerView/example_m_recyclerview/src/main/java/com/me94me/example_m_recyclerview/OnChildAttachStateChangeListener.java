package com.me94me.example_m_recyclerview;

import android.view.View;

import androidx.annotation.NonNull;

/**
 * A Listener interface that can be attached to a RecylcerView to get notified
 * whenever a ViewHolder is attached to or detached from RecyclerView.
 */
public interface OnChildAttachStateChangeListener {

    /**
     * Called when a view is attached to the RecyclerView.
     *
     * @param view The View which is attached to the RecyclerView
     */
    void onChildViewAttachedToWindow(@NonNull View view);

    /**
     * Called when a view is detached from RecyclerView.
     *
     * @param view The View which is being detached from the RecyclerView
     */
    void onChildViewDetachedFromWindow(@NonNull View view);
}