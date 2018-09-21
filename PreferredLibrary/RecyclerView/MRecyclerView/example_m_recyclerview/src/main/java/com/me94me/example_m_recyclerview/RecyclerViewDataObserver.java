package com.me94me.example_m_recyclerview;

public class RecyclerViewDataObserver extends AdapterDataObserver {
    RecyclerViewDataObserver() {
    }

    @Override
    public void onChanged() {
        assertNotInLayoutOrScroll(null);
        mState.mStructureChanged = true;

        processDataSetCompletelyChanged(true);
        if (!mAdapterHelper.hasPendingUpdates()) {
            requestLayout();
        }
    }

    @Override
    public void onItemRangeChanged(int positionStart, int itemCount, Object payload) {
        assertNotInLayoutOrScroll(null);
        if (mAdapterHelper.onItemRangeChanged(positionStart, itemCount, payload)) {
            triggerUpdateProcessor();
        }
    }

    @Override
    public void onItemRangeInserted(int positionStart, int itemCount) {
        assertNotInLayoutOrScroll(null);
        if (mAdapterHelper.onItemRangeInserted(positionStart, itemCount)) {
            triggerUpdateProcessor();
        }
    }

    @Override
    public void onItemRangeRemoved(int positionStart, int itemCount) {
        assertNotInLayoutOrScroll(null);
        if (mAdapterHelper.onItemRangeRemoved(positionStart, itemCount)) {
            triggerUpdateProcessor();
        }
    }

    @Override
    public void onItemRangeMoved(int fromPosition, int toPosition, int itemCount) {
        assertNotInLayoutOrScroll(null);
        if (mAdapterHelper.onItemRangeMoved(fromPosition, toPosition, itemCount)) {
            triggerUpdateProcessor();
        }
    }

    void triggerUpdateProcessor() {
        if (POST_UPDATES_ON_ANIMATION && mHasFixedSize && mIsAttached) {
            ViewCompat.postOnAnimation(RecyclerView.this, mUpdateChildViewsRunnable);
        } else {
            mAdapterUpdateDuringMeasure = true;
            requestLayout();
        }
    }
}