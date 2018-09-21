package com.me94me.practice_customize_view

import android.content.Intent
import com.chad.library.adapter.base.BaseQuickAdapter
import com.me94me.practice_customize_view.adapter.MainAdapter
import com.me94me.practice_customize_view.base.BaseActivity
import com.me94me.practice_customize_view.databinding.ActivityMainBinding
import com.me94me.practice_customize_view.ui.C1FoundationActivity

class MainActivity : BaseActivity() {

    var binding:ActivityMainBinding?=null
    var adapter:MainAdapter?=null

    override fun layoutId(): Int {
        return R.layout.activity_main
    }

    var onClickItem = BaseQuickAdapter.OnItemClickListener { adapter, view, position ->
        when(position){
            0->{
                startActivity(Intent(this,C1FoundationActivity::class.java))
            }
        }
    }

    override fun initView() {
        adapter = MainAdapter()
        binding = dataBinding as ActivityMainBinding
        binding!!.activity = this
    }

    override fun initData() {
        adapter!!.setNewData(listOf("自定义View基础知识","进阶"))
    }
}
