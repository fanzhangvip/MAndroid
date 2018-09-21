package com.me94me.practice_customize_view.ui.foundation.f3drawtext


import android.view.View
import com.me94me.practice_customize_view.R
import com.me94me.practice_customize_view.base.BaseFragment
import com.me94me.practice_customize_view.databinding.FragmentDrawTextBinding

class DrawTextFragment : BaseFragment() {
    var position = 0
    var binding:FragmentDrawTextBinding?=null
    companion object {
        var titles = listOf(
                "画文字的方法"
        )
    }

    override fun layoutId(): Int {
        return R.layout.fragment_draw_text
    }

    override fun initView() {
        binding = dataBinding as FragmentDrawTextBinding

    }
    override fun initData() {
        position = arguments!!.getInt("position")
        var view = View(context)
        when(position){
            0->{
                view = Draw1TextView(context)
            }
        }
        binding!!.layout.addView(view)
    }


}
