package com.me94me.practice_customize_view.ui.foundation.f2paint.color


import android.view.View
import com.me94me.practice_customize_view.R
import com.me94me.practice_customize_view.base.BaseFragment
import com.me94me.practice_customize_view.databinding.FragmentPaint1ColorBinding

/**
 * 基本颜色
 *      Canvas.drawColor()/Argb()/Rgb()——颜色参数
 *      Canvas.drawBitmap()——bitmap参数
 *
 *      //该fragment
 *      Canvas图形和文字绘制——paint参数
 *          1、Paint.setColor()
 *          2、Paint.setArgb()/setRgb()
 *          3、Paint.setShader()
 *          4、Paint.setColorFilter()
 *          5、Paint.setXfermode()
 */
class Paint1ColorFragment : BaseFragment() {
    companion object {
        var titles = listOf(
                "Shader着色",
                "ColorFilter着色",
                "Xfermode着色")
    }
    var binding:FragmentPaint1ColorBinding?=null
    var position = 0
    override fun layoutId(): Int {
        return R.layout.fragment_paint1_color
    }

    override fun initView() {
        binding = dataBinding as FragmentPaint1ColorBinding
    }

    override fun initData() {
        position = arguments!!.getInt("position")
        var view = View(context)
        when(position){
            0->{
                view = Paint1Color1ShaderView(context)
            }
            1->{
                view = Paint1Color2FilterView(context)
            }
            2->{
                view = Paint1Color3XfermodeView(context)
            }
        }
        binding!!.layout.addView(view)
    }

}
