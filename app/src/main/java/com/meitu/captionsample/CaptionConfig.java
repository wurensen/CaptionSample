package com.meitu.captionsample;

import java.io.Serializable;

/**
 * Created by wrs on 2015/11/30.
 */
public class CaptionConfig implements Serializable {
    public String text;
    public float textSize;
    public int textColor;
    public int typefaceIndex;
    public int typefaceStyleIndex;
    public int paddingLeft;
    public int paddingRight;
    public int paddingTop;
    public int paddingBottom;

    public CaptionConfig(String text, float textSize, int textColor, int typefaceIndex, int typefaceStyleIndex,
        int paddingLeft, int paddingRight, int paddingTop, int paddingBottom) {
        this.text = text;
        this.textSize = textSize;
        this.textColor = textColor;
        this.typefaceIndex = typefaceIndex;
        this.typefaceStyleIndex = typefaceStyleIndex;
        this.paddingLeft = paddingLeft;
        this.paddingRight = paddingRight;
        this.paddingTop = paddingTop;
        this.paddingBottom = paddingBottom;
    }

    @Override
    public String toString() {
        return "CaptionConfig{" + "text='" + text + '\'' + ", textSize=" + textSize + ", textColor=" + textColor
            + ", typefaceIndex=" + typefaceIndex + ", typefaceStyleIndex=" + typefaceStyleIndex + ", paddingLeft="
            + paddingLeft + ", paddingRight=" + paddingRight + ", paddingTop=" + paddingTop + ", paddingBottom="
            + paddingBottom + '}';
    }
}
