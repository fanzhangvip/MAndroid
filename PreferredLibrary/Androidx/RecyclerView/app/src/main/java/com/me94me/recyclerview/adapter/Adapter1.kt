package com.me94me.recyclerview.adapter

import android.widget.ImageView
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.me94me.recyclerview.R

/**
 *@author 94Me
 *@date 2018/10/23
 */
class Adapter1:BaseQuickAdapter<Int,BaseViewHolder>(R.layout.adapter1) {
    override fun convert(helper: BaseViewHolder?, item: Int?) {
        if(helper == null)return
        val imageview = helper.getView<ImageView>(R.id.iv)
        when(helper.adapterPosition){
            0->{
                imageview.setBackgroundResource(R.color.black)
            }
            1->{
                imageview.setBackgroundResource(R.color.red)
            }
            2->{
                imageview.setBackgroundResource(R.color.blue)
            }
            3->{
                imageview.setBackgroundResource(R.color.green)
            }
        }
    }
}