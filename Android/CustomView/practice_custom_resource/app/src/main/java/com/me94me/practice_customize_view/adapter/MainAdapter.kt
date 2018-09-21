package com.me94me.practice_customize_view.adapter

import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.me94me.practice_customize_view.R

class MainAdapter:BaseQuickAdapter<String,BaseViewHolder>(R.layout.main_adapter) {
    override fun convert(helper: BaseViewHolder?, item: String?) {
        helper!!.setText(R.id.tv_title,item)
    }
}