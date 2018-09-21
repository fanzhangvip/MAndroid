package com.me94me.practice_customize_view.ui


import android.content.Intent
import com.chad.library.adapter.base.BaseQuickAdapter
import com.me94me.practice_customize_view.R
import com.me94me.practice_customize_view.adapter.CFoundationAdapter
import com.me94me.practice_customize_view.base.BaseActivity
import com.me94me.practice_customize_view.databinding.ActivityC1FoundationBinding
import com.me94me.practice_customize_view.ui.foundation.f1cavas.F1CanvasActivity
import com.me94me.practice_customize_view.ui.foundation.f4clipmatrix.F4ClipMatrixActivity
import com.me94me.practice_customize_view.ui.foundation.f3drawtext.F3DrawTextActivity
import com.me94me.practice_customize_view.ui.foundation.f2paint.F2PaintActivity

class C1FoundationActivity : BaseActivity() {
    private var binding: ActivityC1FoundationBinding?=null
    var adapter:CFoundationAdapter?=null

    override fun layoutId(): Int {
        return R.layout.activity_c1_foundation
    }
    var onClickItem = BaseQuickAdapter.OnItemClickListener{adapter, view, position->
        when(position){
            0->{
                startActivity(Intent(this, F1CanvasActivity::class.java))
            }
            1->{
                startActivity(Intent(this,F2PaintActivity::class.java))
            }
            2->{
                startActivity(Intent(this,F3DrawTextActivity::class.java))
            }
            3->{
                startActivity(Intent(this,F4ClipMatrixActivity::class.java))
            }
            4->{

            }
        }
    }

    override fun initView() {
        adapter = CFoundationAdapter()
        binding = dataBinding as ActivityC1FoundationBinding
        binding!!.activity = this
    }

    override fun initData() {
        supportActionBar!!.title = "自定义View基础知识"
        adapter!!.setNewData(listOf(
                "F1Canvas",
                "F2Paint",
                "F3绘制文字",
                "F4ClipAndMatrix",
                "F5绘制顺序"))
    }
}
