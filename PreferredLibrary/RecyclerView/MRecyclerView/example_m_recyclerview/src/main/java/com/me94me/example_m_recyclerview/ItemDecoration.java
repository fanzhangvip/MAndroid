package com.me94me.example_m_recyclerview;

import android.graphics.Canvas;
import android.graphics.Rect;
import android.view.View;

import androidx.annotation.NonNull;

/**
 * An ItemDecoration allows the application to add a special drawing and layout offset
 * to specific item views from the adapter's data set. This can be useful for drawing dividers
 * between items, highlights, visual grouping boundaries and more.
 *
 * <p>All ItemDecorations are drawn in the order they were added, before the item
 * views (in {@link ItemDecoration#onDraw(Canvas, RecyclerView, RecyclerView.State) onDraw()}
 * and after the items (in {@link ItemDecoration#onDrawOver(Canvas, RecyclerView,
 * RecyclerView.State)}.</p>
 */
public abstract class ItemDecoration {
    /**
     * Draw any appropriate decorations into the Canvas supplied to the RecyclerView.
     * Any content drawn by this method will be drawn before the item views are drawn,
     * and will thus appear underneath the views.
     *
     * @param c      Canvas to draw into
     * @param parent RecyclerView this ItemDecoration is drawing into
     * @param state  The current state of RecyclerView
     */
    public void onDraw(@NonNull Canvas c, @NonNull RecyclerView parent, @NonNull State state) {
        onDraw(c, parent);
    }

    /**
     * @deprecated Override {@link #onDraw(Canvas, RecyclerView, RecyclerView.State)}
     */
    @Deprecated
    public void onDraw(@NonNull Canvas c, @NonNull RecyclerView parent) {
    }

    /**
     * Draw any appropriate decorations into the Canvas supplied to the RecyclerView.
     * Any content drawn by this method will be drawn after the item views are drawn
     * and will thus appear over the views.
     *
     * @param c      Canvas to draw into
     * @param parent RecyclerView this ItemDecoration is drawing into
     * @param state  The current state of RecyclerView.
     */
    public void onDrawOver(@NonNull Canvas c, @NonNull RecyclerView parent,
                           @NonNull State state) {
        onDrawOver(c, parent);
    }

    /**
     * @deprecated Override {@link #onDrawOver(Canvas, RecyclerView, RecyclerView.State)}
     */
    @Deprecated
    public void onDrawOver(@NonNull Canvas c, @NonNull RecyclerView parent) {
    }


    /**
     * @deprecated Use {@link #getItemOffsets(Rect, View, RecyclerView, State)}
     */
    @Deprecated
    public void getItemOffsets(@NonNull Rect outRect, int itemPosition,
                               @NonNull RecyclerView parent) {
        outRect.set(0, 0, 0, 0);
    }

    /**
     * Retrieve any offsets for the given item. Each field of <code>outRect</code> specifies
     * the number of pixels that the item view should be inset by, similar to padding or margin.
     * The default implementation sets the bounds of outRect to 0 and returns.
     *
     * <p>
     * If this ItemDecoration does not affect the positioning of item views, it should set
     * all four fields of <code>outRect</code> (left, top, right, bottom) to zero
     * before returning.
     *
     * <p>
     * If you need to access Adapter for additional data, you can call
     * {@link RecyclerView#getChildAdapterPosition(View)} to get the adapter position of the
     * View.
     *
     * @param outRect Rect to receive the output.
     * @param view    The child view to decorate
     * @param parent  RecyclerView this ItemDecoration is decorating
     * @param state   The current state of RecyclerView.
     */
    public void getItemOffsets(@NonNull Rect outRect, @NonNull View view,
                               @NonNull RecyclerView parent, @NonNull State state) {
        getItemOffsets(outRect, ((LayoutParams) view.getLayoutParams()).getViewLayoutPosition(),
                parent);
    }
}