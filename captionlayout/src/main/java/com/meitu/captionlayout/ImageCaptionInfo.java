package com.meitu.captionlayout;

import android.graphics.Bitmap;

/**
 * 导出的贴图字幕信息
 * Created by WuRS on 2015/12/2.
 */
public class ImageCaptionInfo extends CaptionInfo {
    public Bitmap intrinsicBitmap; // 原先的贴图图片

    public ImageCaptionInfo() {
    }

    public ImageCaptionInfo(Bitmap captionBitmap, float degree, float relativeCenterX,
        float relativeCenterY, float relativeWidth, float relativeHeight, Bitmap intrinsicBitmap) {
        super(captionBitmap, degree, relativeCenterX, relativeCenterY, relativeWidth, relativeHeight);
        this.intrinsicBitmap = intrinsicBitmap;
    }

}
