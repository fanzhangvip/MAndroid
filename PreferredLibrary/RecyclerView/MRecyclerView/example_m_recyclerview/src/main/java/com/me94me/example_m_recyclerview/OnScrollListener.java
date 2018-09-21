package com.me94me.example_m_recyclerview;

import androidx.annotation.NonNull;

/**
 * An OnScrollListener can be added to a RecyclerView to receive messages when a scrolling event
 * has occurred on that RecyclerView.
 * <p>
 *
 * @see RecyclerView#addOnScrollListener(OnScrollListener)
 * @see RecyclerView#clearOnChildAttachStateChangeListeners()
 */
public abstract class OnScrollListener {
    /**
     * Callback method to be invoked when RecyclerView's scroll state changes.
     *
     * @param recyclerView The RecyclerView whose scroll state has changed.
     * @param newState     The updated scroll state. One of {@link #SCROLL_STATE_IDLE},
     *                     {@link #SCROLL_STATE_DRAGGING} or {@link #SCROLL_STATE_SETTLING}.
     */
    public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
    }

    /**
     * Callback method to be invoked when the RecyclerView has been scrolled. This will be
     * called after the scroll has completed.
     * <p>
     * This callback will also be called if visible item range changes after a layout
     * calculation. In that case, dx and dy will be 0.
     *
     * @param recyclerView The RecyclerView which scrolled.
     * @param dx           The amount of horizontal scroll.
     * @param dy           The amount of vertical scroll.
     */
    public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
    }
}