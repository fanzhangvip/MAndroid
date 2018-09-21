package com.me94me.practice_customize_view.ui.foundation.f3drawtext

import androidx.viewpager.widget.ViewPager
import com.me94me.practice_customize_view.R
import com.me94me.practice_customize_view.adapter.viewpager.F3DrawTextPagerAdapter
import com.me94me.practice_customize_view.base.BaseActivity
import com.me94me.practice_customize_view.databinding.ActivityF3DrawTextBinding

class F3DrawTextActivity : BaseActivity() {
    var binding:ActivityF3DrawTextBinding?=null
    override fun layoutId(): Int {
        return R.layout.activity_f3_draw_text
    }

    override fun initView() {
        binding = dataBinding as ActivityF3DrawTextBinding
        binding!!.viewPager.adapter = F3DrawTextPagerAdapter(supportFragmentManager)

        supportActionBar!!.title = DrawTextFragment.titles[0]
    }

    override fun initClick() {
        binding!!.viewPager.addOnPageChangeListener(object: ViewPager.OnPageChangeListener {
            override fun onPageScrollStateChanged(state: Int) {

            }

            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
            }

            override fun onPageSelected(position: Int) {
                supportActionBar!!.title = DrawTextFragment.titles[position]
            }
        })
    }

}
