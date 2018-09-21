package com.me94me.mrecyclerview

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import kotlinx.android.synthetic.main.activity_m1_fast_scroller.*

class M1FastScrollerActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_m1_fast_scroller)

        val adapter = object :BaseQuickAdapter<String,BaseViewHolder>(R.layout.adapter_m1_fast_scroller){
            override fun convert(helper: BaseViewHolder?, item: String?) {

            }
        }
        recyclerView.adapter = adapter
        for (st in 0 .. 30){
            adapter.addData(st.toString())
        }
    }
}
