package com.me94me.practice_customize_view.adapter.viewpager

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import com.me94me.practice_customize_view.ui.foundation.f2paint.color.Paint1ColorFragment

class F2PaintPagerAdapter(fragmentManager:FragmentManager) :FragmentStatePagerAdapter(fragmentManager) {

    override fun getItem(position: Int): Fragment {
        val bundle = Bundle()
        bundle.putInt("position",position)
        val fragment = Paint1ColorFragment()
        fragment.arguments = bundle
        return fragment
    }

    override fun getCount(): Int {
        return Paint1ColorFragment.titles.size
    }
}