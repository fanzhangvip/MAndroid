package com.me94me.practice_customize_view.ui.foundation.f2paint

import android.content.Intent
import com.chad.library.adapter.base.BaseQuickAdapter
import com.me94me.practice_customize_view.R
import com.me94me.practice_customize_view.adapter.F2PaintAdapter
import com.me94me.practice_customize_view.base.BaseActivity
import com.me94me.practice_customize_view.databinding.ActivityF2PaintBinding
import com.me94me.practice_customize_view.ui.foundation.f2paint.color.Paint1ColorActivity
import com.me94me.practice_customize_view.ui.foundation.f2paint.effect.Paint2EffectActivity
import com.me94me.practice_customize_view.ui.foundation.f2paint.initialize.Paint3InitializeActivity

/**
 * Canvas绘制的内容有三层对颜色的处理
 * 1、基本颜色
 *      Canvas.drawColor()/Argb()/Rgb()——颜色参数
 *      Canvas.drawBitmap()——bitmap参数
 *      Canvas图形和文字绘制——paint参数/** */
 *
 * 1、ColorFilter
 *      Paint.setColorFilter()
 *
 * 2、Xfermode
 *      Paint.setXfermode()
 */
class F2PaintActivity : BaseActivity() {

    var binding: ActivityF2PaintBinding? = null
    var adapter: F2PaintAdapter? = null

    override fun layoutId(): Int {
        return R.layout.activity_f2_paint
    }

    var onClickItem = BaseQuickAdapter.OnItemClickListener { adapter, view, position ->
        when(position){
            0->{
                startActivity(Intent(this, Paint1ColorActivity::class.java))
            }
            1->{
                startActivity(Intent(this,Paint2EffectActivity::class.java))
            }
            2->{
                startActivity(Intent(this,Paint3InitializeActivity::class.java))
            }
        }
    }

    override fun initView() {
        adapter = F2PaintAdapter()
        binding = dataBinding as ActivityF2PaintBinding
        binding!!.activity = this

        supportActionBar!!.title = "Paint着色"
    }

    override fun initData() {
        adapter!!.setNewData(listOf("Paint1颜色","Paint2效果","Paint3初始化"))
    }

}
