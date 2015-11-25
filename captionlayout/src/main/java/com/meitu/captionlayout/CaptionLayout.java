package com.meitu.captionlayout;

import java.util.ArrayList;

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
     * @param captionView 字幕控件
     */
    public void addCaptionView(FlexibleCaptionView captionView) {
        clearChildrenFocus();
        addView(captionView);
    }

    /**
     * 移除字幕控件
     * @param captionView 字幕控件
     */
    public void removeCaptionView(FlexibleCaptionView captionView) {
        removeView(captionView);
    }

    /**
     * @return 获取所有字幕控件的信息
     */
    public ArrayList<CaptionInfo> findAllCaptionInfos() {
        ArrayList<CaptionInfo> captionInfos = new ArrayList<>();
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (child instanceof FlexibleCaptionView) {
                FlexibleCaptionView captionView = (FlexibleCaptionView) child;
                captionInfos.add(captionView.getCurrentCaption());
            }
        }
        return captionInfos;
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

    protected boolean isPointerDown;

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        // 记录是否为多指按下
        isPointerDown = (ev.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_POINTER_DOWN;
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            clearChildrenFocus();
        }
        return super.dispatchTouchEvent(ev);
    }

    private void clearChildrenFocus() {
        // 去除所有字幕控件的focus状态
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (child instanceof FlexibleCaptionView) {
                FlexibleCaptionView captionView = (FlexibleCaptionView) child;
                captionView.setFocus(false);
            }
        }
    }
}
