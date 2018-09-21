package com.me94me.practice_customize_view.ui.foundation.f1cavas;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

public class D6LineView extends View {
    float[] lines = new float[]{10,300,400,300  ,400,300,400,600  ,400,600,10,600  ,10,600,10,300};

    float[] lines1 = new float[]{500,500, 500,400,900,400};
    Paint paint = new Paint();
    public D6LineView(Context context) {
        super(context);
    }

    public D6LineView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public D6LineView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        paint.setColor(Color.BLACK);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(10);
        paint.setAntiAlias(true);
        canvas.drawLine(10,10,200,100,paint);

        canvas.drawLines(lines,paint);

        /**
         * lines1 数组
         * offset 越过几个数开始算，2表示越过2个数
         * count 需要使用几个数，需要4的倍数才会绘制一条直线
         */
        canvas.drawLines(lines1,2,4,paint);
    }
}
