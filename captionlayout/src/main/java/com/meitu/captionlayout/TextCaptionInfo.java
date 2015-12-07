package com.meitu.captionlayout;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.text.Layout;

/**
 * 导出的文字字幕信息
 * Created by WuRS on 2015/12/2.
 */
public class TextCaptionInfo extends CaptionInfo {

    public String text; // 字幕内容
    public float relativeTextSize; // 字体大小比例，相对于控件宽度
    public int textColor; // 字体颜色
    public int textBorderColor; // 边框颜色
    public Typeface textTypeface; // 字体
    public Layout.Alignment textAlignment; // 字体对齐方式
    public float relativeTextPadding; // 内容与边框的间距比例，相对于控件宽度

    public TextCaptionInfo() {
    }

    public TextCaptionInfo(Bitmap captionBitmap, Rect targetRect, float degree, float relativeCenterX,
        float relativeCenterY, float relativeWidth, float relativeHeight, String text, float relativeTextSize,
        int textColor, int textBorderColor, Typeface textTypeface, Layout.Alignment textAlignment, float relativeTextPadding) {
        super(captionBitmap, targetRect, degree, relativeCenterX, relativeCenterY, relativeWidth, relativeHeight);
        this.text = text;
        this.relativeTextSize = relativeTextSize;
        this.textColor = textColor;
        this.textBorderColor = textBorderColor;
        this.textTypeface = textTypeface;
        this.textAlignment = textAlignment;
        this.relativeTextPadding = relativeTextPadding;
    }
}
