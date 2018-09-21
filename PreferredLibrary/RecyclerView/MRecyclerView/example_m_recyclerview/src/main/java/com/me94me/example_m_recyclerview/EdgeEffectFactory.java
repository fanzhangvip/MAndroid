package com.me94me.example_m_recyclerview;

import android.widget.EdgeEffect;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;

/**
 * EdgeEffectFactory lets you customize the over-scroll edge effect for RecyclerViews.
 *
 * @see RecyclerView#setEdgeEffectFactory(EdgeEffectFactory)
 */
public class EdgeEffectFactory {

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({DIRECTION_LEFT, DIRECTION_TOP, DIRECTION_RIGHT, DIRECTION_BOTTOM})
    public @interface EdgeDirection {
    }

    /**
     * Direction constant for the left edge
     */
    public static final int DIRECTION_LEFT = 0;

    /**
     * Direction constant for the top edge
     */
    public static final int DIRECTION_TOP = 1;

    /**
     * Direction constant for the right edge
     */
    public static final int DIRECTION_RIGHT = 2;

    /**
     * Direction constant for the bottom edge
     */
    public static final int DIRECTION_BOTTOM = 3;

    /**
     * Create a new EdgeEffect for the provided direction.
     */
    protected @NonNull
    EdgeEffect createEdgeEffect(@NonNull RecyclerView view,
                                @EdgeDirection int direction) {
        return new EdgeEffect(view.getContext());
    }
}