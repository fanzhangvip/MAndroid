package com.me94me.practice_customize_view.ui.foundation.f4clipmatrix;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

import com.me94me.practice_customize_view.R;

import androidx.annotation.Nullable;

/**
 * ClipRect()
 * ClipPath()
 */
public class C1ClipView extends View {
    Paint paint = new Paint();

    public C1ClipView(Context context) {
        super(context);
    }

    public C1ClipView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public C1ClipView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.dog);
       // Rect rectsrc = new Rect(0,0,bitmap.getWidth(),bitmap.getWidth());

        canvas.save();
        canvas.clipRect(100,100,800,400);
        canvas.drawBitmap(bitmap,0,0,paint);
        canvas.restore();

        canvas.save();
        Path path = new Path();
        path.moveTo(0,500);
        path.rLineTo(400,400);
        path.rLineTo(400,-400);
        path.close();
        canvas.clipPath(path);
        canvas.drawBitmap(bitmap,0,0,paint);
        canvas.restore();
    }
}
