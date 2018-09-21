package com.me94me.practice_customize_view.ui.foundation.f2paint.color

import androidx.viewpager.widget.ViewPager
import com.me94me.practice_customize_view.R
import com.me94me.practice_customize_view.adapter.viewpager.Paint1ColorPagerAdapter
import com.me94me.practice_customize_view.base.BaseActivity
import com.me94me.practice_customize_view.databinding.ActivityPaint1ColorBinding

class Paint1ColorActivity : BaseActivity() {
    var binding:ActivityPaint1ColorBinding?=null

    override fun layoutId(): Int {
        return R.layout.activity_paint1_color
    }

    override fun initView() {
        binding = dataBinding as ActivityPaint1ColorBinding
        binding!!.viewPager.adapter = Paint1ColorPagerAdapter(supportFragmentManager)
        supportActionBar!!.title = Paint1ColorFragment.titles[0]
    }

    override fun initClick() {
        binding!!.viewPager.addOnPageChangeListener(object: ViewPager.OnPageChangeListener {
            override fun onPageScrollStateChanged(state: Int) {

            }

            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
            }

            override fun onPageSelected(position: Int) {
                supportActionBar!!.title = Paint1ColorFragment.titles[position]
            }
        })
    }
}
