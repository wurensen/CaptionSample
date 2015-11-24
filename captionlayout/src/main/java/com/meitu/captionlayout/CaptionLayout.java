package com.meitu.captionlayout;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

/**
 * 字幕控件布局，提供多个字幕控件的控制，增加，移除，上下层关系的改变
 * 
 * Created by meitu on 2015/11/17.
 */
public class CaptionLayout extends FrameLayout {

    public CaptionLayout(Context context) {
        this(context, null);
    }

    public CaptionLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CaptionLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    /**
     * 新增字幕控件
     * @param captionView
     */
    public void addCaptionView(FlexibleCaptionView captionView) {
        addView(captionView);
    }

    /**
     * 移除字幕控件
     * @param captionView
     */
    public void removeCaptionView(FlexibleCaptionView captionView) {
        removeView(captionView);
    }

    /**
     * @return 获取当前操作的字幕控件
     */
    public FlexibleCaptionView getCurrentFocusCaptionView() {
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (child instanceof FlexibleCaptionView) {
                FlexibleCaptionView captionView = (FlexibleCaptionView) child;
                if (captionView.getFocus()) {
                    return captionView;
                }
            }
        }
        return null;
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            // 去除所有focus状态
            for (int i = 0; i < getChildCount(); i++) {
                View child = getChildAt(i);
                if (child instanceof FlexibleCaptionView) {
                    FlexibleCaptionView captionView = (FlexibleCaptionView) child;
                    captionView.setFocus(false);
                }
            }
        }
        return super.dispatchTouchEvent(ev);
    }
}
