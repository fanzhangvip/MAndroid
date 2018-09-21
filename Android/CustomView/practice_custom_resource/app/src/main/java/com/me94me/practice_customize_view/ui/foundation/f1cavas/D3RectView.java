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

public class D3RectView extends View {
    Paint paint = new Paint();
    public D3RectView(Context context) {
        super(context);
    }

    public D3RectView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public D3RectView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        paint.setColor(Color.BLACK);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5);
        paint.setAntiAlias(true);
        //canvas.drawRect(new Rect(),paint);
        //canvas.drawRect(new RectF(),paint);
        canvas.drawRect(20,20,220,120,paint);

        //画圆角矩形
        //canvas.drawRoundRect(new RectF(),paint);
        /** 前4个数表示左上右下*/
        /** rx表示x轴圆角半径，ry表示y轴的圆角半径*/
        canvas.drawRoundRect(20,300,520,600,50,50,paint);
    }
}
