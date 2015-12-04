package com.meitu.captionlayout;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Rect;

/**
 * 导出的贴图字幕信息
 * Created by WuRS on 2015/12/2.
 */
public class ImageCaptionInfo extends CaptionInfo {
    public Bitmap intrinsicBitmap; // 原先的贴图图片

    public ImageCaptionInfo() {
    }

    public ImageCaptionInfo(Bitmap captionBitmap, Rect locationRect, Rect intrinsicRect, float degree,
        Matrix updateMatrix, Bitmap intrinsicBitmap) {
        super(captionBitmap, locationRect, intrinsicRect, degree, updateMatrix);
        this.intrinsicBitmap = intrinsicBitmap;
    }

}
