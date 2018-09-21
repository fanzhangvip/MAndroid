package com.me94me.practice_customize_view.ui.foundation.f2paint.effect;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ComposePathEffect;
import android.graphics.CornerPathEffect;
import android.graphics.DashPathEffect;
import android.graphics.DiscretePathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PathDashPathEffect;
import android.graphics.PathEffect;
import android.graphics.SumPathEffect;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

/**
 * CornerPathEffect
 * DiscretePathEffect
 * DashPathEffect
 * PathDashPathEffect
 * SumPathEffect
 * ComposePathEffect
 *
 * 有些情况不支持硬件加速最好把硬件加速关了
 */
public class Paint2Effect4PathView extends View {
    Paint paint = new Paint();

    Path path10 = new Path();
    Path path11 = new Path();

    Path path21 = new Path();

    Path path31 = new Path();

    Path path41 = new Path();
    Path path51 = new Path();
    Path path61 = new Path();

    public Paint2Effect4PathView(Context context) {
        super(context);
    }

    public Paint2Effect4PathView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public Paint2Effect4PathView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        paint.setColor(Color.BLACK);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5);

        //CornerPathEffect
        path10.rLineTo(150,100);
        path10.rLineTo(150,-100);
        path10.rLineTo(150,50);
        canvas.drawPath(path10,paint);
        path11.moveTo(500,0);
        path11.rLineTo(150,100);
        path11.rLineTo(150,-100);
        path11.rLineTo(150,50);
        PathEffect pathEffect1 = new CornerPathEffect(50);
        paint.setPathEffect(pathEffect1);
        canvas.drawPath(path11,paint);

        //DiscretePathEffect
        path21.moveTo(500,200);
        path21.rLineTo(150,100);
        path21.rLineTo(150,-100);
        path21.rLineTo(150,50);
        //segmentLength用来拼接的每个线段的长度
        //deviation偏移量
        PathEffect pathEffect2 = new DiscretePathEffect(20,10);
        paint.setPathEffect(pathEffect2);
        canvas.drawPath(path21,paint);

        //DashPathEffect
        //虚线
        path31.moveTo(500,400);
        path31.rLineTo(150,100);
        path31.rLineTo(150,-100);
        path31.rLineTo(150,50);
        //数组中必须是偶数，格式为画10像素，空10像素，画20像素，空20像素
        //phase虚线的偏移量
        PathEffect pathEffect3 = new DashPathEffect(new float[]{10,10,20,20},10);
        paint.setPathEffect(pathEffect3);
        canvas.drawPath(path31,paint);

        //PathDashPathEffect
        Path p = new Path();
        p.lineTo(10,10);
        p.rLineTo(10,-10);
        p.lineTo(0,0);
        path41.moveTo(500,600);
        path41.rLineTo(150,100);
        path41.rLineTo(150,-100);
        path41.rLineTo(150,50);
//advance是两个path之间的间隔
        //phase虚线偏移量
        //style
        PathEffect pathEffect4 = new PathDashPathEffect(p,10,10,PathDashPathEffect.Style.TRANSLATE);
        paint.setPathEffect(pathEffect4);
        canvas.drawPath(path41,paint);

        //SumPathEffect
        path51.moveTo(500,800);
        path51.rLineTo(150,100);
        path51.rLineTo(150,-100);
        path51.rLineTo(150,50);
        SumPathEffect pathEffect5 = new SumPathEffect(pathEffect3,pathEffect2);
        paint.setPathEffect(pathEffect5);
        canvas.drawPath(path51,paint);

        //ComposePathEffect
        path61.moveTo(500,1000);
        path61.rLineTo(150,100);
        path61.rLineTo(150,-100);
        path61.rLineTo(150,50);
        PathEffect pathEffect6 = new ComposePathEffect(pathEffect3,pathEffect2);
        paint.setPathEffect(pathEffect6);
        canvas.drawPath(path61,paint);
    }
}
