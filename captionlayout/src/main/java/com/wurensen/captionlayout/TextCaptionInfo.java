package com.wurensen.captionlayout;

import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.text.Layout;

/**
 * 导出的文字字幕信息
 * Created by WuRS on 2015/12/2.
 */
public class TextCaptionInfo extends CaptionInfo {

    public String text; // 字幕内容
    public float textSize; // 字体大小
    public int textColor; // 字体颜色
    public int textBorderColor; // 边框颜色
    public Typeface textTypeface; // 字体
    public Layout.Alignment textAlignment; // 字体对齐方式
    public int textPadding; // 内容与边框的间距

    public TextCaptionInfo() {
    }

    public TextCaptionInfo(Bitmap captionBitmap, float degree, float relativeCenterX, float relativeCenterY, int width,
        int height, String text, float textSize, int textColor, int textBorderColor, Typeface textTypeface,
        Layout.Alignment textAlignment, int textPadding) {
        super(captionBitmap, degree, relativeCenterX, relativeCenterY, width, height);
        this.text = text;
        this.textSize = textSize;
        this.textColor = textColor;
        this.textBorderColor = textBorderColor;
        this.textTypeface = textTypeface;
        this.textAlignment = textAlignment;
        this.textPadding = textPadding;
    }
}
