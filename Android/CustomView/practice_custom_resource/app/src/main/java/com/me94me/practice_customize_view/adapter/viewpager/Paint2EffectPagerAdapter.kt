package com.me94me.practice_customize_view.adapter.viewpager

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import com.me94me.practice_customize_view.ui.foundation.f2paint.effect.Paint2EffectFragment

class Paint2EffectPagerAdapter(fragmentManager:FragmentManager) :FragmentStatePagerAdapter(fragmentManager) {

    override fun getItem(position: Int): Fragment {
        val bundle = Bundle()
        bundle.putInt("position",position)
        val fragment = Paint2EffectFragment()
        fragment.arguments = bundle
        return fragment
    }

    override fun getCount(): Int {
        return Paint2EffectFragment.titles.size
    }
}