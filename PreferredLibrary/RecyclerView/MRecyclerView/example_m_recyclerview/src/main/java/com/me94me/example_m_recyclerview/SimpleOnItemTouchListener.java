package com.me94me.example_m_recyclerview;

import android.view.MotionEvent;

import androidx.annotation.NonNull;

/**
 * An implementation of {@link RecyclerView.OnItemTouchListener} that has empty method bodies
 * and default return values.
 * <p>
 * You may prefer to extend this class if you don't need to override all methods. Another
 * benefit of using this class is future compatibility. As the interface may change, we'll
 * always provide a default implementation on this class so that your code won't break when
 * you update to a new version of the support library.
 */
public class SimpleOnItemTouchListener implements RecyclerView.OnItemTouchListener {
    @Override
    public boolean onInterceptTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {
        return false;
    }

    @Override
    public void onTouchEvent(@NonNull RecyclerView rv, @NonNull MotionEvent e) {
    }

    @Override
    public void onRequestDisallowInterceptTouchEvent(boolean disallowIntercept) {
    }
}