package com.me94me.practice_customize_view.ui.foundation.f4clipmatrix;

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

public class C2CanvasView extends View {
    Paint paint = new Paint();
    public C2CanvasView(Context context) {
        super(context);
    }

    public C2CanvasView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public C2CanvasView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.dog);
        Rect rectSrc = new Rect(0,0,bitmap.getWidth(),bitmap.getHeight());

        Rect rectD0 = new Rect(100,100,400,250);
        canvas.drawBitmap(bitmap,rectSrc,rectD0,paint);
        //canvas平移
        canvas.save();
        canvas.translate(400,0);
        canvas.drawBitmap(bitmap,rectSrc,rectD0,paint);
        canvas.restore();
        //canvas旋转
        Rect rectD1 = new Rect(100,300,400,450);
        canvas.save();
        canvas.rotate(45,100,300);
        canvas.drawBitmap(bitmap,rectSrc,rectD1,paint);
        canvas.restore();
        //canvas缩放
        Rect rectD2 = new Rect(500,300,800,450);
        canvas.save();
        canvas.scale(1.2f,1.2f,rectD2.left+(rectD2.right-rectD2.left)/2,(rectD2.bottom-rectD2.top)/2+rectD2.top);
        canvas.drawBitmap(bitmap,rectSrc,rectD2,paint);
        canvas.restore();
        //canvas错切
        Rect rectD3 = new Rect(100,600,400,750);
        canvas.save();
        canvas.skew(0.5f,0f);
        canvas.drawBitmap(bitmap,rectSrc,rectD3,paint);
        canvas.restore();
    }
}
