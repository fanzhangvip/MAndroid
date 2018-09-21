package com.me94me.practice_customize_view.ui.foundation.f2paint.effect;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

public class Paint2Effect2StyleView extends View {
    Paint paint = new Paint();

    public Paint2Effect2StyleView(Context context) {
        super(context);
    }

    public Paint2Effect2StyleView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public Paint2Effect2StyleView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        paint.setColor(Color.BLACK);
        paint.setStrokeWidth(10);

        paint.setStyle(Paint.Style.FILL);

        canvas.drawCircle(200,200,100,paint);

        paint.setStyle(Paint.Style.STROKE);

        canvas.drawCircle(200,500,100,paint);

        paint.setStyle(Paint.Style.FILL_AND_STROKE);

        canvas.drawCircle(200,800,100,paint);

    }
}
