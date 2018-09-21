package com.me94me.practice_customize_view.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.fragment.app.Fragment

open class BaseFragment:Fragment() {
    var dataBinding:ViewDataBinding?=null
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        dataBinding = DataBindingUtil.bind(inflater.inflate(layoutId(),container,false))
        return dataBinding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
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