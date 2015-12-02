package com.meitu.captionlayout;

import java.util.ArrayList;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

/**
 * 字幕控件布局，提供多个字幕控件的控制，增加，移除，上下层关系的改变
 *
 * Created by meitu on 2015/11/17.
 */
public class CaptionLayout extends FrameLayout {

    protected boolean mIsPointerDown; // 标记为多点按下
    private OnCaptionFocusChangeListener mOnCaptionFocusChangeListener;
    private FlexibleCaptionView mLastFocusCaptionView;
    private FlexibleCaptionView mCurFocusCaptionView;
    private boolean mFocusChanged, mHasSetLastFocus, mHasSetCurFocus;

    public CaptionLayout(Context context) {
        this(context, null);
    }

    public CaptionLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CaptionLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public OnCaptionFocusChangeListener getOnCaptionFocusChangeListener() {
        return mOnCaptionFocusChangeListener;
    }

    public void setOnCaptionFocusChangeListener(OnCaptionFocusChangeListener onCaptionFocusChangeListener) {
        this.mOnCaptionFocusChangeListener = onCaptionFocusChangeListener;
    }

    protected void updateCaptionFocusChangeView(FlexibleCaptionView captionView, boolean focus) {
        if (focus) {
            mFocusChanged = true;
            mLastFocusCaptionView = mCurFocusCaptionView;
            mCurFocusCaptionView = captionView;
            mHasSetLastFocus = true;
            mHasSetCurFocus = true;
            if (mLastFocusCaptionView != null) {
                mLastFocusCaptionView.setFocus(false);
            }
        } else {
            mFocusChanged = true;
            mLastFocusCaptionView = captionView;
            mHasSetLastFocus = true;
        }
    }

    @Override
    public void addView(View child, int index, ViewGroup.LayoutParams params) {
        super.addView(child, index, params);
        if (child instanceof FlexibleCaptionView) {
            FlexibleCaptionView captionView = (FlexibleCaptionView) child;
            if (captionView.getFocus()) {
                mLastFocusCaptionView = mCurFocusCaptionView;
                mCurFocusCaptionView = captionView;
                mHasSetLastFocus = true;
                mHasSetCurFocus = true;
                if (mLastFocusCaptionView != null) {
                    mLastFocusCaptionView.setFocus(false);
                }
                mFocusChanged = true;
                performCaptionFocusChange();
            }
        }
    }

    @Override
    public void removeView(View view) {
        super.removeView(view);
        if (view == mCurFocusCaptionView) {
            mCurFocusCaptionView = null;
            mHasSetCurFocus = true;
            mFocusChanged = true;
            performCaptionFocusChange();
        } else if (view == mLastFocusCaptionView) {
            mLastFocusCaptionView = null;
            mHasSetLastFocus = true;
        }
    }

    private void performCaptionFocusChange() {
        if (mFocusChanged) {
            if (!mHasSetLastFocus) {
                mLastFocusCaptionView = null;
            }
            if (!mHasSetCurFocus) {
                mCurFocusCaptionView = null;
            }
            mHasSetLastFocus = false;
            mHasSetCurFocus = false;
            mFocusChanged = false;
            if (mOnCaptionFocusChangeListener != null) {
                mOnCaptionFocusChangeListener.onCaptionFocusChange(this, mLastFocusCaptionView, mCurFocusCaptionView);
            }
        }
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
                captionInfos.add(captionView.getCurrentCaption(1));
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

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        // 记录是否为多指按下
        mIsPointerDown = (ev.getAction() & MotionEvent.ACTION_MASK) == MotionEvent.ACTION_POINTER_DOWN;

        boolean consume = super.dispatchTouchEvent(ev);
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            performCaptionFocusChange();
        }
        return consume;
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

    /**
     * 字幕间焦点变更监听器
     */
    public interface OnCaptionFocusChangeListener {
        /**
         * 字幕控件焦点发生改变时触发
         * @param captionLayout 当前字幕容器布局
         * @param lastFocusCaptionView 上一个拥有焦点的字幕控件，可能为null
         * @param curFocusCaptionView 当前拥有焦点的字幕控件，null表示当前没有选中的字幕
         */
        void onCaptionFocusChange(CaptionLayout captionLayout, FlexibleCaptionView lastFocusCaptionView,
            FlexibleCaptionView curFocusCaptionView);
    }
}
