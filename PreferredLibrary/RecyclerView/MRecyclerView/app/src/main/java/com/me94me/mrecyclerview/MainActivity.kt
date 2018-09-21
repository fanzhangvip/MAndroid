package com.me94me.mrecyclerview

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        val adapter = object :BaseQuickAdapter<String,BaseViewHolder>(R.layout.adapter){
            override fun convert(helper: BaseViewHolder?, item: String?) {
                val position = helper!!.adapterPosition

                helper.getView<TextView>(R.id.text).text = item

                helper.itemView.setOnClickListener {
                    when(position){
                        0->{
                            startActivity(Intent(this@MainActivity,M1FastScrollerActivity::class.java))
                        }
                    }
                }
            }
        }
        recyclerView.adapter = adapter
        adapter.setNewData(listOf("fastScroller"))

    }
}
