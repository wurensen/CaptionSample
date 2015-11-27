package com.meitu.captionlayout;

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
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.FloatMath;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

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
    private CharSequence mText = "";
    private float mTextSize = 50f;
    private int mTextColor = Color.BLACK;
    private Typeface mTextTypeface = Typeface.DEFAULT;
    private int mTextBorderWidth; // 文字的边界宽度，和padding一起决定换行宽度
    private int mPaddingLeft, mPaddingRight, mPaddingTop, mPaddingBottom;
    private int mLineBreakWidth; // 文字达到该宽度时换行
    private int mTextBorderHeight;
    private PointF mInitTextStartPoint = new PointF();
    private StaticLayout mTextLayout;

    private TouchRegion mTouchRegion; // 触摸的区域
    private TouchMode mTouchMode = TouchMode.NONE; // 触摸模式
    private boolean mFocus = true;

    private float mTotalScale;
    private float mMaxScale;
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
        mIconSize = (int) convert2px(context, TypedValue.COMPLEX_UNIT_DIP, 30f);

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

    private static float convert2px(Context context, int unit, float value) {
        return TypedValue.applyDimension(unit, value, context.getResources().getDisplayMetrics());
    }

    private static Bitmap loadBitmap(Context context, int id) {
        return BitmapFactory.decodeResource(context.getResources(), id);
    }

    private void initPaint() {
        // 边框画笔
        mBorderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mBorderPaint.setStyle(Paint.Style.STROKE);

        // 图标画笔
        mBitmapPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mBitmapPaint.setDither(true);

        // 文字画笔
        mTextPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    }

    /**
     * 字幕控件构造器，方便创建字幕控件对象
     */
    public static class Builder {

        private Context mContext;
        private CharSequence mText;
        private float mTextSize;
        private int mTextBorderWidth;
        private int mTextColor;
        private Typeface mTextTypeface;
        private int mPaddingLeft, mPaddingRight, mPaddingTop, mPaddingBottom;
        private Bitmap mLeftTopIconBmp, mRightTopIconBmp, mRightBottomIconBmp;

        private Builder(Context context) {
            mContext = context;
            // TODO: 2015/11/27 默认值的设置
        }

        public static Builder create(Context context) {
            if (context == null) {
                throw new NullPointerException("context must not be null");
            }
            return new Builder(context);
        }

        public Builder loadConfigFromEditText(EditText editText) {
            mText = editText.getText();
            mTextSize = editText.getTextSize();
            mTextBorderWidth = editText.getWidth();
            mTextColor = editText.getCurrentTextColor();
            mTextTypeface = editText.getTypeface();

            mPaddingLeft = editText.getPaddingLeft();
            mPaddingRight = editText.getPaddingRight();
            mPaddingTop = editText.getPaddingTop();
            mPaddingBottom = editText.getPaddingBottom();
            return this;
        }

        public Builder text(CharSequence text) {
            mText = text;
            return this;
        }

        public Builder textSize(int unit, float value) {
            mTextSize = convert2px(mContext, unit, value);
            return this;
        }

        public Builder textBorderWidth(int textBorderWidth) {
            mTextBorderWidth = textBorderWidth;
            return this;
        }

        public Builder textColor(int textColor) {
            mTextColor = textColor;
            return this;
        }

        public Builder textTypeface(Typeface textTypeface) {
            mTextTypeface = textTypeface;
            return this;
        }

        public Builder padding(int unit, int value) {
            mPaddingLeft = mPaddingRight = mPaddingTop = mPaddingBottom = (int) convert2px(mContext, unit, value);
            return this;
        }

        public Builder paddingLeft(int unit, int value) {
            mPaddingLeft = (int) convert2px(mContext, unit, value);
            return this;
        }

        public Builder paddingRight(int unit, int value) {
            mPaddingRight = (int) convert2px(mContext, unit, value);
            return this;
        }

        public Builder paddingTop(int unit, int value) {
            mPaddingTop = (int) convert2px(mContext, unit, value);
            return this;
        }

        public Builder paddingBottom(int unit, int value) {
            mPaddingBottom = (int) convert2px(mContext, unit, value);
            return this;
        }

        public Builder icon(int leftTopIconId, int rightTopIconId, int rightBottomIconId) {
            return icon(loadBitmap(mContext, leftTopIconId), loadBitmap(mContext, rightTopIconId),
                loadBitmap(mContext, rightBottomIconId));
        }

        public Builder icon(Bitmap leftTopIconBmp, Bitmap rightTopIconBmp, Bitmap rightBottomIconBmp) {
            mLeftTopIconBmp = leftTopIconBmp;
            mRightTopIconBmp = rightTopIconBmp;
            mRightBottomIconBmp = rightBottomIconBmp;
            return this;
        }

        public FlexibleCaptionView build() {
            FlexibleCaptionView captionView = new FlexibleCaptionView(mContext);
            captionView.mText = mText;
            captionView.mTextSize = mTextSize;
            captionView.mTextBorderWidth = mTextBorderWidth;
            captionView.mTextColor = mTextColor;
            captionView.mTextTypeface = mTextTypeface;

            captionView.mPaddingLeft = mPaddingLeft;
            captionView.mPaddingRight = mPaddingRight;
            captionView.mPaddingTop = mPaddingTop;
            captionView.mPaddingBottom = mPaddingBottom;

            captionView.mLeftTopBmp = mLeftTopIconBmp;
            captionView.mRightTopBmp = mRightTopIconBmp;
            captionView.mRightBottomBmp = mRightBottomIconBmp;
            captionView.refresh(true);
            return captionView;
        }

    }

    /**
     * 设置字体
     *
     * @param typeface 字体
     */
    public void setTextTypeface(Typeface typeface) {
        this.mTextTypeface = typeface;
        mTextPaint.setTypeface(mTextTypeface);
        refresh(true);
    }

    /**
     * @return 获取字体
     */
    public Typeface getTextTypeface() {
        return mTextTypeface;
    }

    /**
     * 设置文本内容
     *
     * @param text 文本内容
     */
    public void setText(CharSequence text) {
        this.mText = text;
        refresh(true);
    }

    /**
     * @return 获取文本内容
     */
    public CharSequence getText() {
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
     *
     * @param textColor 颜色值
     */
    public void setTextColor(int textColor) {
        this.mTextColor = textColor;
        mTextPaint.setColor(textColor);
        refresh(false);
    }

    /**
     * @return 获取字体大小，单位px
     */
    public float getTextSize() {
        return mTextSize;
    }

    /**
     * 设置字体大小，有最大限制{@link #MAX_TEXT_SIZE}
     *
     * @param unit 单位
     * @param value 指
     */
    public void setTextSize(int unit, float value) {
        if (value <= 0) {
            log("mTextSize must>0");
            return;
        }
        float textSize = convert2px(getContext(), unit, value);
        this.mTextSize = textSize > MAX_TEXT_SIZE ? MAX_TEXT_SIZE : textSize;
        mTextPaint.setTextSize(textSize);
        refresh(true);
    }

    /**
     * 设置边框宽度，会影响内容换行
     * @param mTextBorderWidth 单位px
     */
    public void setTextBorderWidth(int mTextBorderWidth) {
        this.mTextBorderWidth = mTextBorderWidth;
        refresh(true);
    }

    /**
     * 设置文字与边框的间隔，会影响内容换行
     * @param unit 单位
     * @param value 值
     */
    public void setPadding(int unit, float value) {
        mPaddingLeft = mPaddingRight = mPaddingTop = mPaddingBottom = (int) convert2px(getContext(), unit, value);
        refresh(true);
    }

    /**
     * @return 获取边框颜色
     */
    public int getBorderColor() {
        return mBorderColor;
    }

    /**
     * 设置边框颜色
     *
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
     *
     * @param iconSize 单位像素
     */
    public void setIconSizePx(int iconSize) {
        this.mIconSize = iconSize;
        refresh(false);
    }

    /**
     * 设置图标大小
     *
     * @param iconSize 单位dp
     */
    public void setIconSizeDp(int iconSize) {
        setIconSizePx((int) convert2px(getContext(), TypedValue.COMPLEX_UNIT_DIP, iconSize));
    }

    /**
     * 设置左上角删除功能图标
     *
     * @param bitmap 位图对象
     */
    public void setLeftTopIcon(Bitmap bitmap) {
        this.mLeftTopBmp = bitmap;
        refresh(false);
    }

    /**
     * 设置左上角删除功能图标
     *
     * @param id 图片资源id
     */
    public void setLeftTopIcon(int id) {
        setLeftTopIcon(BitmapFactory.decodeResource(getResources(), id));
    }

    /**
     * 设置右上角删除功能图标
     *
     * @param bitmap 位图对象
     */
    public void setRightTopIcon(Bitmap bitmap) {
        this.mRightTopBmp = bitmap;
        refresh(false);
    }

    /**
     * 设置右上角删除功能图标
     *
     * @param id 图片资源id
     */
    public void setRightTopIcon(int id) {
        setRightTopIcon(BitmapFactory.decodeResource(getResources(), id));
    }

    /**
     * 设置右下角删除功能图标
     *
     * @param bitmap 位图对象
     */
    public void setRightBottomIcon(Bitmap bitmap) {
        this.mRightBottomBmp = bitmap;
        refresh(false);
    }

    /**
     * 设置右下角删除功能图标
     *
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
     *
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
        // 更新摆正后要写字的起始点
        float[] curTextStartPoint = new float[2];
        mUpdateMatrix.mapPoints(curTextStartPoint, new float[] {mInitTextStartPoint.x, mInitTextStartPoint.y});
        // 恢复位置
        mUpdateMatrix.postRotate(mTotalDegree, mCenterPoint.x, mCenterPoint.y);
        // 创建字幕图层
        Bitmap textBitmap = Bitmap.createBitmap(locationRect.width(), locationRect.height(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(textBitmap);
        canvas.translate(mPaddingLeft * mTotalScale, mPaddingTop * mTotalScale);
        int breakWidth = (int) Math.ceil(mLineBreakWidth * mTotalScale);
        mTextLayout = new StaticLayout(mText, mTextPaint, breakWidth, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0f, false);
        mTextLayout.draw(canvas);
        return new CaptionInfo(textBitmap, locationRect, mTotalDegree);
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
        drawText(canvas);
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
        mTextPaint.setTypeface(mTextTypeface);

        mBorderPaint.setColor(mBorderColor);

        mTotalScale = 1f;
        mTotalDegree = 0f;

        initBorderRect();
        updateBorderVertexData();
        updateCornerLocationData();
    }

    private void drawText(Canvas canvas) {
        // 摆正
        mUpdateMatrix.postRotate(-mTotalDegree, mCenterPoint.x, mCenterPoint.y);
        // 更新摆正后要写字的起始点
        float[] dst = new float[2];
        mUpdateMatrix.mapPoints(dst, new float[] {mInitTextStartPoint.x, mInitTextStartPoint.y});
        canvas.save();
        // 旋转画布
        canvas.rotate(mTotalDegree, mCenterPoint.x, mCenterPoint.y);
        canvas.translate(dst[0], dst[1]);
        int breakWidth = (int) Math.ceil(mLineBreakWidth * mTotalScale);
        mTextLayout = new StaticLayout(mText, mTextPaint, breakWidth, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0f, false);
        mTextLayout.draw(canvas);
        canvas.restore();
        // 恢复矩阵的改变
        mUpdateMatrix.postRotate(mTotalDegree, mCenterPoint.x, mCenterPoint.y);
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

        log("mTextBorderWidth=" + mTextBorderWidth + ",measureText=" + mTextPaint.measureText(mText, 0, mText.length()));
        mLineBreakWidth = mTextBorderWidth - mPaddingLeft - mPaddingRight;
        mTextLayout =
            new StaticLayout(mText, mTextPaint, mLineBreakWidth, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0f, false);
        mTextBorderHeight = mTextLayout.getHeight() + mPaddingTop + mPaddingBottom;
        log("mLineBreakWidth=" + mLineBreakWidth);
        // 边界检查，检查外框是否超出控件
        if (checkBounds()) {
            float rectLeft = (getWidth() - mTextBorderWidth) / 2;
            float rectRight = rectLeft + mTextBorderWidth;
            float rectTop = (getHeight() - mTextBorderHeight) / 2;
            float rectBottom = rectTop + mTextBorderHeight;
            mBorderRect.set(rectLeft, rectTop, rectRight, rectBottom);
            mInitTextStartPoint.set(rectLeft + mPaddingLeft, rectTop + mPaddingTop);
        }
    }

    private boolean checkBounds() {
        float scale = Math.min(getWidth() * 1.0f / mTextBorderWidth, getHeight() * 1.0f / mTextBorderHeight);
        if (mTextBorderWidth > getWidth() || mTextBorderHeight > getHeight()) {
            // 超出边界，调整边框和文本大小
            resizeTextAndBorder(scale);
            initBorderRect();
            mMaxScale = 1f;
            return false;
        }
        mMaxScale = scale;
        return true;
    }

    private void resizeTextAndBorder(float scale) {
        mTextSize *= scale;
        mTextBorderWidth *= scale;
        mPaddingLeft *= scale;
        mPaddingRight *= scale;
        mPaddingTop *= scale;
        mPaddingBottom *= scale;
        mTextPaint.setTextSize(mTextSize);
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
        log("determineTouchRegion,curX=" + curX + ",curY=" + curY + ",mTouchRegion=" + mTouchRegion.name());
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
        if (this.mTotalScale * scale > mMaxScale) {
            scale = mMaxScale / this.mTotalScale;
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
        mTextPaint.setTextSize(mTextSize * mTotalScale);
    }

    // 更新位置信息并重绘控件
    private void updateLocationDataAndRefresh() {
        updateBorderVertexData();
        updateCornerLocationData();

        invalidate();
    }

    // 更新边框顶点位置
    private void updateBorderVertexData() {
        // 根据矩阵变化，映射到新的顶点位置
        float[] dst = new float[8];
        mUpdateMatrix.mapPoints(dst, new float[] {mBorderRect.left, mBorderRect.top, mBorderRect.right,
            mBorderRect.top, mBorderRect.left, mBorderRect.bottom, mBorderRect.right, mBorderRect.bottom});
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

    private void log(String text) {
        Log.d(TAG, text);
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
