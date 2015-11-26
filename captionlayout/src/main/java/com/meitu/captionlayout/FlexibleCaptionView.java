package com.meitu.captionlayout;

import java.util.ArrayList;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region;
import android.graphics.Typeface;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.FloatMath;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

/**
 * 字幕控件，支持移动、旋转、缩放、导出字幕信息
 * Created by wrs on 2015/11/18.
 */
public class FlexibleCaptionView extends View {

    /**
     * 输出日志TAG
     */
    private static final String TAG = FlexibleCaptionView.class.getSimpleName();
    /**
     * 支持的最大字体大小，单位像素
     */
    private static final int MAX_TEXT_SIZE = 150;

    private Matrix mUpdateMatrix = new Matrix(); // 变化矩阵，用来获取最新的点

    private Paint mBorderPaint; // 画矩形的笔
    private Region mBorderRegion = new Region();
    private Path mBorderPath = new Path();
    private int mBorderColor = Color.GRAY;

    private PointF mCenterPoint = new PointF(); // 矩形中心坐标

    private PointF mLeftTopPoint = new PointF();
    private PointF mRightTopPoint = new PointF();
    private PointF mLeftBottomPoint = new PointF();
    private PointF mRightBottomPoint = new PointF();

    private RectF mBorderRect = new RectF(); // 初始位置
    private RectF mCurrentLeftTopIconRect = new RectF();
    private RectF mCurrentRightTopIconRect = new RectF();
    private RectF mCurrentRightBottomRect = new RectF();

    private Paint mBitmapPaint;
    private Bitmap mLeftTopBmp, mRightTopBmp, mRightBottomBmp;
    private int mIconSize;

    private TextPaint mTextPaint; // 写字幕的笔
    private String mText = "";
    private float mTextStrokeWidth = 1;
    private float mTextSize = 50f;
    private int mTextColor = Color.BLACK;
    private Typeface mTextTypeFace = Typeface.DEFAULT;
    private ArrayList<OneLineText> mMultiLines = new ArrayList<>();
    private int mOneLineHeight;
    private int mBaselineHeight;

    private TouchRegion mTouchRegion; // 触摸的区域
    private TouchMode mTouchMode = TouchMode.NONE; // 触摸模式
    private boolean mFocus = true;

    private float mTotalScale;
    private float mTotalDegree;
    private boolean mNeverDraw = true;
    private boolean mReInit = true; // 是否需要重新初始化

    public FlexibleCaptionView(Context context) {
        this(context, null);
    }

    public FlexibleCaptionView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FlexibleCaptionView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        // 设置默认属性值
        mIconSize = (int) convertDp2Px(30f);

        // 加载自定义属性
        if (attrs != null) {
            loadAttrs(context, attrs);
        }

