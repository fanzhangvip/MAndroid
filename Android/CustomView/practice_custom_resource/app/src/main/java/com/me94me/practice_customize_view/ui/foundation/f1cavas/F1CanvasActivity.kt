package com.me94me.practice_customize_view.ui.foundation.f1cavas

import androidx.viewpager.widget.ViewPager
import com.me94me.practice_customize_view.R
import com.me94me.practice_customize_view.adapter.viewpager.F1CanvasPagerAdapter
import com.me94me.practice_customize_view.base.BaseActivity
import com.me94me.practice_customize_view.databinding.ActivityF1CanvasBinding

class F1CanvasActivity : BaseActivity() {
    var binding:ActivityF1CanvasBinding?=null

    override fun layoutId(): Int {
        return R.layout.activity_f1_canvas
    }

    override fun initView() {
        binding = dataBinding as ActivityF1CanvasBinding
        binding!!.activity = this
        binding!!.viewPager.adapter = F1CanvasPagerAdapter(supportFragmentManager)

        supportActionBar!!.title = F1CanvasFragment.titles[0]
    }

    override fun initClick() {
        binding!!.viewPager.addOnPageChangeListener(object: ViewPager.OnPageChangeListener {
            override fun onPageScrollStateChanged(state: Int) {
            }
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
            }
            override fun onPageSelected(position: Int) {
                supportActionBar!!.title = F1CanvasFragment.titles[position]
            }
        })
    }
}
