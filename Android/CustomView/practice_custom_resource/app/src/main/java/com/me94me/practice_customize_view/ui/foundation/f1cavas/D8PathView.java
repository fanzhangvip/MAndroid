package com.me94me.practice_customize_view.ui.foundation.f1cavas;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

public class D8PathView extends View {
    /**
     * Path有两类方法：
     *
     * 1、直接描述路径(addxxx()添加子图形和画线、xxxTo()画线)
     *
     * addXxx()添加子图形(addCircle()、addOval()、addRect()、addRoundRect()、addPath()、addArc())
     *
     * xxxTo()画线(lineTo()、rLineTo()、quadTo()、rQuadTo()、cubicTo()、rCubicTo()、moveTo()、rMoveTo()、arcTo())
     * 无论直线还是贝塞尔曲线不能指定起点，但可通过moveTo()、rMoveTo()移动启动
     *
     * close()封闭当前图形
     *
     * 2、辅助的设置和计算
     *
     * Path.setFillType(Path.FillType)
     * (WINDING、EVEN_ODD、INVERSE_WINDING、INVERSE_EVEN_ODD)
     *
     * WINDING：全填充
     *          原理:非零环绕数原则(要求所有线都是有方向的)
     *          任意一点一条射线，对于射线与图形的交点，初始为0，遇顺时针加1，遇逆时针减1，计算完不为0表示内部需要填充，为0表示外部不需要填充
     *
     * EVEN_ODD：非相交填充
     *           原理:奇偶原则
     *           任意一点一条射线，与Path图形的交点(相切不算)，为奇数表示Path内部需要填充;为偶数表示Path外部不需要填充
     */
    Path path1 = new Path();
    Paint paint = new Paint();
    public D8PathView(Context context) {
        super(context);
    }

    public D8PathView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public D8PathView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        paint.setColor(Color.BLACK);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5);

        //简单画一下
        path1.moveTo(600,400);
        path1.lineTo(700,300);
        path1.quadTo(650,200,600,300);
        path1.quadTo(550,200,500,300);
        path1.close();
        canvas.drawPath(path1,paint);
    }
}
