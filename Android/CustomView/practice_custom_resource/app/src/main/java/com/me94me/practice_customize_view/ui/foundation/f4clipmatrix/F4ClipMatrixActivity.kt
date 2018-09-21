package com.me94me.practice_customize_view.ui.foundation.f4clipmatrix

import androidx.viewpager.widget.ViewPager
import com.me94me.practice_customize_view.R
import com.me94me.practice_customize_view.adapter.viewpager.F4ClipMatrixPagerAdapter
import com.me94me.practice_customize_view.base.BaseActivity
import com.me94me.practice_customize_view.databinding.ActivityF4ClipMatrixBinding

class F4ClipMatrixActivity : BaseActivity() {

    var binding:ActivityF4ClipMatrixBinding?=null

    override fun layoutId(): Int {
        return R.layout.activity_f4_clip_matrix
    }

    override fun initView() {
        binding = dataBinding as ActivityF4ClipMatrixBinding
        binding!!.viewPager.adapter = F4ClipMatrixPagerAdapter(supportFragmentManager)

        supportActionBar!!.title = F4ClipMatrixFragment.titles[0]
    }

    override fun initClick() {
        binding!!.viewPager.addOnPageChangeListener(object: ViewPager.OnPageChangeListener {
            override fun onPageScrollStateChanged(state: Int) {
            }

            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
            }

            override fun onPageSelected(position: Int) {
                supportActionBar!!.title = F4ClipMatrixFragment.titles[position]
            }
        })
    }
}
