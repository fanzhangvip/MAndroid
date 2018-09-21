package com.me94me.practice_customize_view.ui.foundation.f2paint.color;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.LightingColorFilter;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;

import com.me94me.practice_customize_view.R;

import androidx.annotation.Nullable;

/**
 * ColorFilter:
 * LightingColorFilter
 * PorterDuffColorFilter
 * ColorMatrixColorFilter
 */
public class Paint1Color2FilterView extends View {
    Paint paint = new Paint();
    public Paint1Color2FilterView(Context context) {
        super(context);
    }

    public Paint1Color2FilterView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public Paint1Color2FilterView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.dog);
        Rect rectSrc = new Rect(0,0,bitmap.getWidth(),bitmap.getHeight());
        Rect rectDst0 = new Rect(0,0,200,200);
        canvas.drawBitmap(bitmap,rectSrc,rectDst0,paint);

        //LightingColorFilter
        //LightingColorFilter(int mul,int add)(mul和add是和颜色值相同的int值)
        //默认mul为0xffffff,add为0x000000
        //R' = R * mul.R/0xff + add.R
        //G' = G * mul.G/0xff + add.G
        //B' = B * mul.B/0xff + add.B
        //如果要去掉红色可以将mul设置为0x00ffff
        ColorFilter colorFilter1 = new LightingColorFilter(0x00ffff,0x000000);
        Rect rectDst1 = new Rect(400,0,600,200);
        paint.setColorFilter(colorFilter1);
        canvas.drawBitmap(bitmap, rectSrc,rectDst1,paint);

        //PorterDuffColorFilter
        //使用一种指定的颜色来与绘制对象进行合成
        PorterDuffColorFilter colorFilter2 = new PorterDuffColorFilter(0x123451, PorterDuff.Mode.DST_OVER);
        paint.setColorFilter(colorFilter2);
        Rect rectDst2 = new Rect(0,300,200,500);
        canvas.drawBitmap(bitmap,rectSrc,rectDst2,paint);

        //ColorMatrixColorFilter
        //内部是一个4*5的矩阵
        //a,b,c,d,e
        //f,g,h,i,j
        //k,l,m,n,o
        //p,q,r,s,t
        //R' = a*R + b*G + c*B + d*A + e
        //G' = f*R + g*G + h*B + i*A + j
        //B' = k*R + l*G + m*B + n*A + o
        //A' = p*R + q*G + r*B + s*A + t

        //推荐参考这个库
        //https://github.com/chengdazhi/StyleImageView
        ColorMatrix colorMatrix = new ColorMatrix();
        colorMatrix.setSaturation(20);
        ColorMatrixColorFilter colorFilter3 = new ColorMatrixColorFilter(colorMatrix);
        paint.setColorFilter(colorFilter3);
        Rect rectDst3 = new Rect(400,300,600,500);
        canvas.drawBitmap(bitmap,rectSrc,rectDst3,paint);
    }
}
