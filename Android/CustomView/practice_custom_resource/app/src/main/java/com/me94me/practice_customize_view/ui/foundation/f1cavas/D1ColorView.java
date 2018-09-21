package com.me94me.practice_customize_view.ui.foundation.f1cavas;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

public class D1ColorView extends View {
    public D1ColorView(Context context) {
        super(context);
    }

    public D1ColorView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public D1ColorView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor(Color.BLACK);

        //canvas.drawColor(Color.parseColor("#123456"));
        //canvas.drawRGB(12,12,12);
        //canvas.drawARGB(1,1,1,1);
    }
}
