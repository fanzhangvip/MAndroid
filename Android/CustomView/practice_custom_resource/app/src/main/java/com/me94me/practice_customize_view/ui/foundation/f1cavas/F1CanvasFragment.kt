package com.me94me.practice_customize_view.ui.foundation.f1cavas


import android.view.View
import android.view.ViewGroup
import com.me94me.practice_customize_view.R
import com.me94me.practice_customize_view.base.BaseFragment
import com.me94me.practice_customize_view.databinding.FragmentF1CanvasBinding

class F1CanvasFragment : BaseFragment() {
    companion object {
        var titles = listOf(
                "画颜色",
                "画圆",
                "画矩形",
                "画点",
                "画椭圆",
                "画线",
                "画弧形",
                "画Path",
                "画Bitmap")
//        "画文字"单独举例
    }
    var binding:FragmentF1CanvasBinding?=null
    var positioin = 0

    override fun layoutId(): Int {
        return R.layout.fragment_f1_canvas
    }

    override fun initView() {
        binding = dataBinding as FragmentF1CanvasBinding
        positioin = arguments!!.getInt("position")
    }

    override fun initData() {
        var view= View(context)
        when(positioin){
            0->{
                view = D1ColorView(context)
                view.layoutParams = ViewGroup.LayoutParams(300,300)
            }
            1->{
                view = D2CircleView(context)
            }
            2->{
                view = D3RectView(context)
            }
            3->{
                view = D4PointView(context)
            }
            4->{
                view = D5OvalView(context)
            }
            5->{
                view = D6LineView(context)
            }
            6->{
                view = D7ArcView(context)
            }
            7->{
                view = D8PathView(context)
            }
            8->{
                view = D9BitmapView(context)
            }
        }
        binding!!.layout.addView(view)
    }
}
