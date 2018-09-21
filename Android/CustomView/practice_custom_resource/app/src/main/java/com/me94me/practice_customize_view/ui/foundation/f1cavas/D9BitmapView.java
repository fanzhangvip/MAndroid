package com.me94me.practice_customize_view.ui.foundation.f1cavas;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

import com.me94me.practice_customize_view.R;

import androidx.annotation.Nullable;

public class D9BitmapView extends View {
    Bitmap bitmap = null;
    Paint paint = new Paint();
    public D9BitmapView(Context context) {
        super(context);
    }

    public D9BitmapView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public D9BitmapView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        bitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.fire);
        Rect rect = new Rect(0,0,bitmap.getWidth(),bitmap.getHeight());
        Rect rectDst = new Rect(200,200,800,800);
        //canvas.drawBitmap(bitmap,0,0,paint);
        //canvas.drawBitmap(bitmap,new Matrix(),paint);
        canvas.drawBitmap(bitmap,rect,rectDst,paint);

        bitmap.recycle();
        bitmap = null;
    }
}
