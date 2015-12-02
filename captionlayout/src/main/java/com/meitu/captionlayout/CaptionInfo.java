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
    public Rect locationRect; // 字幕的位置信息
    public float degree; // 旋转角度，绕矩形中心点旋转
    public Matrix updateMatrix; // 导出时当前的矩阵

    public CaptionInfo() {
    }

    public CaptionInfo(Bitmap captionBitmap, Rect locationRect, float degree, Matrix updateMatrix) {
        this.captionBitmap = captionBitmap;
        this.locationRect = locationRect;
        this.degree = degree;
        this.updateMatrix = updateMatrix;
    }

    @Override
    public String toString() {
        return "CaptionInfo{" +
                "captionBitmap=" + captionBitmap +
                ", locationRect=" + locationRect.toShortString() +
                ", degree=" + degree +
                ", updateMatrix=" + updateMatrix.toShortString() +
                '}';
    }

}
