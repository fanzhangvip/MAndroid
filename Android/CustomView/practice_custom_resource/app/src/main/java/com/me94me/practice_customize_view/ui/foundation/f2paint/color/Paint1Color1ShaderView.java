package com.me94me.practice_customize_view.ui.foundation.f2paint.color;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ComposeShader;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.graphics.SweepGradient;
import android.util.AttributeSet;
import android.view.View;

import com.me94me.practice_customize_view.R;

import androidx.annotation.Nullable;

/**
 * Shader:
 * LinearGradient
 * RadialGradient
 * SweepGradient
 * BitmapShader
 * ComposeShader
 */
public class Paint1Color1ShaderView extends View {
    Paint paint = new Paint();
    public Paint1Color1ShaderView(Context context) {
        super(context);
    }

    public Paint1Color1ShaderView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public Paint1Color1ShaderView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        //LinearGradient
        /**
         * x0,y0,x1,y1渐变的两个端点的位置
         * 两个颜色为两个端点的颜色
         * tileMode 范围之外的着色规则
         *  CLAMP(同两端的颜色) , MIRROR(镜像模式) , REPEAT(重复模式)
         * */
        Shader shader = new LinearGradient(100,200,300,200,Color.BLUE, Color.YELLOW,Shader.TileMode.CLAMP);
        paint.setShader(shader);
        canvas.drawCircle(200,200,100,paint);

        //RadialGradient
        /**
         * centerX,centerY中心坐标
         * radius半径
         * tileMode 范围之外的着色规则
         *      CLAMP(同两端的颜色) , MIRROR(镜像模式) , REPEAT(重复模式)
         * */
        Shader shaderRadial = new RadialGradient(600,200,100,Color.BLUE, Color.YELLOW,Shader.TileMode.CLAMP);
        paint.setShader(shaderRadial);
        canvas.drawCircle(600,200,100,paint);

        //SweepGradient
        /**
         * x,y中心坐标
         * */
        Shader shaderSweep = new SweepGradient(200,600,Color.BLUE, Color.YELLOW);
        paint.setShader(shaderSweep);
        canvas.drawCircle(200,600,100,paint);

        //BitmapShader
        /** x轴范围之外的着色规则，y轴范围之外的着色规则 */
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.dog);
        Shader shaderBitmap = new BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
        paint.setShader(shaderBitmap);
        canvas.drawCircle(600,600,100,paint);

        //ComposeShader
        /** PorterDuff.Mode有17种查阅资料 */
        Bitmap bitmapCompose = BitmapFactory.decodeResource(getResources(), R.mipmap.fire);
        Shader shaderBitmap2 = new BitmapShader(bitmapCompose, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
        /**
         * ComposeShader在硬件加速下不支持
         */
        Shader shaderCompose = new ComposeShader(shaderBitmap,shaderBitmap2, PorterDuff.Mode.ADD);
        paint.setShader(shaderCompose);
        canvas.drawCircle(600,600,100,paint);
    }
}
