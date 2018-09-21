package com.me94me.practice_customize_view.ui.foundation.f2paint.effect;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.EmbossMaskFilter;
import android.graphics.MaskFilter;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

import com.me94me.practice_customize_view.R;

import androidx.annotation.Nullable;

public class Paint2Effect6MaskFilterView extends View {

    Paint paint = new Paint();
    public Paint2Effect6MaskFilterView(Context context) {
        super(context);
    }

    public Paint2Effect6MaskFilterView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public Paint2Effect6MaskFilterView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        //模糊效果
        BlurMaskFilter maskFilter = new BlurMaskFilter(100, BlurMaskFilter.Blur.NORMAL);
        paint.setMaskFilter(maskFilter);

        //需要关闭硬件加速
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.dog);
        Rect rectSrc = new Rect(0,0,bitmap.getWidth(),bitmap.getHeight());
        Rect rectDst = new Rect(100,100,600,500);
        canvas.drawBitmap(bitmap,rectSrc,rectDst,paint);

        //浮雕效果
        //direction指定光源的方向、ambient指定光源的强度(0——1)、specular是光源系数、blurRadius应用光线的范围
        MaskFilter maskFilter1 = new EmbossMaskFilter(new float[]{0,1,1}, 0.7f,2,10);
        paint.setMaskFilter(maskFilter1);

        Rect rectDst1 = new Rect(100,700,600,1100);
        canvas.drawBitmap(bitmap,rectSrc,rectDst1,paint);

    }
}
