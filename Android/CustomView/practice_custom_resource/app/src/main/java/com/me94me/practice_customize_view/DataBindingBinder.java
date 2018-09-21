package com.me94me.practice_customize_view;

import com.chad.library.adapter.base.BaseQuickAdapter;

import androidx.databinding.BindingAdapter;
import androidx.recyclerview.widget.RecyclerView;

public class DataBindingBinder {
    @BindingAdapter(value = "onItemClickListener",requireAll = false)
    public static void setOnclick(RecyclerView recyclerView, BaseQuickAdapter.OnItemClickListener listener){
        BaseQuickAdapter adapter = (BaseQuickAdapter) recyclerView.getAdapter();
        if (adapter != null) {
            adapter.setOnItemClickListener(listener);
        }
    }
}
