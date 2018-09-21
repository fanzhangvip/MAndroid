package com.me94me.practice_customize_view.ui.foundation.f2paint.color;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Xfermode;
import android.util.AttributeSet;
import android.view.View;

import com.me94me.practice_customize_view.R;

import androidx.annotation.Nullable;

/**
 * Xfermode:
 * Xfermode 即 TransferMode
 * 将要绘制的内容和目标内容怎么结合计算出最终的颜色
 */
public class Paint1Color3XfermodeView extends View {
    Paint paint = new Paint();
    public Paint1Color3XfermodeView(Context context) {
        super(context);
    }

    public Paint1Color3XfermodeView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public Paint1Color3XfermodeView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    /**
     * PorterDuff.Mode
     *
     * ComposeShader混合两个Shader
     * PorterDuffColorFilter增加一个单色的ColorFilter
     * Xfermode设置绘制内容与已有内容的混合计算方式
     */
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        Bitmap bitmapDog = BitmapFactory.decodeResource(getResources(), R.mipmap.dog);
        Bitmap bitmapFire = BitmapFactory.decodeResource(getResources(),R.mipmap.fire);

        Xfermode xfermode = new PorterDuffXfermode(PorterDuff.Mode.DST_OVER);

        //离屏缓冲
        int saved = canvas.saveLayer(null,null,Canvas.ALL_SAVE_FLAG);

        canvas.drawBitmap(bitmapDog,0,0,paint);
        paint.setXfermode(xfermode);
        canvas.drawBitmap(bitmapFire,0,0,paint);
        paint.setXfermode(null);

        canvas.restoreToCount(saved);

        /**
         * View.setLayerType()直接把整个view都绘制在离屏缓冲上
         *      setLayerType(LAYER_TYPE_HARDWARE)//使用GPU来缓冲
         *      setLayerType(LAYER_TYPE_SOFTWARE)//直接使用Bitmap来缓冲
         */
    }
}
