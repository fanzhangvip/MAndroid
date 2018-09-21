package com.me94me.practice_customize_view.ui.foundation.f4clipmatrix


import android.view.View
import com.me94me.practice_customize_view.R
import com.me94me.practice_customize_view.base.BaseFragment
import com.me94me.practice_customize_view.databinding.FragmentF4ClipMatrixBinding


class F4ClipMatrixFragment : BaseFragment() {
    var position = 0
    var binding:FragmentF4ClipMatrixBinding?=null
    companion object {
        var titles = listOf(
                "Clip裁剪",
                "Canvas变换",
                "Matrix变换",
                "Camera三维变换")
    }

    override fun layoutId(): Int {
        return R.layout.fragment_f4_clip_matrix
    }

    override fun initView() {
        binding = dataBinding as FragmentF4ClipMatrixBinding
        position = arguments!!.getInt("position")
        var view = View(context)
        when(position){
            0->{
                view = C1ClipView(context)
            }
            1->{
                view = C2CanvasView(context)
            }
            2->{
                view = C3MatrixView(context)
            }
            3->{
                view = C4CameraView(context)
            }
        }
        binding!!.layout.addView(view)
    }


}
