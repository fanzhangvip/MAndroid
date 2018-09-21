package com.me94me.practice_customize_view.ui.foundation.f2paint.effect

import androidx.viewpager.widget.ViewPager
import com.me94me.practice_customize_view.R
import com.me94me.practice_customize_view.adapter.viewpager.Paint2EffectPagerAdapter
import com.me94me.practice_customize_view.base.BaseActivity
import com.me94me.practice_customize_view.databinding.ActivityPaint2EffectBinding

class Paint2EffectActivity : BaseActivity() {
    var binding:ActivityPaint2EffectBinding?=null

    override fun layoutId(): Int {
        return R.layout.activity_paint2_effect
    }

    override fun initView() {
        binding = dataBinding as ActivityPaint2EffectBinding
        binding!!.viewPager.adapter = Paint2EffectPagerAdapter(supportFragmentManager)

        supportActionBar!!.title = Paint2EffectFragment.titles[0]
    }

    override fun initClick() {
        binding!!.viewPager.addOnPageChangeListener(object: ViewPager.OnPageChangeListener {
            override fun onPageScrollStateChanged(state: Int) {

            }

            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
            }

            override fun onPageSelected(position: Int) {
                supportActionBar!!.title = Paint2EffectFragment.titles[position]
            }
        })
    }
}
