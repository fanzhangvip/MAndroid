package com.example.example_aidl

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.os.RemoteException
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {
    //由AIDL文件生成的Java类
    private var mBookManager: BookService? = null

    //标志当前与服务端连接状况的布尔值，false为未连接，true为连接中
    private var mBound = false

    //包含Book对象的list
    private var mBooks: List<Book>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tv_text.setOnClickListener {
            addBook()
        }
    }


    /**
     * 按钮的点击事件，点击之后调用服务端的addBookIn方法
     *
     * @param view
     */
    fun addBook() {
        //如果与服务端的连接处于未连接状态，则尝试连接
        if (!mBound) {
            attemptToBindService()
            Toast.makeText(this, "当前与服务端处于未连接状态，正在尝试重连，请稍后再试", Toast.LENGTH_SHORT).show()
            return
        }
        if (mBookManager == null) return


        val book = Book()
        book.setName("APP研发录In")
        book.setPrice(30)


        //向服务器添加book
        try {
            mBookManager!!.addBook(book)
            Log.e(localClassName, book.toString())
        } catch (e: RemoteException) {
            e.printStackTrace()
        }
    }


    /**
     * 尝试与服务端建立连接
     */
    private fun attemptToBindService() {
        val intent = Intent()
        intent.action = "com.example.example_aidl"
        intent.setPackage("com.example.example_aidl")
        bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE)
    }



    override fun onStart() {
        super.onStart()
        //绑定service
        if (!mBound) {
            attemptToBindService()
        }
    }



    override fun onStop() {
        super.onStop()
        //解绑service
        if (mBound) {
            unbindService(mServiceConnection)
            mBound = false
        }
    }



    private val mServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            Log.e(localClassName, "service connected")
            mBookManager = BookService.Stub.asInterface(service)
            mBound = true
            if (mBookManager != null) {
                try {
                    mBooks = mBookManager!!.books
                    Log.e(localClassName, mBooks.toString())
                } catch (e: RemoteException) {
                    e.printStackTrace()
                }
            }
        }
        override fun onServiceDisconnected(name: ComponentName) {
            Log.e(localClassName, "service disconnected")
            mBound = false
        }
    }
}
