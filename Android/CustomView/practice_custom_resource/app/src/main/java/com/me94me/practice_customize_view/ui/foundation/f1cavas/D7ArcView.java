package com.me94me.practice_customize_view.ui.foundation.f1cavas;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

/**
 * 画弧形
 */
public class D7ArcView extends View {
    Paint paint = new Paint();
    public D7ArcView(Context context) {
        super(context);
    }

    public D7ArcView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public D7ArcView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        paint.setColor(Color.BLACK);
        paint.setStyle(Paint.Style.STROKE);
        paint.setAntiAlias(true);
        paint.setStrokeWidth(10);

        //canvas.drawArc(new RectF(),10,90,true,paint);
        /** 前4个表示矩形 */
        /** startAngle 开始角度 正数为顺时针 */
        /** sweepAngle 弧形度数 */
        /** useCenter true表示画到达圆心的线，false不画到达圆心的线*/
        canvas.drawArc(100,100,600,400,10,90,true,paint);

        canvas.drawArc(100,100,600,400,-10,-90,false,paint);
    }
}
