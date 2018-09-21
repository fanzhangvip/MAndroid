package com.me94me.practice_customize_view.ui.foundation.f2paint.effect;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

/**
 * 在硬件加速开启的情况下，setShadowLayer()只支持文字的绘制
 * 文字之外需要关闭硬件加速
 */
public class Paint2Effect5ShadowLayerView extends View {
    Paint paint = new Paint();
    public Paint2Effect5ShadowLayerView(Context context) {
        super(context);
    }

    public Paint2Effect5ShadowLayerView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public Paint2Effect5ShadowLayerView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        paint.setColor(Color.BLACK);
        //如果shadowColor是半透明的，阴影的透明度就使用shadowColor自己的透明度
        //而如果shadowColor不是透明的，阴影的透明度就使用paint的透明度
        paint.setShadowLayer(10,5,5,Color.RED);
        paint.setTextSize(50);
        canvas.drawText("I Love Android",100,200,paint);
        paint.clearShadowLayer();
    }
}
