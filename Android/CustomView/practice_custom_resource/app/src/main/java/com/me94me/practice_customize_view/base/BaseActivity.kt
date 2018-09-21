package com.me94me.practice_customize_view.base

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding

@SuppressLint("Registered")
open class BaseActivity:AppCompatActivity() {
    var dataBinding:ViewDataBinding?=null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dataBinding = DataBindingUtil.setContentView<ViewDataBinding>(this,layoutId())
        initView()
        initData()
        initClick()
    }
    open fun layoutId():Int{
        return 0
    }
    open fun initView(){

    }

    open fun initData(){

    }
    open fun initClick(){

    }
}