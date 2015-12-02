package com.meitu.captionlayout;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.text.Layout;

/**
 * 导出的文字字幕信息
 * Created by WuRS on 2015/12/2.
 */
public class TextCaptionInfo extends CaptionInfo {

    public float scale; // 缩放的倍数
    public String text; // 字幕内容
    public float textSize; // 字体大小
    public int textColor; // 字体颜色
    public int textBorderWidth; // 边框宽度
    public int textBorderColor; // 边框颜色
    public Typeface textTypeface; // 字体
    public Layout.Alignment textAlignment; // 字体对齐方式
    public int paddingLeft;
    public int paddingRight;
    public int paddingTop;
    public int paddingBottom;

    public TextCaptionInfo() {
    }

    public TextCaptionInfo(Bitmap captionBitmap, Rect locationRect, float degree, Matrix updateMatrix, float scale, String
            text, float textSize, int textColor, int textBorderWidth, int textBorderColor, Typeface textTypeface,
                           Layout.Alignment textAlignment, int paddingLeft, int paddingRight, int paddingTop, int
                                   paddingBottom) {
        super(captionBitmap, locationRect, degree, updateMatrix);
        this.scale = scale;
        this.text = text;
        this.textSize = textSize;
        this.textColor = textColor;
        this.textBorderWidth = textBorderWidth;
        this.textBorderColor = textBorderColor;
        this.textTypeface = textTypeface;
        this.textAlignment = textAlignment;
        this.paddingLeft = paddingLeft;
        this.paddingRight = paddingRight;
        this.paddingTop = paddingTop;
        this.paddingBottom = paddingBottom;
    }

    @Override
    public String toString() {
        return "TextCaptionInfo{" +
                "updateMatrix=" + updateMatrix.toShortString() +
                ", scale=" + scale +
                ", text='" + text + '\'' +
                ", textSize=" + textSize +
                ", textColor=" + textColor +
                ", textBorderWidth=" + textBorderWidth +
                ", textBorderColor=" + textBorderColor +
                ", textTypeface=" + textTypeface +
                ", textAlignment=" + textAlignment +
                ", paddingLeft=" + paddingLeft +
                ", paddingRight=" + paddingRight +
                ", paddingTop=" + paddingTop +
                ", paddingBottom=" + paddingBottom +
                '}';
    }
}
