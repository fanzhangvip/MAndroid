package com.me94me.example_m_recyclerview;

import android.view.MotionEvent;

import androidx.annotation.NonNull;

/**
 * An OnItemTouchListener allows the application to intercept touch events in progress at the
 * view hierarchy level of the RecyclerView before those touch events are considered for
 * RecyclerView's own scrolling behavior.
 *
 * <p>This can be useful for applications that wish to implement various forms of gestural
 * manipulation of item views within the RecyclerView. OnItemTouchListeners may intercept
 * a touch interaction already in progress even if the RecyclerView is already handling that
 * gesture stream itself for the purposes of scrolling.</p>
 *
 * @see SimpleOnItemTouchListener
 */
public interface OnItemTouchListener {
    /**
     * Silently observe and/or take over touch events sent to the RecyclerView
     * before they are handled by either the RecyclerView itself or its child views.
     *
     * <p>The onInterceptTouchEvent methods of each attached OnItemTouchListener will be run
     * in the order in which each listener was added, before any other touch processing
     * by the RecyclerView itself or child views occurs.</p>
     *
     * @param e MotionEvent describing the touch event. All coordinates are in
     *          the RecyclerView's coordinate system.
     * @return true if this OnItemTouchListener wishes to begin intercepting touch events, false
     * to continue with the current behavior and continue observing future events in
     * the gesture.
     */
    boolean onInterceptTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e);

    /**
     * Process a touch event as part of a gesture that was claimed by returning true from
     * a previous call to {@link #onInterceptTouchEvent}.
     *
     * @param e MotionEvent describing the touch event. All coordinates are in
     *          the RecyclerView's coordinate system.
     */
    void onTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e);

    /**
     * Called when a child of RecyclerView does not want RecyclerView and its ancestors to
     * intercept touch events with
     * {@link ViewGroup#onInterceptTouchEvent(MotionEvent)}.
     *
     * @param disallowIntercept True if the child does not want the parent to
     *                          intercept touch events.
     * @see ViewParent#requestDisallowInterceptTouchEvent(boolean)
     */
    void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept);
}