        initPaint();
    }

    private void loadAttrs(Context context, AttributeSet attrs) {
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.FlexibleCaptionView);
        int count = typedArray.getIndexCount();
        for (int i = 0; i < count; i++) {
            int attr = typedArray.getIndex(i);
            if (attr == R.styleable.FlexibleCaptionView_text) {
                mText = typedArray.getString(attr);
            } else if (attr == R.styleable.FlexibleCaptionView_textSize) {
                mTextSize = typedArray.getDimensionPixelSize(attr, (int) mTextSize);
            } else if (attr == R.styleable.FlexibleCaptionView_textColor) {
                mTextColor = typedArray.getColor(attr, mTextColor);
            } else if (attr == R.styleable.FlexibleCaptionView_borderColor) {
                mBorderColor = typedArray.getColor(attr, mBorderColor);
            } else if (attr == R.styleable.FlexibleCaptionView_leftTopIcon) {
                mLeftTopBmp = BitmapFactory.decodeResource(getResources(), typedArray.getResourceId(attr, 0));
            } else if (attr == R.styleable.FlexibleCaptionView_rightTopIcon) {
                mRightTopBmp = BitmapFactory.decodeResource(getResources(), typedArray.getResourceId(attr, 0));
            } else if (attr == R.styleable.FlexibleCaptionView_rightBottomIcon) {
                mRightBottomBmp = BitmapFactory.decodeResource(getResources(), typedArray.getResourceId(attr, 0));
            } else if (attr == R.styleable.FlexibleCaptionView_iconSize) {
                mIconSize = typedArray.getDimensionPixelSize(attr, mIconSize);
            }
        }
        typedArray.recycle();
    }

    private float convertDp2Px(float dp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }

    private float convertSp2Px(float sp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, getResources().getDisplayMetrics());
    }

    private void initPaint() {
        // 边框画笔
        mBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mBorderPaint.setStyle(Paint.Style.STROKE);
        mBorderPaint.setStrokeWidth(mTextStrokeWidth);
        mBorderPaint.setColor(mBorderColor);

        // 图标画笔
        mBitmapPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mBitmapPaint.setDither(true);

        // 文字画笔
        mTextPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        mTextPaint.setColor(mTextColor);
        mTextPaint.setStrokeWidth(mTextStrokeWidth);
        mTextPaint.setTextSize(mTextSize);
        mTextPaint.setTypeface(mTextTypeFace);
    }

    /**
     * 设置字体
     * @param typeFace
     */
    public void setTextTypeFace(Typeface typeFace) {
        this.mTextTypeFace = typeFace;
        mTextPaint.setTypeface(mTextTypeFace);
        refresh(true);
    }

    /**
     * @return 获取字体
     */
    public Typeface getTextTypeFace() {
        return mTextTypeFace;
    }

    /**
     * 设置文本内容
     *
     * @param text 文本内容
     */
    public void setText(String text) {
        this.mText = text;
        refresh(true);
    }

    /**
     * @return 获取文本内容
     */
    public String getText() {
        return mText;
    }

    /**
     * @return 获取文本颜色
     */
    public int getTextColor() {
        return mTextColor;
    }

    /**
     * 设置文本颜色
     * @param textColor 颜色值
     */
    public void setTextColor(int textColor) {
        this.mTextColor = textColor;
        mTextPaint.setColor(textColor);
        refresh(false);
    }

    /**
     * @return 获取字体大小
     */
    public float getTextSize() {
        return mTextSize;
    }

    /**
     * 设置字体大小，有最大限制{@link #MAX_TEXT_SIZE}
     *
     * @param textSize 单位px
     */
    public void setTextSizePx(float textSize) {
        if (textSize <= 0) {
            return;
        }
        this.mTextSize = textSize > MAX_TEXT_SIZE ? MAX_TEXT_SIZE : textSize;
        mTextPaint.setTextSize(textSize);
        refresh(true);
    }

    /**
     * 设置字体大小，有最大限制{@link #MAX_TEXT_SIZE}
     *
     * @param textSize 单位dp
     */
    public void setTextSizeDp(float textSize) {
        setTextSizePx(convertDp2Px(textSize));
    }

    /**
     * 设置字体大小，有最大限制{@link #MAX_TEXT_SIZE}
     *
     * @param textSize 单位sp
     */
    public void setTextSizeSp(float textSize) {
        setTextSizePx(convertSp2Px(textSize));
    }

    /**
     * @return 获取边框颜色
     */
    public int getBorderColor() {
        return mBorderColor;
    }

    /**
     * 设置边框颜色
     * @param borderColor 颜色值
     */
    public void setBorderColor(int borderColor) {
        this.mBorderColor = borderColor;
        mBorderPaint.setColor(borderColor);
        refresh(false);
    }

    /**
     * @return 获取图标大小
     */
    public int getIconSize() {
        return mIconSize;
    }

    /**
     * 设置图标大小
     * @param iconSize 单位像素
     */
    public void setIconSizePx(int iconSize) {
        this.mIconSize = iconSize;
        refresh(false);
    }

    /**
     * 设置图标大小
     * @param iconSize 单位dp
     */
    public void setIconSizeDp(int iconSize) {
        setIconSizePx((int) convertDp2Px(iconSize));
    }

    /**
     * 设置左上角删除功能图标
     * @param bitmap 位图对象
     */
    public void setLeftTopIcon(Bitmap bitmap) {
        this.mLeftTopBmp = bitmap;
        refresh(false);
    }

    /**
     * 设置左上角删除功能图标
     * @param id 图片资源id
     */
    public void setLeftTopIcon(int id) {
        setLeftTopIcon(BitmapFactory.decodeResource(getResources(), id));
    }

    /**
     * 设置右上角删除功能图标
     * @param bitmap 位图对象
     */
    public void setRightTopIcon(Bitmap bitmap) {
        this.mRightTopBmp = bitmap;
        refresh(false);
    }

    /**
     * 设置右上角删除功能图标
     * @param id 图片资源id
     */
    public void setRightTopIcon(int id) {
        setRightTopIcon(BitmapFactory.decodeResource(getResources(), id));
    }

    /**
     * 设置右下角删除功能图标
     * @param bitmap 位图对象
     */
    public void setRightBottomIcon(Bitmap bitmap) {
        this.mRightBottomBmp = bitmap;
        refresh(false);
    }

    /**
     * 设置右下角删除功能图标
     * @param id 图片资源id
     */
    public void setRightBottomIcon(int id) {
        setRightBottomIcon(BitmapFactory.decodeResource(getResources(), id));
    }

    /**
     * @return 获取是否为focus状态
     */
    public boolean getFocus() {
        return mFocus;
    }

    /**
     * 设置控件是否为focus状态
     * @param focus true：将显示功能图标和边框；false：隐藏功能图标和边框
     */
    public void setFocus(boolean focus) {
        if (this.mFocus == focus) {
            return;
        }
        this.mFocus = focus;
        refresh(false);
    }

    /**
     * @return 获取字幕信息
     */
    public CaptionInfo getCurrentCaption() {
        return buildCurrentCaptionInfo();
    }

    private CaptionInfo buildCurrentCaptionInfo() {
        // 暂时摆正
        mUpdateMatrix.postRotate(-mTotalDegree, mCenterPoint.x, mCenterPoint.y);
        RectF dst = new RectF();
        mUpdateMatrix.mapRect(dst, mBorderRect);
        Rect locationRect = new Rect((int) dst.left, (int) dst.top, (int) dst.right, (int) dst.bottom);
        // 更新字幕位置
        updateTextBaselineLocationData(mMultiLines, mUpdateMatrix);
        Bitmap textBitmap = Bitmap.createBitmap(locationRect.width(), locationRect.height(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(textBitmap);
        canvas.translate(-locationRect.left, -locationRect.top);
        drawText(canvas, mMultiLines);
        CaptionInfo captionInfo = new CaptionInfo(textBitmap, locationRect, mTotalDegree);
        // 恢复位置
        mUpdateMatrix.postRotate(mTotalDegree, mCenterPoint.x, mCenterPoint.y);
        updateTextBaselineLocationData(mMultiLines, mUpdateMatrix);
        return captionInfo;
    }

    /**
     * 重绘控件
     *
     * @param reInit 内部是否需要重新初始化数据
     */
    private void refresh(boolean reInit) {
        this.mReInit = reInit;
        postInvalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        log("onMeasure");
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        log("onLayout");
        super.onLayout(changed, left, top, right, bottom);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        log("onAttachedToWindow()");
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        log("onDetachedFromWindow()");
    }

    // 更新各位置信息
    private void updateCornerLocationData() {
        updateLeftTopIconRect();
        updateRightTopIconRect();
        updateRightBottomIconRect();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        // log("onDraw");
        if (mNeverDraw || mReInit) {
            mNeverDraw = false;
            mReInit = false;
            init();
        }
        drawText(canvas, mMultiLines);
        if (mFocus) {
            drawBorderRect(canvas);
            drawCornerIcon(canvas, mLeftTopBmp, mRightTopBmp, mRightBottomBmp);
        }
    }

    private void init() {
        log("init");
        mUpdateMatrix.reset();
        mTextPaint.setTextSize(mTextSize);
        mTextPaint.setColor(mTextColor);
        mTextPaint.setStrokeWidth(mTextStrokeWidth);
        mTotalScale = 1;
        mTotalDegree = 0;

        initBorderRect();
        updateBorderVertexData();
        determineTextInitBaselineLocation(mBaselineHeight, mOneLineHeight);
        updateCornerLocationData();
    }

    private void drawBorderRect(Canvas canvas) {
        mBorderPath.reset();
        mBorderPath.moveTo(mLeftTopPoint.x, mLeftTopPoint.y);
        mBorderPath.lineTo(mRightTopPoint.x, mRightTopPoint.y);
        mBorderPath.lineTo(mRightBottomPoint.x, mRightBottomPoint.y);
        mBorderPath.lineTo(mLeftBottomPoint.x, mLeftBottomPoint.y);
        mBorderPath.lineTo(mLeftTopPoint.x, mLeftTopPoint.y);
        canvas.drawPath(mBorderPath, mBorderPaint);
    }

    private void drawText(Canvas canvas, ArrayList<OneLineText> multiLines) {
        Path textPath = new Path();
        for (OneLineText oneLineText : multiLines) {
            textPath.moveTo(oneLineText.drawBaselineStartPoint.x, oneLineText.drawBaselineStartPoint.y);
            textPath.lineTo(oneLineText.drawBaselineEndPoint.x, oneLineText.drawBaselineEndPoint.y);
            canvas.drawTextOnPath(oneLineText.text, textPath, 0, 0, mTextPaint);
            textPath.reset();
        }
    }

    private void drawCornerIcon(Canvas canvas, Bitmap leftTopBitmap, Bitmap rightTopBitmap, Bitmap rightBottomBitmap) {
        if (leftTopBitmap != null) {
            canvas.drawBitmap(leftTopBitmap, null, mCurrentLeftTopIconRect, mBitmapPaint);
        }
        if (rightTopBitmap != null) {
            canvas.drawBitmap(rightTopBitmap, null, mCurrentRightTopIconRect, mBitmapPaint);
        }
        if (rightBottomBitmap != null) {
            canvas.drawBitmap(rightBottomBitmap, null, mCurrentRightBottomRect, mBitmapPaint);
        }
    }

    private void initBorderRect() {
        determineBorderSize();
    }

    private void determineBorderSize() {
        mMultiLines.clear();

        Paint.FontMetricsInt fmi = mTextPaint.getFontMetricsInt(); // 获取字体信息
        log(fmi.toString());
        mOneLineHeight = fmi.bottom - fmi.top;
        mBaselineHeight = -fmi.top;

        float availableWidthSpace = getWidth(); // 每行允许写文字最大空间
        float[] textWidths = new float[mText.length()];
        mTextPaint.getTextWidths(mText, 0, mText.length(), textWidths);
        float totalWidths = 0;
        int lineStartIndex = 0;
        OneLineText firstLine = new OneLineText();
        mMultiLines.add(firstLine);
        float maxLineWidth = 0; // 多行时，最宽一行文字的宽度
        for (int i = 0; i < textWidths.length; i++) {
            totalWidths += textWidths[i];
            if (totalWidths > availableWidthSpace) {
                totalWidths -= textWidths[i];
                if (totalWidths > maxLineWidth) {
                    maxLineWidth = totalWidths;
                }
                // 文字超过一行，记录下该行信息
                mMultiLines.get(mMultiLines.size() - 1).text = mText.substring(lineStartIndex, i);
                // 开启下一行
                OneLineText nextLine = new OneLineText();
                mMultiLines.add(nextLine);
                // 换行
                lineStartIndex = i;
                totalWidths = textWidths[i];
            }
        }
        mMultiLines.get(mMultiLines.size() - 1).text = mText.substring(lineStartIndex, textWidths.length);

        int lineCount = mMultiLines.size();
        // 根据行数来确定矩形框宽高
        float borderWidth = lineCount > 1 ? maxLineWidth : totalWidths;
        float borderHeight = mOneLineHeight * lineCount;
        log("mOneLineHeight=" + mOneLineHeight + ",borderWidth:" + borderWidth + ",borderHeight=" + borderHeight);

        float rectLeft = (getWidth() - borderWidth) / 2;
        float rectRight = rectLeft + borderWidth;
        float rectTop = (getHeight() - borderHeight) / 2;
        float rectBottom = rectTop + borderHeight;
        mBorderRect.set(rectLeft, rectTop, rectRight, rectBottom);
    }

    // 确定文字初始化时baseline的位置
    private void determineTextInitBaselineLocation(int baselineHeight, int lineHeight) {
        OneLineText firstLine = mMultiLines.get(0);
        firstLine.setBaselineStartPoint(mLeftTopPoint.x, mLeftTopPoint.y + baselineHeight);
        firstLine.setBaselineEndPoint(mRightTopPoint.x, mRightTopPoint.y + baselineHeight);
        for (int i = 1; i < mMultiLines.size(); i++) {
            OneLineText curLineText = mMultiLines.get(i);
            OneLineText lastLineText = mMultiLines.get(i - 1);
            curLineText.setBaselineStartPoint(lastLineText.baselineStartPoint.x, lastLineText.baselineStartPoint.y
                + lineHeight);
            curLineText.setBaselineEndPoint(lastLineText.baselineEndPoint.x, lastLineText.baselineEndPoint.y
                + lineHeight);
        }
    }

    private void updateLeftTopIconRect() {
        int halfIconSize = mIconSize / 2;
        float left = mLeftTopPoint.x - halfIconSize;
        float top = mLeftTopPoint.y - halfIconSize;
        float right = mLeftTopPoint.x + halfIconSize;
        float bottom = mLeftTopPoint.y + halfIconSize;
        mCurrentLeftTopIconRect.set(left, top, right, bottom);
    }

    private void updateRightTopIconRect() {
        int halfIconSize = mIconSize / 2;
        float left = mRightTopPoint.x - halfIconSize;
        float top = mRightTopPoint.y - halfIconSize;
        float right = mRightTopPoint.x + halfIconSize;
        float bottom = mRightTopPoint.y + halfIconSize;
        mCurrentRightTopIconRect.set(left, top, right, bottom);
    }

    private void updateRightBottomIconRect() {
        int halfIconSize = mIconSize / 2;
        float left = mRightBottomPoint.x - halfIconSize;
        float top = mRightBottomPoint.y - halfIconSize;
        float right = mRightBottomPoint.x + halfIconSize;
        float bottom = mRightBottomPoint.y + halfIconSize;
        mCurrentRightBottomRect.set(left, top, right, bottom);
    }

    private float lastX, lastY; // 上次点击的坐标
    private float downDistance; // 按下时两指间的距离
    private float pointerDegree; // 按下时两指的旋转角度
    private int alwaysPressedPointId = 0;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (shouldGiveUpTheEvent()) {
            return false;
        }
        boolean consume = true;
        float curX = event.getX();
        float curY = event.getY();
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                consume = determineTouchRegion(curX, curY);
                if (mTouchRegion == TouchRegion.INSIDE) {
                    mTouchMode = TouchMode.DRAG;
                }
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                // 不处理超过两个点的情况
                if (event.getActionIndex() > 1) {
                    break;
                }
                float secondPointX = event.getX(event.getActionIndex());
                float secondPointY = event.getY(event.getActionIndex());
                if (isInBorderRegion(secondPointX, secondPointY)) {
                    downDistance = calculatePointsDistance(event);
                    mTouchMode = TouchMode.POINTER_SCALE_ROTATE;
                    pointerDegree = calculatePointerRotationDegree(event);
                } else {
                    mTouchMode = TouchMode.NONE;
                }
                break;
            case MotionEvent.ACTION_MOVE:
                onMove(event);
                break;
            case MotionEvent.ACTION_POINTER_UP:
                int leavePointerId = event.getActionIndex();
                // 前两个按下的点有效
                if (leavePointerId < 2) {
                    // 前一个按住的点离开，切换点击坐标
                    if (leavePointerId == alwaysPressedPointId) {
                        alwaysPressedPointId = alwaysPressedPointId == 0 ? 1 : 0;
                    }
                    curX = event.getX(alwaysPressedPointId);
                    curY = event.getY(alwaysPressedPointId);
                    if (isInBorderRegion(curX, curY)) {
                        // 留下的点还在区域内，转为拖拽
                        mTouchMode = TouchMode.DRAG;
                    } else {
                        mTouchMode = TouchMode.NONE;
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                mTouchMode = TouchMode.NONE;
                alwaysPressedPointId = 0;
                break;
        }
        lastX = curX;
        lastY = curY;
        return consume;
    }

    // 是否要放弃处理事件
    private boolean shouldGiveUpTheEvent() {
        if (getParent() instanceof CaptionLayout) {
            CaptionLayout captionLayout = (CaptionLayout) getParent();
            // 不处理可能已经由其它控件正在处理的事件
            if (mTouchMode == TouchMode.NONE && captionLayout.isPointerDown) {
                return true;
            }
        }
        return false;
    }

    private void onMove(MotionEvent event) {
        float curX = event.getX();
        float curY = event.getY();
        switch (mTouchRegion) {
            case INSIDE:
                if (mTouchMode == TouchMode.DRAG) {
                    processMove(curX - lastX, curY - lastY);
                } else if (mTouchMode == TouchMode.POINTER_SCALE_ROTATE) {
                    // 双指缩放
                    float curDistance = calculatePointsDistance(event);
                    float scale = curDistance / downDistance;
                    downDistance = curDistance;
                    processScale(scale);
                    // 旋转
                    float curDegree = calculatePointerRotationDegree(event);
                    float degree = curDegree - pointerDegree;
                    pointerDegree = curDegree;
                    processRotate(degree);
                }
                break;
            case RIGHT_BOTTOM_ICON:
                scaleAndRotate(curX, curY);
                break;
        }
    }

    // 取两指间旋转角度
    private float calculatePointerRotationDegree(MotionEvent event) {
        double dx = (event.getX(0) - event.getX(1));
        double dy = (event.getY(0) - event.getY(1));
        double radians = Math.atan2(dy, dx);
        return (float) Math.toDegrees(radians);
    }

    // 计算两个触摸点之间的距离
    private float calculatePointsDistance(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return FloatMath.sqrt(x * x + y * y);
    }

    private void scaleAndRotate(float curX, float curY) {
        float scale = calculateScale(curX, curY);
        processScale(scale);
        float degree = calculateRotationDegree(curX, curY, lastX, lastY);
        processRotate(degree);
    }

    // 计算旋转角度
    private float calculateRotationDegree(float curX, float curY, float lastX, float lastY) {
        // 根据斜率算夹角
        return (float) Math.toDegrees(Math.atan2(curY - mCenterPoint.y, curX - mCenterPoint.x))
            - (float) Math.toDegrees(Math.atan2(lastY - mCenterPoint.y, lastX - mCenterPoint.x));
    }

    private float calculateScale(float curX, float curY) {
        double oldRadius = calculateRadius(lastX, lastY);
        double newRadius = calculateRadius(curX, curY);

        float scale = 1;
        if (oldRadius != 0 && newRadius != 0) {
            scale = (float) (newRadius / oldRadius);
        }
        return scale;
    }

    private double calculateRadius(float x, float y) {
        double dxPower = Math.pow((x - mCenterPoint.x), 2);
        double dyPower = Math.pow((y - mCenterPoint.y), 2);
        return Math.sqrt(dxPower + dyPower);
    }

    // 确定事件点击的区域
    private boolean determineTouchRegion(float curX, float curY) {
        boolean consume = true;
        setFocus(true);
        if (mRightBottomBmp != null && mCurrentRightBottomRect.contains(curX, curY)) {
            mTouchRegion = TouchRegion.RIGHT_BOTTOM_ICON;
        } else if (mLeftTopBmp != null && mCurrentLeftTopIconRect.contains(curX, curY)) {
            mTouchRegion = TouchRegion.LEFT_TOP_ICON;
            ((ViewGroup) getParent()).removeView(this);
        } else if (isInBorderRegion(curX, curY)) {
            // 内容与角落重叠区域相应角落事件
            mTouchRegion = TouchRegion.INSIDE;
        } else {
            mTouchRegion = TouchRegion.OUTSIDE;
            consume = false;
            setFocus(false);
        }
        log(this + "curX=" + curX + ",curY=" + curY + ",mTouchRegion=" + mTouchRegion.name());
        return consume;
    }

    // 判断触摸点是否在边框区域内
    private boolean isInBorderRegion(float curX, float curY) {
        RectF r = new RectF();
        // 计算控制点的边界
        mBorderPath.computeBounds(r, true);
        // 设置区域路径和剪辑描述的区域
        mBorderRegion.setPath(mBorderPath, new Region((int) r.left, (int) r.top, (int) r.right, (int) r.bottom));
        return mBorderRegion.contains((int) curX, (int) curY);
    }

    private void processMove(float dx, float dy) {
        mUpdateMatrix.postTranslate(dx, dy);
        updateLocationDataAndRefresh();
    }

    private void processScale(float scale) {
        if (scale == 1) {
            return;
        }
        // 检查缩放是否超过限定
        float maxScale = getWidth() * 1.0f / mBorderRect.width();
        if (this.mTotalScale * scale > maxScale) {
            scale = maxScale / this.mTotalScale;
        }
        this.mTotalScale *= scale;
        mUpdateMatrix.postScale(scale, scale, mCenterPoint.x, mCenterPoint.y);
        updateTextPaint();
        updateLocationDataAndRefresh();
    }

    private void processRotate(float degree) {
        if (degree == 0) {
            return;
        }
        this.mTotalDegree = (this.mTotalDegree + degree) % 360;
        mUpdateMatrix.postRotate(degree, mCenterPoint.x, mCenterPoint.y);
        updateLocationDataAndRefresh();
    }

    // 字体相关参数改变
    private void updateTextPaint() {
        mTextPaint.setStrokeWidth(mTextStrokeWidth * mTotalScale);
        mTextPaint.setTextSize(mTextSize * mTotalScale);
    }

    // 更新位置信息并重绘控件
    private void updateLocationDataAndRefresh() {
        updateBorderVertexData();
        updateTextBaselineLocationData(mMultiLines, mUpdateMatrix);
        updateCornerLocationData();

        invalidate();
    }

    // 更新边框顶点位置
    private void updateBorderVertexData() {
        // 根据矩阵变化，映射到新的顶点位置
        float[] dst = new float[8];
        mUpdateMatrix.mapPoints(dst, new float[]{mBorderRect.left, mBorderRect.top, mBorderRect.right, mBorderRect.top,
                mBorderRect.left, mBorderRect.bottom, mBorderRect.right, mBorderRect.bottom});
        mLeftTopPoint.x = dst[0];
        mLeftTopPoint.y = dst[1];
        mRightTopPoint.x = dst[2];
        mRightTopPoint.y = dst[3];
        mLeftBottomPoint.x = dst[4];
        mLeftBottomPoint.y = dst[5];
        mRightBottomPoint.x = dst[6];
        mRightBottomPoint.y = dst[7];

        updateCenterPoint();
    }

    private void updateCenterPoint() {
        // 根据对角的点获取中心点
        mCenterPoint.x = (mLeftTopPoint.x + mRightBottomPoint.x) / 2;
        mCenterPoint.y = (mLeftTopPoint.y + mRightBottomPoint.y) / 2;
    }

    private void updateTextBaselineLocationData(ArrayList<OneLineText> multiLines, Matrix updateMatrix) {
        // 根据矩阵变化，映射到新的baseline位置
        for (OneLineText oneLineText : multiLines) {
            float[] dst = new float[4];
            updateMatrix.mapPoints(dst, new float[] {oneLineText.baselineStartPoint.x,
                oneLineText.baselineStartPoint.y, oneLineText.baselineEndPoint.x, oneLineText.baselineEndPoint.y});
            oneLineText.setDrawBaselineStartPoint(dst[0], dst[1]);
            oneLineText.setDrawBaselineEndPoint(dst[2], dst[3]);
        }
    }

    private void log(String text) {
        Log.d(TAG, text);
    }

    /**
     * 一行文字所包含的信息
     */
    private class OneLineText {
        public String text;
        public PointF baselineStartPoint = new PointF();
        public PointF baselineEndPoint = new PointF();
        public PointF drawBaselineStartPoint = new PointF();
        public PointF drawBaselineEndPoint = new PointF();

        public void setBaselineStartPoint(float x, float y) {
            this.baselineStartPoint.x = x;
            this.baselineStartPoint.y = y;
            this.drawBaselineStartPoint.set(baselineStartPoint);
        }

        public void setBaselineEndPoint(float x, float y) {
            this.baselineEndPoint.x = x;
            this.baselineEndPoint.y = y;
            this.drawBaselineEndPoint.set(baselineEndPoint);
        }

        public void setDrawBaselineStartPoint(float x, float y) {
            this.drawBaselineStartPoint.x = x;
            this.drawBaselineStartPoint.y = y;
        }

        public void setDrawBaselineEndPoint(float x, float y) {
            this.drawBaselineEndPoint.x = x;
            this.drawBaselineEndPoint.y = y;
        }
    }

    /**
     * 事件处理类型（TouchRegion为INSIDE的情况）
     */
    enum TouchMode {
        NONE, DRAG, POINTER_SCALE_ROTATE
    }

    /**
     * 触摸事件的区域
     */
    enum TouchRegion {
        LEFT_TOP_ICON, RIGHT_BOTTOM_ICON, INSIDE, OUTSIDE
    }
}
