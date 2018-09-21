package com.me94me.example_m_recyclerview;

/**
     * A callback interface that can be used to alter the drawing order of RecyclerView children.
     * <p>
     * It works using the {@link ViewGroup#getChildDrawingOrder(int, int)} method, so any case
     * that applies to that method also applies to this callback. For example, changing the drawing
     * order of two views will not have any effect if their elevation values are different since
     * elevation overrides the result of this callback.
     */
    public interface ChildDrawingOrderCallback {
        /**
         * Returns the index of the child to draw for this iteration. Override this
         * if you want to change the drawing order of children. By default, it
         * returns i.
         *
         * @param i The current iteration.
         * @return The index of the child to draw this iteration.
         *
         * @see RecyclerView#setChildDrawingOrderCallback(RecyclerView.ChildDrawingOrderCallback)
         */
        int onGetChildDrawingOrder(int childCount, int i);
    }