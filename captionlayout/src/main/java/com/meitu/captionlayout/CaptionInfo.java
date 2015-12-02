package com.meitu.captionlayout;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.text.Layout;

/**
 * 导出的字幕信息
 * Created by meitu on 2015/11/24.
 */
public class CaptionInfo {
    public Bitmap bitmap; // 字幕图片
    public String text;
    public Matrix updateMatrix; // 变化矩阵
    public float textSize; // 字体大小
    public int textColor; // 字体颜色
    public int textBorderWidth;
    public int paddingLeft;
    public int paddingRight;
    public int paddingTop;
    public int paddingBottom;
    public Layout.Alignment textAlignment; // 字体对齐方式
    public Rect locationRect; // 字幕图片的位置信息
    public float degree; // 旋转角度，绕矩形中心点旋转

    public CaptionInfo() {
    }

    public CaptionInfo(Bitmap bitmap, Rect locationRect, float degree) {
        this.bitmap = bitmap;
        this.locationRect = locationRect;
        this.degree = degree;
    }

    @Override
    public String toString() {
        return "CaptionInfo{" + "bitmap=" + bitmap + ", locationRect=" + locationRect.toShortString() + ", degree="
            + degree + '}';
    }
}
