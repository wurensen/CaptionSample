package com.meitu.captionlayout;

import android.content.Context;
import android.support.v4.widget.ViewDragHelper;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

/**
 * 字幕控件布局
 * 
 * Created by meitu on 2015/11/17.
 */
public class CaptionLayout extends FrameLayout {

    private ViewDragHelper dragHelper;

    public CaptionLayout(Context context) {
        this(context, null);
    }

    public CaptionLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CaptionLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initDragHelper();
    }

    private void initDragHelper() {
        dragHelper = ViewDragHelper.create(this, 1.0f, new ViewDragHelper.Callback() {

            @Override
            public boolean tryCaptureView(View child, int pointerId) {
                return true;
            }

            @Override
            public int clampViewPositionHorizontal(View child, int left, int dx) {
                // 左边界
                int leftBound = getPaddingLeft();
                // 右边界
                int rightBound = getWidth() - getPaddingRight() - child.getWidth();
                int newLeft = Math.min(Math.max(left, leftBound), rightBound);
                return newLeft;
            }

            @Override
            public int clampViewPositionVertical(View child, int top, int dy) {
                // 上边界
                int topBound = getPaddingTop();
                // 下边界
                int bottomBound = getHeight() - getPaddingBottom() - child.getHeight();
                int newTop = Math.min(Math.max(top, topBound), bottomBound);
                return newTop;
            }
        });

    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        return dragHelper.shouldInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        dragHelper.processTouchEvent(event);
        return true;
    }
}
