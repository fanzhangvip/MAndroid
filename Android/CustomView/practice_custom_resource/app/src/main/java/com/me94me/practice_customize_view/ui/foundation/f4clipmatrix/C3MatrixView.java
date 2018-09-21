package com.me94me.practice_customize_view.ui.foundation.f4clipmatrix;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import com.me94me.practice_customize_view.R;

import androidx.annotation.Nullable;

/**
 * 常见变换
 * 1、创建Matrix对象
 * 2、调用Matrix的pre/postTranslate/Rotate/Scale/Skew()来设置几何变换
 * 3、使用Canvas.setMatrix(matrix)/Canvas.concat(matrix)把几何变换应用来canvas上
 * 4、记得canvas.save()/canvas.restore()
 *
 * 自定义变换
 * Matrix.setPolyToPoly()//多点对多点映射方式设置变换
 */
public class C3MatrixView extends View {
    Paint paint = new Paint();
    public C3MatrixView(Context context) {
        super(context);
    }

    public C3MatrixView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public C3MatrixView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.mipmap.dog);
        //Rect rectSrc = new Rect(0,0,bitmap.getWidth(),bitmap.getHeight());
        //Rect rectDst = new Rect(100,100,300,500);
        //canvas.drawBitmap(bitmap,rectSrc,rectDst,paint);


        float[] src = {0, 0,                                    // 左上
                bitmap.getWidth(), 0,                          // 右上
                bitmap.getWidth(), bitmap.getHeight(),        // 右下
                0, bitmap.getHeight()};                        // 左下

        float[] dst = {0, 0,                                    // 左上
                bitmap.getWidth(), 400,                        // 右上
                bitmap.getWidth(), bitmap.getHeight() - 200,  // 右下
                0, bitmap.getHeight()};

        canvas.save();
        Matrix matrix = new Matrix();
        matrix.setPolyToPoly(src,0,dst,0,src.length >> 1);//src.length/2
//        pointCount
//        0	相当于reset
//        1	相当于translate
//        2	可以进行 缩放、旋转、平移 变换
//        3	可以进行 缩放、旋转、平移、错切 变换
//        4	可以进行 缩放、旋转、平移、错切以及任何形变

        // 此处为了更好的显示对图片进行了等比缩放和平移(图片本身有点大)
        matrix.postScale(0.26f, 0.26f);
        //matrix.postTranslate(0,200);

        canvas.drawBitmap(bitmap,matrix,paint);
    }
}
