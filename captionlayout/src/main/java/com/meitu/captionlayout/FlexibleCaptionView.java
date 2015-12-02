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
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.EditText;

/**
 * 字幕控件，文字字幕支持移动、旋转、缩放、导出、导入字幕信息；贴图字幕只支持移动、导出、导入字幕信息
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
    private static final int MAX_TEXT_SIZE = 200;
    /**
     * 水平或竖直方向的偏移角度范围
     */
    private static final float OFFSET_DEGREE = 10f;
    /**
     * 点击事件触发的超时时间
     */
    private static final int CLICK_TIMEOUT = ViewConfiguration.getTapTimeout();
    /**
     * 当未设备边框宽度时，采用默认的边框与控件的间距
     */
    private static final int DEFAULT_BORDER_MARGIN = 10;

    private boolean debug;

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
    private float mTextSize;
    private int mTextColor;
    private Typeface mTextTypeface = Typeface.DEFAULT;
    private int mTextBorderWidth; // 文字的边界宽度，和padding一起决定换行宽度
    private int mPaddingLeft, mPaddingRight, mPaddingTop, mPaddingBottom;
    private int mTextBorderHeight;
    private Layout.Alignment mLayoutTextAlignment = Layout.Alignment.ALIGN_CENTER;
    private PointF mInitTextStartPoint = new PointF();
    private float[] mUpdateTextStartPoint = new float[2];
    private StaticLayout mTextLayout;

    private Bitmap mImgCaptionBitmap; // 贴图字幕
    private boolean mIsImgCaption;
    private boolean mBlockRotateScaleEvent; // 贴图字幕拦截旋转缩放功能

    private TouchRegion mTouchRegion; // 触摸的区域
    private TouchMode mTouchMode = TouchMode.NONE; // 触摸模式
    private boolean mFocus = true;

    private float mTotalScale;
    private float mMaxScale;
    private float mTotalDegree;
    private boolean mFirstDraw = true;
    private boolean mResetData = true; // 是否需要重置数据
    private boolean mUpdateCurrent = true; // 是否基于当前的位置更新
    private boolean mIsImportCaption = false; // 是否为导入的字幕

    private Matrix mExportMatrix = new Matrix(); // 用于导出字幕的矩阵

    private OnCaptionClickListener mOnCaptionClickListener;
    private boolean mJustGetFocus; // 刚获取焦点

    public FlexibleCaptionView(Context context) {
        this(context, null);
    }

    public FlexibleCaptionView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FlexibleCaptionView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        // 加载自定义属性
        loadAttrs(context, attrs);

        initPaint();
    }

    private void loadAttrs(Context context, AttributeSet attrs) {
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.FlexibleCaptionView);
        mText = typedArray.getString(R.styleable.FlexibleCaptionView_text);
        if (mText == null) {
            mText = "";
        }
        mTextSize =
                typedArray.getDimensionPixelSize(R.styleable.FlexibleCaptionView_textSize,
                        (int) convert2px(context, TypedValue.COMPLEX_UNIT_DIP, 20f));
        mTextColor = typedArray.getColor(R.styleable.FlexibleCaptionView_textColor, Color.BLACK);

        int padding = typedArray.getDimensionPixelSize(R.styleable.FlexibleCaptionView_textPadding, 0);
        mPaddingLeft = mPaddingRight = mPaddingTop = mPaddingBottom = padding;
        mPaddingLeft = typedArray.getDimensionPixelSize(R.styleable.FlexibleCaptionView_textPaddingLeft, mPaddingLeft);
        mPaddingRight =
                typedArray.getDimensionPixelSize(R.styleable.FlexibleCaptionView_textPaddingRight, mPaddingRight);
        mPaddingTop = typedArray.getDimensionPixelSize(R.styleable.FlexibleCaptionView_textPaddingTop, mPaddingTop);
        mPaddingBottom =
                typedArray.getDimensionPixelSize(R.styleable.FlexibleCaptionView_textPaddingBottom, mPaddingBottom);

        // 未设定边框宽度且未设定文本内容，后续绘制将会以父控件的宽度为参考作为边框宽度
        int defaultBorderWidth =
                mText.length() == 0 ? 0 : (int) (mTextSize * mText.length() + mPaddingLeft + mPaddingRight);
        mTextBorderWidth =
                typedArray.getDimensionPixelSize(R.styleable.FlexibleCaptionView_textBorderWidth, defaultBorderWidth);

        mBorderColor = typedArray.getColor(R.styleable.FlexibleCaptionView_borderColor, Color.GRAY);

        mLeftTopBmp =
                BitmapFactory.decodeResource(getResources(),
                        typedArray.getResourceId(R.styleable.FlexibleCaptionView_leftTopIcon, 0));
        mRightTopBmp =
                BitmapFactory.decodeResource(getResources(),
                        typedArray.getResourceId(R.styleable.FlexibleCaptionView_rightTopIcon, 0));
        mRightBottomBmp =
                BitmapFactory.decodeResource(getResources(),
                        typedArray.getResourceId(R.styleable.FlexibleCaptionView_rightBottomIcon, 0));
        mIconSize =
                typedArray.getDimensionPixelSize(R.styleable.FlexibleCaptionView_iconSize,
                        (int) convert2px(context, TypedValue.COMPLEX_UNIT_DIP, 30f));
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
        private Float mTextSize;
        private Integer mTextColor;
        private Integer mTextBorderWidth;
        private Integer mTextBorderColor;
        private Typeface mTextTypeface;
        private Layout.Alignment mLayoutTextAlignment;
        private Integer mPaddingLeft, mPaddingRight, mPaddingTop, mPaddingBottom;
        private Bitmap mLeftTopIconBmp, mRightTopIconBmp, mRightBottomIconBmp;
        private Integer mIconSize;
        private Matrix mUpdateMatrix;
        private float mDegree;
        private float mScale;
        private Bitmap mImgCaptionBitmap;

        private Builder(Context context) {
            mContext = context;
        }

        public static Builder create(Context context) {
            if (context == null) {
                throw new NullPointerException("context must not be null");
            }
            return new Builder(context);
        }

        public Builder imgCaption(int id) {
            return imgCaption(loadBitmap(mContext, id));
        }

        public Builder imgCaption(Bitmap imgCaptionBitmap) {
            mImgCaptionBitmap = imgCaptionBitmap;
            return this;
        }

        public Builder loadConfig(EditText editText) {
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

        public Builder loadConfig(CaptionInfo captionInfo) {
            if (captionInfo instanceof TextCaptionInfo) {
                TextCaptionInfo textCaption = (TextCaptionInfo) captionInfo;
                mText = textCaption.text;
                mTextSize = textCaption.textSize;
                mTextBorderWidth = textCaption.textBorderWidth;
                mTextBorderColor = textCaption.textBorderColor;
                mTextColor = textCaption.textColor;
                mTextTypeface = textCaption.textTypeface;

                mPaddingLeft = textCaption.paddingLeft;
                mPaddingRight = textCaption.paddingRight;
                mPaddingTop = textCaption.paddingTop;
                mPaddingBottom = textCaption.paddingBottom;

                mUpdateMatrix = captionInfo.updateMatrix;
                mDegree = captionInfo.degree;
                mScale = textCaption.scale;
            } else if (captionInfo instanceof ImageCaptionInfo) {
                ImageCaptionInfo imageCaption = (ImageCaptionInfo) captionInfo;
                mImgCaptionBitmap = imageCaption.intrinsicBitmap;
                mUpdateMatrix = imageCaption.updateMatrix;
            }
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

        public Builder textColor(int textColor) {
            mTextColor = textColor;
            return this;
        }

        public Builder textBorderWidth(int textBorderWidth) {
            mTextBorderWidth = textBorderWidth;
            return this;
        }

        public Builder textBorderColor(int textBorderColor) {
            mTextBorderColor = textBorderColor;
            return this;
        }

        public Builder textTypeface(Typeface textTypeface) {
            mTextTypeface = textTypeface;
            return this;
        }

        public Builder layoutTextAlignment(Layout.Alignment textAlignment) {
            mLayoutTextAlignment = textAlignment;
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

        public Builder iconSize(int unit, int value) {
            mIconSize = (int) convert2px(mContext, unit, value);
            return this;
        }

        public FlexibleCaptionView build() {
            FlexibleCaptionView view = new FlexibleCaptionView(mContext);
            view.mText = mText == null ? view.mText : mText;
            view.mTextSize = mTextSize == null ? view.mTextSize : mTextSize;
            view.mTextColor = mTextColor == null ? view.mTextColor : mTextColor;
            view.mBorderColor = mTextBorderColor == null ? view.mBorderColor : mTextBorderColor;
            view.mTextTypeface = mTextTypeface == null ? view.mTextTypeface : mTextTypeface;
            view.mLayoutTextAlignment = mLayoutTextAlignment == null ? view.mLayoutTextAlignment : mLayoutTextAlignment;

            view.mPaddingLeft = mPaddingLeft == null ? view.mPaddingLeft : mPaddingLeft;
            view.mPaddingRight = mPaddingRight == null ? view.mPaddingRight : mPaddingRight;
            view.mPaddingTop = mPaddingTop == null ? view.mPaddingTop : mPaddingTop;
            view.mPaddingBottom = mPaddingBottom == null ? view.mPaddingBottom : mPaddingBottom;

            int defaultBorderWidth =
                    view.mText.length() == 0 ? 0
                            : (int) (view.mText.length() * view.mTextSize + view.mPaddingLeft + view.mPaddingRight);
            view.mTextBorderWidth = mTextBorderWidth == null ? defaultBorderWidth : mTextBorderWidth;

            view.mLeftTopBmp = mLeftTopIconBmp;
            view.mRightTopBmp = mRightTopIconBmp;
            view.mRightBottomBmp = mRightBottomIconBmp;
            view.mIconSize = mIconSize == null ? view.mIconSize : mIconSize;

            view.mImgCaptionBitmap = mImgCaptionBitmap;

            if (mUpdateMatrix != null) {
                view.mIsImportCaption = true;
                view.mUpdateMatrix.set(mUpdateMatrix);
                view.mTotalDegree = mDegree;
                view.mTotalScale = mScale;
                if (mImgCaptionBitmap != null) {
                    view.mIsImgCaption = true;
                    view.mImgCaptionBitmap = mImgCaptionBitmap;
                }
            }
            view.refresh(true, true);
            return view;
        }
    }

    public OnCaptionClickListener getOnCaptionClickListener() {
        return mOnCaptionClickListener;
    }

    public void setOnCaptionClickListener(OnCaptionClickListener mCaptionClickListener) {
        this.mOnCaptionClickListener = mCaptionClickListener;
    }

    /**
     * @return 获取是否为调试模式
     */
    public boolean isDebug() {
        return debug;
    }

    /**
     * 设置是否为调试模式
     *
     * @param debug 是否调试
     */
    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    /**
     * @return 获取字幕内容对齐方式
     */
    public Layout.Alignment getLayoutTextAlignment() {
        return mLayoutTextAlignment;
    }

    /**
     * 设置字幕对齐方式
     *
     * @param textAlignment 对齐方式
     */
    public void setLayoutTextAlignment(Layout.Alignment textAlignment) {
        if (textAlignment != null) {
            this.mLayoutTextAlignment = textAlignment;
        }
        refresh(false, true);
    }

    /**
     * 设置字体
     *
     * @param typeface 字体
     */
    public void setTextTypeface(Typeface typeface) {
        this.mTextTypeface = typeface;
        mTextPaint.setTypeface(mTextTypeface);
        refresh(false, true);
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
        refresh(false, true);
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
        refresh(false, false);
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
     * @param unit  单位
     * @param value 指
     */
    public void setTextSize(int unit, float value) {
        if (value <= 0) {
            log("mTextSize must be > 0");
            return;
        }
        float textSize = convert2px(getContext(), unit, value);
        this.mTextSize = textSize > MAX_TEXT_SIZE ? MAX_TEXT_SIZE : textSize;
        mTextPaint.setTextSize(textSize);
        refresh(true, true);
    }

    /**
     * @return 获取文字边框宽度
     */
    public int getTextBorderWidth() {
        return mTextBorderWidth;
    }

    /**
     * 设置边框宽度，会影响内容换行
     *
     * @param mTextBorderWidth 单位px
     */
    public void setTextBorderWidth(int mTextBorderWidth) {
        this.mTextBorderWidth = mTextBorderWidth;
        refresh(true, true);
    }

    /**
     * 设置文字与边框的间隔，会影响内容换行
     *
     * @param unit  单位
     * @param value 值
     */
    public void setPadding(int unit, float value) {
        mPaddingLeft = mPaddingRight = mPaddingTop = mPaddingBottom = (int) convert2px(getContext(), unit, value);
        refresh(true, true);
    }

    /**
     * @return 获取边框与文字左边间距，单位px
     */
    public int getPaddingLeft() {
        return mPaddingLeft;
    }

    /**
     * 设置边框与文字左边间距，会影响内容换行
     *
     * @param mPaddingLeft 间距，单位px
     */
    public void setPaddingLeft(int mPaddingLeft) {
        this.mPaddingLeft = mPaddingLeft;
        refresh(true, true);
    }

    /**
     * @return 获取边框与文字右边间距，单位px
     */
    public int getPaddingRight() {
        return mPaddingRight;
    }

    /**
     * 设置边框与文字右边间距，会影响内容换行
     *
     * @param mPaddingRight 间距，单位px
     */
    public void setPaddingRight(int mPaddingRight) {
        this.mPaddingRight = mPaddingRight;
        refresh(true, true);
    }

    /**
     * @return 获取边框与上字左边间距，单位px
     */
    public int getPaddingTop() {
        return mPaddingTop;
    }

    /**
     * 设置边框与文字上边间距
     *
     * @param mPaddingTop 间距，单位px
     */
    public void setPaddingTop(int mPaddingTop) {
        this.mPaddingTop = mPaddingTop;
        refresh(true, true);
    }

    /**
     * @return 获取边框与文字下边间距，单位px
     */
    public int getPaddingBottom() {
        return mPaddingBottom;
    }

    /**
     * 设置边框与文字下边间距
     *
     * @param mPaddingBottom 间距，单位px
     */
    public void setPaddingBottom(int mPaddingBottom) {
        this.mPaddingBottom = mPaddingBottom;
        refresh(true, true);
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
        refresh(false, false);
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
        refresh(false, true);
    }

    /**
     * 设置图标大小
     *
     * @param iconSize 单位dp
     */
    public void setIconSizeDp(int iconSize) {
        setIconSizePx((int) convert2px(getContext(), TypedValue.COMPLEX_UNIT_DIP, iconSize));
    }

    public Bitmap getLeftTopBmp() {
        return mLeftTopBmp;
    }

    /**
     * 设置左上角删除功能图标
     *
     * @param bitmap 位图对象
     */
    public void setLeftTopIcon(Bitmap bitmap) {
        this.mLeftTopBmp = bitmap;
        refresh(false, false);
    }

    /**
     * 设置左上角删除功能图标
     *
     * @param id 图片资源id
     */
    public void setLeftTopIcon(int id) {
        setLeftTopIcon(BitmapFactory.decodeResource(getResources(), id));
    }

    public Bitmap getmRightTopBmp() {
        return mRightTopBmp;
    }

    /**
     * 设置右上角删除功能图标
     *
     * @param bitmap 位图对象
     */
    public void setRightTopIcon(Bitmap bitmap) {
        this.mRightTopBmp = bitmap;
        refresh(false, false);
    }

    /**
     * 设置右上角删除功能图标
     *
     * @param id 图片资源id
     */
    public void setRightTopIcon(int id) {
        setRightTopIcon(BitmapFactory.decodeResource(getResources(), id));
    }

    public Bitmap getmRightBottomBmp() {
        return mRightBottomBmp;
    }

    /**
     * 设置右下角删除功能图标
     *
     * @param bitmap 位图对象
     */
    public void setRightBottomIcon(Bitmap bitmap) {
        this.mRightBottomBmp = bitmap;
        refresh(false, false);
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
        // 刚获得焦点，不允许点击事件
        mJustGetFocus = focus;
        this.mFocus = focus;
        // 焦点变更，通知
        notifyParentFocusChange(focus);
        refresh(false, false);
    }

    private void notifyParentFocusChange(boolean focus) {
        if (getParent() instanceof CaptionLayout) {
            CaptionLayout captionLayout = (CaptionLayout) getParent();
            captionLayout.updateCaptionFocusChangeView(this, focus);
        }
    }

    /**
     * @param scale 导出的目标相对于字幕控件的倍数
     * @return 获取导出的字幕信息
     */
    public CaptionInfo exportCaptionInfo(float scale) {
        return mIsImgCaption ? buildImageCaptionInfo(scale) : buildTextCaptionInfo(scale);
    }

    private ImageCaptionInfo buildImageCaptionInfo(float scale) {
        Rect locationRect = getTargetRect(scale);
        // 创建字幕图层
        Bitmap imageCaptionBitmap = Bitmap.createBitmap(locationRect.width(), locationRect.height(), Bitmap.Config
                .ARGB_8888);
        Canvas canvas = new Canvas(imageCaptionBitmap);
        canvas.drawBitmap(mImgCaptionBitmap, null, new Rect(0, 0, locationRect.width(), locationRect.height()),
                mBitmapPaint);
        return new ImageCaptionInfo(imageCaptionBitmap, locationRect, mTotalDegree, mUpdateMatrix, mImgCaptionBitmap);
    }

    private TextCaptionInfo buildTextCaptionInfo(float scale) {
        Rect locationRect = getTargetRect(scale);
        // 创建字幕图层
        Bitmap textCaptionBitmap = Bitmap.createBitmap(locationRect.width(), locationRect.height(), Bitmap.Config
                .ARGB_8888);
        Canvas canvas = new Canvas(textCaptionBitmap);
        float scalePaddingLeft = mPaddingLeft * mTotalScale * scale;
        float scalePaddingTop = mPaddingTop * mTotalScale * scale;
        float scalePaddingRight = mPaddingRight * mTotalScale * scale;
        float dx = 0, dy = scalePaddingTop;
        switch (mLayoutTextAlignment) {
            case ALIGN_NORMAL:
                dx = scalePaddingLeft;
                break;
            case ALIGN_OPPOSITE:
                dx = -scalePaddingRight;
                break;
            default:
                break;
        }
        canvas.translate(dx, dy);
        int breakWidth = (int) (mTextBorderWidth * mTotalScale * scale);
        mTextPaint.setTextSize(mTextPaint.getTextSize() * scale);
        mTextLayout = new StaticLayout(mText, mTextPaint, breakWidth, mLayoutTextAlignment, 1.0f, 0f, false);
        mTextLayout.draw(canvas);
        mTextPaint.setTextSize(mTextPaint.getTextSize() / scale);
        return new TextCaptionInfo(textCaptionBitmap, locationRect, mTotalDegree, new Matrix(mUpdateMatrix),
                mTotalScale, mText
                .toString(), mTextSize,
                mTextColor, mTextBorderWidth, mBorderColor, mTextTypeface, mLayoutTextAlignment, mPaddingLeft,
                mPaddingRight,
                mPaddingTop, mPaddingBottom);
    }

    private Rect getTargetRect(float scale) {
        mExportMatrix.set(mUpdateMatrix);
        // 摆正
        mExportMatrix.postRotate(-mTotalDegree, mCenterPoint.x, mCenterPoint.y);
        // 缩放到目标
        mExportMatrix.postScale(scale, scale, getWidth() * 1.0f / 2, getHeight() * 1.0f / 2);
        // 移动原点
        mExportMatrix.postTranslate(-(getWidth() - getWidth() * scale) / 2, -(getHeight() - getHeight() * scale) / 2);
        // 获取映射后矩形的位置信息
        RectF dst = new RectF();
        mExportMatrix.mapRect(dst, mBorderRect);
        return new Rect((int) dst.left, (int) dst.top, (int) dst.right, (int) dst.bottom);
    }

    /**
     * 重绘控件
     *
     * @param reset  内部是否需要重置初始化数据
     * @param update 基于当前状态更新信息
     */
    private void refresh(boolean reset, boolean update) {
        if (reset) {
            this.mResetData = reset;
        }
        if (update) {
            this.mUpdateCurrent = update;
        }
        postInvalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        log("onMeasure");
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        int measureWidth = widthSize, measureHeight = heightSize;
        if (widthMode != MeasureSpec.EXACTLY) {
            measureWidth = mTextBorderWidth <= 0 ? widthSize : mTextBorderWidth;
        }
        if (heightMode != MeasureSpec.EXACTLY) {
            // 未指定高度，与宽度一样
            measureHeight = measureWidth;
        }
        log("measureWidth=" + measureWidth + ",measureHeight=" + measureHeight);
        setMeasuredDimension(measureWidth, measureHeight);
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        log("onLayout");
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
        if (mFirstDraw || mResetData) {
            init();
            mFirstDraw = false;
            mResetData = false;
        }
        if (mUpdateCurrent) {
            updateDataBaseOnCurrent();
            mUpdateCurrent = false;
        }
        if (mIsImgCaption) {
            drawImgCaption(canvas);
        } else {
            drawText(canvas);
        }
        if (mFocus) {
            drawBorderRect(canvas);
            drawCornerIcon(canvas, mLeftTopBmp, mRightTopBmp, mRightBottomBmp);
        }
    }

    private void init() {
        log("init");
        if (mImgCaptionBitmap != null) {
            mIsImgCaption = mBlockRotateScaleEvent = true;
        } else {
            mIsImgCaption = mBlockRotateScaleEvent = false;
            mTextPaint.setTextSize(mTextSize);
            mTextPaint.setColor(mTextColor);
            mTextPaint.setTypeface(mTextTypeface);

            mBorderPaint.setColor(mBorderColor);
        }
        if (!mIsImportCaption) {
            mUpdateMatrix.reset();
            mTotalScale = 1f;
            mTotalDegree = 0f;
        } else {
            mIsImportCaption = false;
        }
    }

    private void updateDataBaseOnCurrent() {
        initBorderRect();
        updateBorderVertexData();
        updateCornerLocationData();
    }

    private void drawImgCaption(Canvas canvas) {
        canvas.save();
        canvas.translate(mLeftTopPoint.x, mLeftTopPoint.y);
        canvas.drawBitmap(mImgCaptionBitmap, 0, 0, mBitmapPaint);
        canvas.restore();
    }

    private void drawText(Canvas canvas) {
        // 摆正
        mUpdateMatrix.postRotate(-mTotalDegree, mCenterPoint.x, mCenterPoint.y);
        // 更新摆正后要写字的起始点
        mUpdateMatrix.mapPoints(mUpdateTextStartPoint, new float[]{mInitTextStartPoint.x, mInitTextStartPoint.y});
        canvas.save();
        // 旋转画布
        canvas.rotate(mTotalDegree, mCenterPoint.x, mCenterPoint.y);
        canvas.translate(mUpdateTextStartPoint[0], mUpdateTextStartPoint[1]);
        // 根据最宽一行测量出宽度用来切割
        // int breakWidth = (int) mTextPaint.measureText(mMaxWidthLineText, 0, mMaxWidthLineText.length());
        int breakWidth = (int) (mTextBorderWidth * mTotalScale);
        mTextLayout = new StaticLayout(mText, mTextPaint, breakWidth, mLayoutTextAlignment, 1.0f, 0f, false);
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
        if (mIsImgCaption) {
            // 贴图字幕
            mTextBorderWidth = mImgCaptionBitmap.getWidth();
            mTextBorderHeight = mImgCaptionBitmap.getHeight();
        } else {
            // 文字字幕
            if (mTextBorderWidth <= 0) {
                // 未设定边框宽度
                mTextBorderWidth = getWidth() - DEFAULT_BORDER_MARGIN;
            }
            float availableTextWidth =
                    Math.min(mTextBorderWidth - mPaddingLeft - mPaddingRight, getWidth() - mPaddingLeft -
                            mPaddingRight);
            float availableTextHeight =
                    Math.min(mTextBorderWidth - mPaddingTop - mPaddingBottom, getHeight() - mPaddingTop -
                            mPaddingBottom);
            // 如果需要，调整字体大小以便边框能够容纳下内容
            adjustTextSizeToFitBorder(availableTextWidth, availableTextHeight);
            // 确定边框高度
            mTextBorderHeight = mTextLayout.getHeight() + mPaddingTop + mPaddingBottom;
            // 获取最大放大比例
            mMaxScale = Math.min(getWidth() * 1.0f / mTextBorderWidth, getHeight() * 1.0f / mTextBorderHeight);
        }
        float rectLeft = (getWidth() - mTextBorderWidth) / 2;
        float rectRight = rectLeft + mTextBorderWidth;
        float rectTop = (getHeight() - mTextBorderHeight) / 2;
        float rectBottom = rectTop + mTextBorderHeight;
        mBorderRect.set(rectLeft, rectTop, rectRight, rectBottom);

        determineTextStartPoint();
    }

    private void determineTextStartPoint() {
        switch (mLayoutTextAlignment) {
            case ALIGN_NORMAL:
                mInitTextStartPoint.set(mBorderRect.left + mPaddingLeft, mBorderRect.top + mPaddingTop);
                break;
            case ALIGN_OPPOSITE:
                mInitTextStartPoint.set(mBorderRect.left - mPaddingRight, mBorderRect.top + mPaddingTop);
                break;
            default:
                mInitTextStartPoint.set(mBorderRect.left, mBorderRect.top + mPaddingTop);
                break;
        }
    }

    private void adjustTextSizeToFitBorder(float availableTextWidth, float availableTextHeight) {
        // 用初始字体大小来调整
        mTextPaint.setTextSize(mTextSize);
        while (true) {
            mTextLayout =
                    new StaticLayout(mText, mTextPaint, Integer.MAX_VALUE, mLayoutTextAlignment, 1.0f, 0f, false);
            // 获取需要缩小倍数
            float resizeScale = getResizeScale(mTextLayout, availableTextWidth, availableTextHeight);
            if (resizeScale < 1) {
                mTextPaint.setTextSize(mTextPaint.getTextSize() * resizeScale);
            } else {
                break;
            }
        }
        // 恢复当前显示的字体大小
        mTextPaint.setTextSize(mTextPaint.getTextSize() * mTotalScale);
    }

    private float getResizeScale(StaticLayout mTextLayout, float availableTextWidth, float availableTextHeight) {
        float maxWidth = mTextLayout.getLineWidth(0);
        for (int i = 1; i < mTextLayout.getLineCount(); i++) {
            float lineWidth = mTextLayout.getLineWidth(i);
            if (lineWidth > maxWidth) {
                maxWidth = lineWidth;
            }
        }
        return Math.min(availableTextWidth / maxWidth, availableTextHeight / mTextLayout.getHeight());
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

    private float mLastX, mLastY; // 上次点击的坐标
    private float mDownDistance; // 按下时两指间的距离
    private float mPointerDegree; // 按下时两指的旋转角度
    private int mStillDownPointId = 0;

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
                if (mBlockRotateScaleEvent) {
                    break;
                }
                // 不处理超过两个点的情况
                if (event.getActionIndex() > 1) {
                    break;
                }
                float secondPointX = event.getX(event.getActionIndex());
                float secondPointY = event.getY(event.getActionIndex());
                if (mTouchRegion == TouchRegion.INSIDE && isInBorderRegion(secondPointX, secondPointY)) {
                    mDownDistance = calculatePointsDistance(event);
                    mTouchMode = TouchMode.POINTER_SCALE_ROTATE;
                    mPointerDegree = calculatePointerRotationDegree(event);
                } else {
                    mTouchMode = TouchMode.NONE;
                }
                break;
            case MotionEvent.ACTION_MOVE:
                onMove(event);
                break;
            case MotionEvent.ACTION_POINTER_UP:
                if (mBlockRotateScaleEvent) {
                    break;
                }
                int leavePointerId = event.getActionIndex();
                // 前两个按下的点有效
                if (mTouchRegion == TouchRegion.INSIDE && leavePointerId < 2) {
                    // 前一个按住的点离开，切换点击坐标
                    if (leavePointerId == mStillDownPointId) {
                        mStillDownPointId = mStillDownPointId == 0 ? 1 : 0;
                    }
                    curX = event.getX(mStillDownPointId);
                    curY = event.getY(mStillDownPointId);
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
                if (mBlockRotateScaleEvent) {
                    break;
                } else if (!mJustGetFocus && mTouchRegion == TouchRegion.INSIDE) {
                    log("event.getDownTime()=" + (event.getEventTime() - event.getDownTime()));
                    long clickTimeout = event.getEventTime() - event.getDownTime();
                    if (clickTimeout <= CLICK_TIMEOUT && mOnCaptionClickListener != null) {
                        mOnCaptionClickListener.onClick(this);
                    }
                }
                mJustGetFocus = false;
                adjustRotateIfNeed();
                mTouchMode = TouchMode.NONE;
                mStillDownPointId = 0;
                break;
        }
        mLastX = curX;
        mLastY = curY;
        return consume;
    }

    private void adjustRotateIfNeed() {
        if (mTouchRegion == TouchRegion.INSIDE || mTouchRegion == TouchRegion.RIGHT_BOTTOM_ICON) {
            float adjustDegree = adjustDegreeToHorizontalOrVertical(mTotalDegree, OFFSET_DEGREE);
            mUpdateMatrix.postRotate(adjustDegree - mTotalDegree, mCenterPoint.x, mCenterPoint.y);
            updateLocationDataAndRefresh();
            mTotalDegree = adjustDegree;
        }
    }

    /**
     * 如果角度与水平或竖直方向夹角为offsetDegree范围内，调整为水平或竖直方向
     *
     * @param mTotalDegree 当前角度
     * @param offsetDegree 需要调整角度的范围
     * @return 调整后的角度
     */
    private float adjustDegreeToHorizontalOrVertical(float mTotalDegree, float offsetDegree) {
        float adjustedDegree = (mTotalDegree + 360) % 360;
        for (int i = 0; i <= 360; i += 90) {
            if (adjustedDegree >= i - offsetDegree && adjustedDegree <= i + offsetDegree) {
                adjustedDegree = i;
                break;
            }
        }
        return adjustedDegree % 360;
    }

    // 是否要放弃处理事件
    private boolean shouldGiveUpTheEvent() {
        if (getParent() instanceof CaptionLayout) {
            CaptionLayout captionLayout = (CaptionLayout) getParent();
            // 不处理可能已经由其它控件正在处理的事件
            if (mTouchMode == TouchMode.NONE && captionLayout.mIsPointerDown) {
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
                    processMove(curX - mLastX, curY - mLastY);
                } else if (mTouchMode == TouchMode.POINTER_SCALE_ROTATE) {
                    // 双指缩放
                    float curDistance = calculatePointsDistance(event);
                    float scale = curDistance / mDownDistance;
                    mDownDistance = curDistance;
                    processScale(scale);
                    // 旋转
                    float curDegree = calculatePointerRotationDegree(event);
                    float degree = curDegree - mPointerDegree;
                    mPointerDegree = curDegree;
                    processRotate(degree);
                }
                break;
            case RIGHT_BOTTOM_ICON:
                if (mBlockRotateScaleEvent) {
                    break;
                }
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
        float degree = calculateRotationDegree(curX, curY, mLastX, mLastY);
        processRotate(degree);
    }

    // 计算旋转角度
    private float calculateRotationDegree(float curX, float curY, float lastX, float lastY) {
        // 根据斜率算夹角
        return (float) Math.toDegrees(Math.atan2(curY - mCenterPoint.y, curX - mCenterPoint.x))
                - (float) Math.toDegrees(Math.atan2(lastY - mCenterPoint.y, lastX - mCenterPoint.x));
    }

    private float calculateScale(float curX, float curY) {
        double oldRadius = calculateRadius(mLastX, mLastY);
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
        if (mRightBottomBmp != null && mCurrentRightBottomRect.contains(curX, curY)) {
            if (!needToSetFocusFirstly()) {
                mTouchRegion = TouchRegion.RIGHT_BOTTOM_ICON;
            }
        } else if (mLeftTopBmp != null && mCurrentLeftTopIconRect.contains(curX, curY)) {
            if (!needToSetFocusFirstly()) {
                mTouchRegion = TouchRegion.LEFT_TOP_ICON;
                ((ViewGroup) getParent()).removeView(this);
            }
        } else if (isInBorderRegion(curX, curY)) {
            if (!needToSetFocusFirstly()) {
                // 内容与角落重叠区域响应角落事件
                mTouchRegion = TouchRegion.INSIDE;
            }
        } else {
            setFocus(false);
            mTouchRegion = TouchRegion.OUTSIDE;
            consume = false;
        }
        log("determineTouchRegion,curX=" + curX + ",curY=" + curY + ",mTouchRegion=" + mTouchRegion.name());
        return consume;
    }

    private boolean needToSetFocusFirstly() {
        if (!mFocus) {
            setFocus(true);
            mTouchRegion = TouchRegion.INSIDE;
            return true;
        }
        return false;
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
        // 检查平移是否超出边界
        float newCenterX = mCenterPoint.x + dx;
        float newCenterY = mCenterPoint.y + dy;
        if (newCenterX < 0) {
            dx = 0 - mCenterPoint.x;
        } else if (newCenterX > getWidth()) {
            dx = getWidth() - mCenterPoint.x;
        }
        if (newCenterY < 0) {
            dy = 0 - mCenterPoint.y;
        } else if (newCenterY > getHeight()) {
            dy = getHeight() - mCenterPoint.y;
        }
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
        updateTextPaint(scale);
        updateLocationDataAndRefresh();
    }

    private void processRotate(float degree) {
        if (degree == 0) {
            return;
        }
        mTotalDegree = (mTotalDegree + degree) % 360;
        mUpdateMatrix.postRotate(degree, mCenterPoint.x, mCenterPoint.y);
        updateLocationDataAndRefresh();
    }

    // 字体相关参数改变
    private void updateTextPaint(float scale) {
        mTextPaint.setTextSize(mTextPaint.getTextSize() * scale);
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
        mUpdateMatrix.mapPoints(dst, new float[]{mBorderRect.left, mBorderRect.top, mBorderRect.right,
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
        if (!debug) {
            return;
        }
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

    /**
     * 字幕点击监听器
     */
    public interface OnCaptionClickListener {
        /**
         * 字幕控件被点击时调用
         *
         * @param captionView 字幕控件
         */
        void onClick(FlexibleCaptionView captionView);
    }
}
