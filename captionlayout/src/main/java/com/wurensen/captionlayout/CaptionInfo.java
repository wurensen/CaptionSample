package com.wurensen.captionlayout;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Rect;

/**
 * 导出的字幕信息
 * Created by wrs on 2015/11/24.
 */
public abstract class CaptionInfo {
    public Bitmap captionBitmap; // 字幕图片
    public float degree; // 旋转角度，绕矩形中心点旋转
    public float relativeCenterX; // 相对字幕控件的中点x
    public float relativeCenterY; // 相对字幕控件的中点y
    public int width; // 绝对宽度
    public int height; // 绝对高度

    public CaptionInfo() {
    }

    public CaptionInfo(Bitmap captionBitmap, float degree, float relativeCenterX, float relativeCenterY,
        int width, int height) {
        this.captionBitmap = captionBitmap;
        this.degree = degree;
        this.relativeCenterX = relativeCenterX;
        this.relativeCenterY = relativeCenterY;
        this.width = width;
        this.height = height;
    }

    /**
     * @param viewWidth 控件宽度
     * @param viewHeight 控件高度
     * @param touchX 横坐标x
     * @param touchY 纵坐标y
     * @return 返回传入的坐标是否落在字幕在字幕控件上的区域
     */
    public boolean isTouchPointInCaption(int viewWidth, int viewHeight, float touchX, float touchY) {
        // 坐标映射到控件上
        Matrix matrix = new Matrix();
        int centerX = (int) (relativeCenterX * viewWidth);
        int centerY = (int) (relativeCenterY * viewHeight);
        matrix.postRotate(-degree, centerX, centerY);
        float[] pst = new float[] {touchX, touchY};
        matrix.mapPoints(pst);
        int halfWidth = (int) (width * 1.0f / 2);
        int halfHeight = (int) (height * 1.0f / 2);
        int left = centerX - halfWidth;
        int right = centerX + halfWidth;
        int top = centerY - halfHeight;
        int bottom = centerY + halfHeight;
        Rect rect = new Rect(left, top, right, bottom);
        return rect.contains((int) pst[0], (int) pst[1]);
    }

}
