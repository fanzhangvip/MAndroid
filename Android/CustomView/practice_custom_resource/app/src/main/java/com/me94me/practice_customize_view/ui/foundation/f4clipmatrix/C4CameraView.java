package com.me94me.practice_customize_view.ui.foundation.f4clipmatrix;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Camera;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import com.me94me.practice_customize_view.R;

import androidx.annotation.Nullable;

/**
 * Camera三维变换
 * camera.rotateZ()
 * camera.rotateY()
 * camera.rotateX()
 * camera.rotate(x,y,z)
 */
public class C4CameraView extends View {
    Paint paint = new Paint();
    public C4CameraView(Context context) {
        super(context);
    }

    public C4CameraView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public C4CameraView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.save();

        Camera camera = new Camera();
        camera.save();
        camera.rotateY(30);
        //camera.applyToCanvas(canvas);
        camera.restore();

        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.dog);
        canvas.scale(0.3f,0.3f);
        canvas.drawBitmap(bitmap,100,100,paint);
        camera.restore();
    }
}
