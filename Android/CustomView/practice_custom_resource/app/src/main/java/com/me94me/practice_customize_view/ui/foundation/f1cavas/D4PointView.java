package com.me94me.practice_customize_view.ui.foundation.f1cavas;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

public class D4PointView extends View {
    Paint paint = new Paint();

    float[] points1 =new float[]{200,400,300,400,400,400,500,400};

    float[] points2 = new float[]{200,500,300,500,400,500,500,500};

    public D4PointView(Context context) {
        super(context);
    }

    public D4PointView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public D4PointView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        //设置点的颜色
        paint.setColor(Color.BLACK);
        paint.setAntiAlias(true);
        //设置点的大小
        paint.setStrokeWidth(50);
        //设置点的形状
        paint.setStrokeCap(Paint.Cap.ROUND);
        canvas.drawPoint(200,200,paint);

        //方头
        paint.setStrokeCap(Paint.Cap.SQUARE);
        canvas.drawPoint(400,200,paint);
        //平头(不要凸出去的头)
        paint.setStrokeCap(Paint.Cap.BUTT);
        canvas.drawPoint(600,200,paint);

        paint.setStrokeCap(Paint.Cap.ROUND);
        /** points1 每两个数组成一个点，即4个点 */
        canvas.drawPoints(points1,paint);
        /** points2 每两个数组成一个点，即4个点 */
        /** offset 表示越过几个数开始记坐标*/
        /** count 表示要应用几个数，需要为2的倍数，为4即画两个点*/
        canvas.drawPoints(points2,2,4,paint);
    }
}
