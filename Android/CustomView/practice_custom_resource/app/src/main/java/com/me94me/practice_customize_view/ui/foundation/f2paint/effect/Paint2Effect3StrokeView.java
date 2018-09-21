package com.me94me.practice_customize_view.ui.foundation.f2paint.effect;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

public class Paint2Effect3StrokeView extends View {
    Paint paint = new Paint();
    Path path = new Path();
    Path path1 = new Path();
    Path path2 = new Path();
    Path path3 = new Path();
    public Paint2Effect3StrokeView(Context context) {
        super(context);
    }

    public Paint2Effect3StrokeView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public Paint2Effect3StrokeView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        paint.setColor(Color.BLACK);
        paint.setStyle(Paint.Style.STROKE);

        //setStrokeWidth
        paint.setStrokeWidth(1);
        canvas.drawCircle(200,200,100,paint);

        paint.setStrokeWidth(5);
        canvas.drawCircle(500,200,100,paint);

        paint.setStrokeWidth(10);
        canvas.drawCircle(800,200,100,paint);

        //setStrokeCap
        paint.setStrokeWidth(20);
        paint.setStrokeCap(Paint.Cap.BUTT);
        canvas.drawLine(20,400,500,400,paint);

        paint.setStrokeCap(Paint.Cap.ROUND);
        canvas.drawLine(20,450,500,450,paint);

        paint.setStrokeCap(Paint.Cap.SQUARE);
        canvas.drawLine(20,500,500,500,paint);

        //setStrokeJoin
        paint.setStrokeWidth(50);
        paint.setStrokeJoin(Paint.Join.MITER);
        path.moveTo(100,600);
        path.rLineTo(300,0);
        path.rLineTo(-150,100);
        canvas.drawPath(path,paint);


        paint.setStrokeJoin(Paint.Join.BEVEL);
        path1.moveTo(100,800);
        path1.rLineTo(300,0);
        path1.rLineTo(-150,100);
        canvas.drawPath(path1,paint);


        paint.setStrokeJoin(Paint.Join.ROUND);
        path2.moveTo(100,1000);
        path2.rLineTo(300,0);
        path2.rLineTo(-150,100);
        canvas.drawPath(path2,paint);


        paint.setStrokeJoin(Paint.Join.MITER);
        path3.moveTo(100,1200);
        path3.rLineTo(300,0);
        path3.rLineTo(-150,50);
        //延长线优化,设置最大延长值，超过将自动设为Paint.Join.BEVEL，针对setStrokeJoin()
        paint.setStrokeMiter(20);
        canvas.drawPath(path3,paint);
    }
}
