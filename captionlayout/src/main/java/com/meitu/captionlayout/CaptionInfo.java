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
    public Rect intrinsicRect; // 字幕在字幕控件上的位置信息（不带角度）
    public float degree; // 旋转角度，绕矩形中心点旋转
    public Matrix updateMatrix; // 导出时当前的矩阵

    public CaptionInfo() {
    }

    public CaptionInfo(Bitmap captionBitmap, Rect targetRect, Rect intrinsicRect, float degree, Matrix updateMatrix) {
        this.captionBitmap = captionBitmap;
        this.targetRect = targetRect;
        this.intrinsicRect = intrinsicRect;
        this.degree = degree;
        this.updateMatrix = updateMatrix;
    }

    @Override
    public String toString() {
        return "CaptionInfo{" + "captionBitmap=" + captionBitmap + ", targetRect=" + targetRect.toShortString()
            + ", intrinsicRect=" + intrinsicRect.toShortString() + ", degree=" + degree + ", updateMatrix="
            + updateMatrix + '}';
    }

    /**
     * @param viewX 横坐标x
     * @param viewY 纵坐标y
     * @return 返回传入的坐标是否落在字幕在字幕控件上的区域
     */
    public boolean isPointInIntrinsicRect(float viewX, float viewY) {
        // 坐标映射到控件上
        Matrix matrix = new Matrix();
        int centerX = (intrinsicRect.right - intrinsicRect.left) / 2;
        int centerY = (intrinsicRect.bottom - intrinsicRect.top) / 2;
        matrix.postRotate(-degree, centerX, centerY);
        float[] pst = new float[] {viewX, viewY};
        matrix.mapPoints(pst);
        return intrinsicRect.contains((int) pst[0], (int) pst[1]);
    }

}
