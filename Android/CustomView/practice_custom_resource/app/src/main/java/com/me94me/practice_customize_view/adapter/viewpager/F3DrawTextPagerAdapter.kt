package com.me94me.practice_customize_view.adapter.viewpager

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import com.me94me.practice_customize_view.ui.foundation.f3drawtext.DrawTextFragment

class F3DrawTextPagerAdapter(fragmentManager:FragmentManager) :FragmentStatePagerAdapter(fragmentManager) {

    override fun getItem(position: Int): Fragment {
        val bundle = Bundle()
        bundle.putInt("position",position)
        val fragment = DrawTextFragment()
        fragment.arguments = bundle
        return fragment
    }

    override fun getCount(): Int {
        return DrawTextFragment.titles.size
    }
}