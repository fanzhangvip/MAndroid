package com.me94me.practice_customize_view.ui.foundation.f2paint.effect

import android.view.View
import com.me94me.practice_customize_view.R
import com.me94me.practice_customize_view.base.BaseFragment
import com.me94me.practice_customize_view.databinding.FragmentPaint2EffectBinding

/**
 * 1、setAntiAlias()
 *  2、setStyle()(Paint.Style.STROKE、Paint.Style.FILL、Paint.style.FILL_AND_STROKE)
 *  3、setStrokeWidth()/setStrokeCap()/setStrokeJoin()/setStrokeMiter()
 *  4、色彩优化：
 *          setDither(true)把图像从较高深度色彩（可用的颜色数）向较低色彩深度的区域绘制时，在图像中有意插入噪点，通过有规律扰乱图像使得图像更加真实
 *          现在Android版本默认色彩深度已经是32位ARGB_8888足够清晰了，只有选择16位色的ARGB_4444和RGB_565开启才有明显的效果
 *
 *          setFilterBitmap(true)是否使用双性线过滤来绘制Bitmap，让图像更加平滑
 *          优化Bitmap放大绘制后的效果
 *  5、setPathEffect(PathEffect effect)
 *  6、setShadowLayer()/clearShadowLayer
 *  7、setMaskFilter()
 *          BlurMaskFilter/EmbossMaskFilter
 *  8、getFillPath()/getTextPath
 */
class Paint2EffectFragment : BaseFragment() {

    var binding: FragmentPaint2EffectBinding? = null

    companion object {
        var titles = listOf(
                "setAntiAlias()",
                "setStyle()",
                "setStroke()",
                "setPathEffect()",
                "setShadowLayer()",
                "setMaskFilter()")
    }

    var position = 0
    override fun layoutId(): Int {
        return R.layout.fragment_paint2_effect
    }

    override fun initView() {
        binding = dataBinding as FragmentPaint2EffectBinding
        position = arguments!!.getInt("position")
    }

    override fun initData() {
        var view = View(context)
        when (position) {
            0 -> {
                view = Paint2Effect1AntiAiasView(context)
            }
            1 -> {
                view = Paint2Effect2StyleView(context)
            }
            2 -> {
                view = Paint2Effect3StrokeView(context)
            }
            3->{
                view = Paint2Effect4PathView(context)
            }
            4->{
                view = Paint2Effect5ShadowLayerView(context)
            }
            5->{
                view = Paint2Effect6MaskFilterView(context)
            }
        }
        binding!!.layout.addView(view)
    }

    override fun initClick() {

    }

}
