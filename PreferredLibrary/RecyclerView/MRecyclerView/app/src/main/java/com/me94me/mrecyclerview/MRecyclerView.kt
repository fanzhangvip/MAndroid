package com.me94me.mrecyclerview

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import androidx.recyclerview.widget.RecyclerView

class MRecyclerView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyle: Int = 0) : RecyclerView(context, attrs, defStyle) {

    val TAG = this.javaClass.simpleName

    var paint = Paint()

    var isDown = false

    var view:View?=null

    override fun onDraw(c: Canvas) {
        super.onDraw(c)

    }

    override fun dispatchDraw(canvas: Canvas?) {
        super.dispatchDraw(canvas)
        paint.color = Color.BLACK
        canvas!!.drawCircle(500f,200f,20f,paint)
    }

    fun translate(e: MotionEvent){
        if(view == null){
            view = LayoutInflater.from(context).inflate(R.layout.refresh,null,false)
            addView(view)
        }
       // view!!.animate().translationY(e.rawY-yDown)
    }

    var yDown = 0f
    override fun onTouchEvent(e: MotionEvent?): Boolean {
        if(e == null){
            return true
        }
        when(e.action){
            MotionEvent.ACTION_DOWN->{
                yDown = e.rawY
            }
            MotionEvent.ACTION_MOVE->{
                if(e.rawY>yDown){
                    Log.e(TAG,(e.rawY-yDown).toString())
                    translate(e)
                }
            }
            MotionEvent.ACTION_UP->{

            }
            MotionEvent.ACTION_CANCEL->{

            }
        }
        return super.onTouchEvent(e)
    }
}