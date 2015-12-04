package com.meitu.captionlayout;

import java.util.ArrayList;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

/**
 * 字幕控件布局，提供多个字幕控件的控制，增加，移除
 *
 * Created by wrs on 2015/11/17.
 */
public class CaptionLayout extends FrameLayout {

    protected boolean mIsPointerDown; // 标记为多点按下
    private OnCaptionFocusChangeListener mOnCaptionFocusChangeListener;
    private FlexibleCaptionView mLastFocusCaptionView;
    private FlexibleCaptionView mCurFocusCaptionView;
    private ArrayList<FlexibleCaptionView> captionViews;
    private int mNeedFocusIndex = -1;

    public CaptionLayout(Context context) {
        this(context, null);
    }

    public CaptionLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CaptionLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        captionViews = new ArrayList<>();
    }

    public OnCaptionFocusChangeListener getOnCaptionFocusChangeListener() {
        return mOnCaptionFocusChangeListener;
    }

    public void setOnCaptionFocusChangeListener(OnCaptionFocusChangeListener onCaptionFocusChangeListener) {
        this.mOnCaptionFocusChangeListener = onCaptionFocusChangeListener;
    }

    @Override
    public void addView(View child, int index, ViewGroup.LayoutParams params) {
        super.addView(child, index, params);
        if (child instanceof FlexibleCaptionView) {
            FlexibleCaptionView captionView = (FlexibleCaptionView) child;
            captionViews.add(captionView);
            if (captionView.getFocus()) {
                mNeedFocusIndex = captionViews.indexOf(captionView);
                performCaptionFocusChange();
            }
        }
    }

    @Override
    public void removeView(View view) {
        super.removeView(view);
        if (view instanceof FlexibleCaptionView) {
            FlexibleCaptionView captionView = (FlexibleCaptionView) view;
            captionViews.remove(captionView);
            if (view == mCurFocusCaptionView) {
                // 当前选中字幕被移除，无选中
                mCurFocusCaptionView = null;
                mLastFocusCaptionView = null;
            } else if (view == mLastFocusCaptionView) {
                mLastFocusCaptionView = null;
            }
        }
    }

    protected void markChildFocus(FlexibleCaptionView view) {
        // 标记选中对象变更
        mNeedFocusIndex = captionViews.indexOf(view);
    }

    // 字幕控件focus状态被非点击事件改变，即被主动设置
    protected void onChildFocusChangeWithoutTouchEvent(FlexibleCaptionView view, boolean focus) {
        if (focus) {
            // -> focus
            mNeedFocusIndex = captionViews.indexOf(view);
            performCaptionFocusChange();
        } else {
            // focus ->
            mCurFocusCaptionView = null;
            // 被主动清除焦点，上次焦点置为本身
            mLastFocusCaptionView = view;
            notifyCaptionFocusChange();
        }
    }

    private void performCaptionFocusChange() {
        // 当前无选中
        if (mCurFocusCaptionView == null) {
            if (mNeedFocusIndex >= 0) {
                mCurFocusCaptionView = captionViews.get(mNeedFocusIndex);
                mCurFocusCaptionView.setFocusFromParent(true);
                notifyCaptionFocusChange();
            }
        } else {
            // 当前有选中，且选中发生了改变
            if (mNeedFocusIndex >= 0 && mCurFocusCaptionView != captionViews.get(mNeedFocusIndex)) {
                mLastFocusCaptionView = mCurFocusCaptionView;
                mCurFocusCaptionView = captionViews.get(mNeedFocusIndex);
                mLastFocusCaptionView.setFocusFromParent(false);
                mCurFocusCaptionView.setFocus(true);
                notifyCaptionFocusChange();
            }
        }
        mNeedFocusIndex = -1;
    }

    private void notifyCaptionFocusChange() {
        // 被主动操作导致的两次焦点一致，清除上一次
        if (mLastFocusCaptionView == mCurFocusCaptionView) {
            mLastFocusCaptionView = null;
        }
        if (mOnCaptionFocusChangeListener != null) {
            mOnCaptionFocusChangeListener.onCaptionFocusChange(this, mLastFocusCaptionView, mCurFocusCaptionView);
        }
    }

    /**
     * 新增字幕控件
     * @param captionView 字幕控件
     */
    public void addCaptionView(FlexibleCaptionView captionView) {
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
     * @param scale 导出的目标相对于字幕的倍数
     */
    public ArrayList<CaptionInfo> findAllCaptionInfos(float scale) {
        ArrayList<CaptionInfo> captionInfos = new ArrayList<>();
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            if (child instanceof FlexibleCaptionView) {
                FlexibleCaptionView captionView = (FlexibleCaptionView) child;
                captionInfos.add(captionView.exportCaptionInfo(scale));
            }
        }
        return captionInfos;
    }

    /**
     * @return 获取当前操作的字幕控件
     */
    public FlexibleCaptionView getCurrentFocusCaptionView() {
        return mCurFocusCaptionView;
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
