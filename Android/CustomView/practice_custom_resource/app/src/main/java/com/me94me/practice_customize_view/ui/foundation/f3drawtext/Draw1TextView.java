package com.me94me.practice_customize_view.ui.foundation.f3drawtext;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;

import java.util.Locale;

import androidx.annotation.Nullable;

public class Draw1TextView extends View {
    Paint paint = new Paint();
    String text = "I Love Android";
    public Draw1TextView(Context context) {
        super(context);
    }

    public Draw1TextView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public Draw1TextView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        //画文字(只能绘制单行的文字不能换行)
        paint.setColor(Color.BLACK);
        paint.setTextSize(50);
        canvas.drawText("I Love Android",0,100,paint);
        //按路径画文字
        paint.setTextSize(30);
        Path path = new Path();
        path.moveTo(500,0);
        path.rLineTo(100,100);
        path.rLineTo(100,-100);
        path.rLineTo(100,100);
        canvas.drawTextOnPath("I Love Android",path,5,5,paint);
        //StaticLayout用于画多行的文字

        paint.setHinting(5);//启动字体微调、现在手机像素已经很高了几乎不需要
        paint.setSubpixelText(true);//是否开启次像素级抗锯齿、现在手机像素已经很高了几乎不需要

        //Paint对文字的辅助
        paint.setTextSize(50);
        //设置字体
        paint.setTypeface(Typeface.MONOSPACE);
        canvas.drawText(text,0,200,paint);
        //设置加粗
        paint.setTypeface(Typeface.DEFAULT);
        paint.setFakeBoldText(true);
        canvas.drawText(text,500,200,paint);
        //添加删除线
        paint.setFakeBoldText(false);
        paint.setStrikeThruText(true);
        canvas.drawText(text,0,300,paint);
        //添加下划线
        paint.setStrikeThruText(false);
        paint.setUnderlineText(true);
        canvas.drawText(text,500,300,paint);
        //设置横向错切
        paint.setUnderlineText(false);
        paint.setTextSkewX(0.8f);
        canvas.drawText(text,0,400,paint);
        //设置横向拉伸
        paint.setTextSkewX(0f);
        paint.setTextScaleX(1.5f);
        canvas.drawText(text,500,400,paint);
        //设置字符间距
        paint.setTextScaleX(1f);
        paint.setLetterSpacing(1);
        canvas.drawText(text,0,500,paint);
        //paint.setFontFeatureSettings("smcp");
        //设置居中
        paint.setLetterSpacing(0);
        paint.setTextAlign(Paint.Align.CENTER);
        canvas.drawText(text,0,600,paint);
        //设置国家，繁体
        paint.setTextAlign(Paint.Align.LEFT);
        paint.setTextLocale(Locale.TAIWAN);
        canvas.drawText("我爱安卓",500,600,paint);


        //测量文字类
        //paint.getFontSpacing();//获取行距 小于bottom-top+leading
        //paint.getFontMetrics();//top/ascent/baseline/descent/bottom/leading
        //ascent为正、descent为负、leading为行的额外间距

        //测量文字的宽度 measureText()会比getTextBounds()大一点
        //paint.getTextBounds();//显示的宽度
        //paint.measureText()//占用的宽度
        //paint.getTextWidths()//measureText()的快捷方法
        //paint.breakText()超出长度截断文字

        //字符间距
        //paint.getLetterSpacing();

        //光标相关
        //paint.getRunAdvance()对于一段文字计算某个字符光标的x坐标
        //paint.getOffsetForAdvance()给出一个位置的像素值，计算出文字中最接近这个位置的字符偏移量(即第几个字符最接近这个坐标)

        //paint.hasGlyph("1111");//检查指定的字符串是否是一个单独的字形(glyph)

    }
}
