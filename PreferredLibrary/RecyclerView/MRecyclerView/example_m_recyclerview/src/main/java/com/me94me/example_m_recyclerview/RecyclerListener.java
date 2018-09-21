package com.me94me.example_m_recyclerview;

import androidx.annotation.NonNull;

/**
 * A RecyclerListener can be set on a RecyclerView to receive messages whenever
 * a view is recycled.
 *
 * @see RecyclerView#setRecyclerListener(RecyclerListener)
 */
public interface RecyclerListener {

    /**
     * This method is called whenever the view in the ViewHolder is recycled.
     * <p>
     * RecyclerView calls this method right before clearing ViewHolder's internal data and
     * sending it to RecycledViewPool. This way, if ViewHolder was holding valid information
     * before being recycled, you can call {@link ViewHolder#getAdapterPosition()} to get
     * its adapter position.
     *
     * @param holder The ViewHolder containing the view that was recycled
     */
    void onViewRecycled(@NonNull ViewHolder holder);
}