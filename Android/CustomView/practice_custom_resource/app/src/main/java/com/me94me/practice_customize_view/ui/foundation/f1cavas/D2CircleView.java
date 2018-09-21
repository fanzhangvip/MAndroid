package com.me94me.practice_customize_view.ui.foundation.f1cavas;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

public class D2CircleView extends View {

    Paint paint = new Paint();

    public D2CircleView(Context context) {
        super(context);
    }

    public D2CircleView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public D2CircleView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        paint.setColor(Color.BLACK);
        //描边
        paint.setStyle(Paint.Style.STROKE);
        //描边的宽度
        paint.setAntiAlias(true);
        paint.setStrokeWidth(5);
        /**
         * cx       圆心x轴坐标
         * cy       圆心y轴坐标
         * radius   圆的半径
         * paint    画笔
         */
        canvas.drawCircle(300,300,100,paint);
    }
}
