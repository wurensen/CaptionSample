package com.meitu.captionlayout;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Rect;

/**
 * 导出的字幕信息
 * Created by wrs on 2015/11/24.
 */
public abstract class CaptionInfo {
    public Bitmap captionBitmap; // 字幕图片
    public Rect targetRect; // 字幕在导出目标的位置信息（不带角度）
    public float degree; // 旋转角度，绕矩形中心点旋转
    public float relativeCenterX; // 相对字幕控件的中点x
    public float relativeCenterY; // 相对字幕控件的中点y
    public float relativeWidth; // 相对字幕控件的宽度
    public float relativeHeight; // 相对字幕控件的高度

    public CaptionInfo() {
    }

    public CaptionInfo(Bitmap captionBitmap, Rect targetRect, float degree, float relativeCenterX,
        float relativeCenterY, float relativeWidth, float relativeHeight) {
        this.captionBitmap = captionBitmap;
        this.targetRect = targetRect;
        this.degree = degree;
        this.relativeCenterX = relativeCenterX;
        this.relativeCenterY = relativeCenterY;
        this.relativeWidth = relativeWidth;
        this.relativeHeight = relativeHeight;
    }

    /**
     * @param viewX 横坐标x
     * @param viewY 纵坐标y
     * @return 返回传入的坐标是否落在字幕在字幕控件上的区域
     */
    public boolean isPointInIntrinsicRect(int viewWidth, int viewHeight, float viewX, float viewY) {
        // 坐标映射到控件上
        Matrix matrix = new Matrix();
        int centerX = (int) (relativeCenterX * viewWidth);
        int centerY = (int) (relativeCenterY * viewHeight);
        matrix.postRotate(-degree, centerX, centerY);
        float[] pst = new float[] {viewX, viewY};
        matrix.mapPoints(pst);
        int halfWidth = (int) (relativeWidth * viewWidth / 2);
        int halfHeight = (int) (relativeHeight * viewHeight / 2);
        int left = centerX - halfWidth;
        int right = centerX + halfWidth;
        int top = centerY - halfHeight;
        int bottom = centerY + halfHeight;
        Rect rect = new Rect(left, top, right, bottom);
        return rect.contains((int) pst[0], (int) pst[1]);
    }

}
