package com.me94me.practice_customize_view.adapter.viewpager

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import com.me94me.practice_customize_view.ui.foundation.f1cavas.F1CanvasFragment

class F1CanvasPagerAdapter(fragmentManager:FragmentManager) :FragmentStatePagerAdapter(fragmentManager) {

    override fun getItem(position: Int): Fragment {
        val bundle = Bundle()
        bundle.putInt("position",position)
        val fragment = F1CanvasFragment()
        fragment.arguments = bundle
        return fragment
    }

    override fun getCount(): Int {
        return F1CanvasFragment.titles.size
    }
}