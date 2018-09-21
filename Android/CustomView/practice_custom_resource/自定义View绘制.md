# 自定义View绘制

关于自定义练习的大纲

## 一、Canvas

画颜色

> canvas.drawColor(Color.BLACK);


画圆形
> canvas.drawCircle(300,300,100,paint);

画矩形

> canvas.drawRect(20,20,220,120,paint);
> canvas.drawRoundRect(20,300,520,600,50,50,paint);


画点

> canvas.drawPoint(200,200,paint);
> canvas.drawPoints(points1,paint);
> canvas.drawPoints(points2,2,4,paint);


画椭圆

> canvas.drawOval(100,100,600,400,paint);


画线


>canvas.drawLine(10,10,200,100,paint);
canvas.drawLines(lines,paint);
canvas.drawLines(lines1,2,4,paint);


画弧形


>canvas.drawArc(100,100,600,400,10,90,true,paint);


画路径


>canvas.drawPath(path1,paint);


画图形


>canvas.drawBitmap(bitmap,rect,rectDst,paint);

## 二、Paint

### 1、Paint对颜色的处理

Canvas绘制的内容有三层对颜色的处理

> Canvas.drawColor()/Argb()/Rgb()——颜色参数   
> Canvas.drawBitmap()——bitmap参数
> Canvas图形和文字绘制——paint参数

#### Paint参数

* Paint.setColor()
* Paint.setArgb()/setRgb()
* Paint.setShader()

>LinearGradient
RadialGradient
SweepGradient
BitmapShader
ComposeShader

* Paint.setColorFilter()

>LightingColorFilter
PorterDuffColorFilter
ColorMatrixColorFilter

* Paint.setXfermode()

### 2、Paint对效果的处理

* setAntiAlias()

* setStyle()

  > Paint.Style.STROKE、Paint.Style.FILL、Paint.style.FILL_AND_STROKE

* setStrokeWidth()/setStrokeCap()/setStrokeJoin()/setStrokeMiter()

* 色彩优化：

> setDither(true)把图像从较高深度色彩（可用的颜色数）向较低色彩深度的区域绘制时，在图像中有意插入噪点，通过有规律扰乱图像使得图像更加真实

> 现在Android版本默认色彩深度已经是32位ARGB_8888足够清晰了，只有选择16位色的ARGB_4444和RGB_565开启才有明显的效果

> setFilterBitmap(true)是否使用双性线过滤来绘制Bitmap，让图像更加平滑，用于优化Bitmap放大绘制后的效果

* setPathEffect(PathEffect effect)

> CornerPathEffect
> DiscretePathEffect
> DashPathEffect
> PathDashPathEffect
> SumPathEffect
> ComposePathEffect


* setShadowLayer()/clearShadowLayer

* setMaskFilter()

> BlurMaskFilter
> EmbossMaskFilter

### 3、Paint初始化

>Paint.reset()
Paint.set(Paint)
Paint.setFlag()

## 三、DrawText()

```Java
canvas.drawText("I Love Android",0,100,paint);
canvas.drawTextOnPath("I Love Android",path,5,5,paint);

paint.setHinting(5);//启动字体微调、现在手机像素已经很高了几乎不需要
paint.setSubpixelText(true);//是否开启次像素级抗锯齿、现在手机像素已经很高了几乎不需要

//Paint对文字的辅助
//设置字体
paint.setTypeface(Typeface.MONOSPACE);
//设置加粗
paint.setFakeBoldText(true);
//添加删除线
paint.setStrikeThruText(true);
//添加下划线
paint.setUnderlineText(true);
//设置横向错切
paint.setTextSkewX(0.8f);
//设置横向拉伸
paint.setTextScaleX(1.5f);
//设置字符间距
paint.setLetterSpacing(1);
//设置居中
paint.setTextAlign(Paint.Align.CENTER);
//设置国家，繁体
paint.setTextLocale(Locale.TAIWAN);

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
```

## 四、范围裁切和几何变换

### 1、范围裁切

> clipRect()
>
> clipPath()

### 3、几何变换

* 使用Canvas来做常见的二位变换
* 使用Matrix来做常见和不规则变换
* 使用Cmaera来做三维变换