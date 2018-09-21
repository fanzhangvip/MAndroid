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

public class D5OvalView extends View {
    Paint paint = new Paint();
    public D5OvalView(Context context) {
        super(context);
    }

    public D5OvalView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public D5OvalView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        paint.setColor(Color.BLACK);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(10);
        paint.setAntiAlias(true);
        //canvas.drawOval(new RectF(),paint);
        canvas.drawOval(100,100,600,400,paint);
    }
}
