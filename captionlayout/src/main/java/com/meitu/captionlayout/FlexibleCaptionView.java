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
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
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
     * 水平或竖直方向的偏移角度范围
     */
    private static final float OFFSET_DEGREE = 10f;
    /**
     * 点击事件触发的超时时间
     */
    private static final int CLICK_TIMEOUT = ViewConfiguration.getTapTimeout();
    /**
     * 默认的边距
     */
    private static final int DEFAULT_PADDING = 8;
    /**
     * 边框能达到的最大放大倍数，相对于控件的宽高
     */
    private static final float MAX_BORDER_SCALE = 1.5f;

    private boolean mDebug = false;

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
    private int mPaddingLeft, mPaddingRight, mPaddingTop, mPaddingBottom;
    private Layout.Alignment mLayoutTextAlignment = Layout.Alignment.ALIGN_CENTER;
    private PointF mInitTextStartPoint = new PointF();
    private float[] mUpdateTextStartPoint = new float[2];
    private StaticLayout mTextLayout;
    private CharSequence mMaxWidthLineText;

    private Bitmap mImgCaptionBitmap; // 贴图字幕
    private boolean mIsImgCaption;
    private boolean mBlockRotateScaleEvent; // 贴图字幕拦截旋转缩放功能

    private TouchRegion mTouchRegion; // 触摸的区域
    private TouchMode mTouchMode = TouchMode.NONE; // 触摸模式
    private boolean mFocus = true;

    private float mTotalScale;
    private float mTotalDegree;
    private boolean mFirstDraw = true;
    private boolean mResetData = true; // 是否需要重置数据
    private boolean mUpdateBaseData = true; // 是否更新初始信息
    private boolean mIsImportCaption = false; // 是否为导入的字幕
    private CaptionInfo mCaptionInfo;
    private float mFingerDegree; // 手指移动的角度，为了旋转时依附的效果并且跟手

    private Matrix mExportMatrix = new Matrix(); // 用于导出字幕的矩阵

    private OnCaptionClickListener mOnCaptionClickListener;
    private OnCaptionTranslateListener mOnCaptionTranslateListener;
    private boolean mTranslateStart; // 平移开始标记
    private boolean mCancelClick; // 取消点击事件
    private boolean mEnable = true;

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

        mPaddingLeft =
            mPaddingRight =
                mPaddingTop = mPaddingBottom = (int) convert2px(context, TypedValue.COMPLEX_UNIT_DIP, DEFAULT_PADDING);
        // int padding = typedArray.getDimensionPixelSize(R.styleable.FlexibleCaptionView_textPadding, 0);
        // mPaddingLeft = mPaddingRight = mPaddingTop = mPaddingBottom = padding;
        // mPaddingLeft = typedArray.getDimensionPixelSize(R.styleable.FlexibleCaptionView_textPaddingLeft,
        // mPaddingLeft);
        // mPaddingRight =
        // typedArray.getDimensionPixelSize(R.styleable.FlexibleCaptionView_textPaddingRight, mPaddingRight);
        // mPaddingTop = typedArray.getDimensionPixelSize(R.styleable.FlexibleCaptionView_textPaddingTop, mPaddingTop);
        // mPaddingBottom =
        // typedArray.getDimensionPixelSize(R.styleable.FlexibleCaptionView_textPaddingBottom, mPaddingBottom);

        // 未设定边框宽度且未设定文本内容，后续绘制将会以父控件的宽度为参考作为边框宽度

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
        // 防止字号太大不能正常渲染的问题
        setLayerType(View.LAYER_TYPE_SOFTWARE, mTextPaint);
    }

    /**
     * 字幕控件构造器，方便创建字幕控件对象
     */
    public static class Builder {

        private Context mContext;
        private CharSequence mText;
        private Float mTextSize;
        private Integer mTextColor;
        private Integer mTextBorderColor;
        private Typeface mTextTypeface;
        private Layout.Alignment mLayoutTextAlignment;
        private Integer mPaddingLeft, mPaddingRight, mPaddingTop, mPaddingBottom;
        private Bitmap mLeftTopIconBmp, mRightTopIconBmp, mRightBottomIconBmp;
        private Integer mIconSize;
        private Bitmap mImgCaptionBitmap;

        private CaptionInfo mCaptionInfo;

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
            mTextColor = editText.getCurrentTextColor();
            mTextTypeface = editText.getTypeface();

            mPaddingLeft = editText.getPaddingLeft();
            mPaddingRight = editText.getPaddingRight();
            mPaddingTop = editText.getPaddingTop();
            mPaddingBottom = editText.getPaddingBottom();
            return this;
        }

        public Builder loadConfig(CaptionInfo captionInfo) {
            mCaptionInfo = captionInfo;
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

        /*        public Builder padding(int unit, int value) {
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
                }*/

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

            view.mLeftTopBmp = mLeftTopIconBmp;
            view.mRightTopBmp = mRightTopIconBmp;
            view.mRightBottomBmp = mRightBottomIconBmp;
            view.mIconSize = mIconSize == null ? view.mIconSize : mIconSize;

            view.mImgCaptionBitmap = mImgCaptionBitmap;

            if (mCaptionInfo != null) {
                view.mIsImportCaption = true;
                view.mCaptionInfo = mCaptionInfo;
                if (mImgCaptionBitmap != null) {
                    view.mIsImgCaption = true;
                    view.mImgCaptionBitmap = mImgCaptionBitmap;
                }
            }
            view.refresh(true, true);
            return view;
        }
    }

    public OnCaptionTranslateListener getOnCaptionTranslateListener() {
        return mOnCaptionTranslateListener;
    }

    /**
     * 设置字幕手势动作监听
     *
     * @param listener
     */
    public void setOnCaptionTranslateListener(OnCaptionTranslateListener listener) {
        this.mOnCaptionTranslateListener = listener;
    }

    public OnCaptionClickListener getOnCaptionClickListener() {
        return mOnCaptionClickListener;
    }

    /**
     * 设置点击事件监听
     *
     * @param mCaptionClickListener 点击监听
     */
    public void setOnCaptionClickListener(OnCaptionClickListener mCaptionClickListener) {
        this.mOnCaptionClickListener = mCaptionClickListener;
    }

    /**
     * @return 获取是否为调试模式
     */
    public boolean isDebug() {
        return mDebug;
    }

    /**
     * 设置是否为调试模式
     *
     * @param debug 是否调试
     */
    public void setDebug(boolean debug) {
        this.mDebug = debug;
    }

    /**
     * @return 获取当前操作状态
     */
    public boolean isEnable() {
        return mEnable;
    }

    /**
     * 设置字幕是否可操作
     *
     * @param mEnable 是否可操作
     */
    public void setEnable(boolean mEnable) {
        this.mEnable = mEnable;
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
        if (this.mLayoutTextAlignment.equals(textAlignment)) {
            return;
        }
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
        if (this.mTextTypeface.equals(typeface)) {
            return;
        }
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
        if (TextUtils.equals(this.mText, text)) {
            return;
        }
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
        if (this.mTextColor == textColor) {
            return;
        }
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
     * 设置当前字体大小
     *
     * @param unit  单位
     * @param value 值
     */
    public void setTextSize(int unit, float value) {
        if (value <= 0) {
            log("mTextSize must be > 0");
            return;
        }
        float textSize = convert2px(getContext(), unit, value);
        if (this.mTextSize == textSize) {
            return;
        }
        mTextPaint.setTextSize(textSize);
        refresh(false, true);
    }

    /*

        */
    /**
     * 设置文字与边框的间隔，会影响内容换行
     *
     * @param unit  单位
     * @param value 值
     */
    /*

     public void setPadding(int unit, float value) {
         mPaddingLeft = mPaddingRight = mPaddingTop = mPaddingBottom = (int) convert2px(getContext(), unit, value);
         refresh(true, true);
     }

     */
    /**
     * @return 获取边框与文字左边间距，单位px
     */
    /*

     public int getPaddingLeft() {
         return mPaddingLeft;
     }

     */
    /**
     * 设置边框与文字左边间距，会影响内容换行
     *
     * @param mPaddingLeft 间距，单位px
     */
    /*

     public void setPaddingLeft(int mPaddingLeft) {
         this.mPaddingLeft = mPaddingLeft;
         refresh(true, true);
     }

     */
    /**
     * @return 获取边框与文字右边间距，单位px
     */
    /*

     public int getPaddingRight() {
         return mPaddingRight;
     }

     */
    /**
     * 设置边框与文字右边间距，会影响内容换行
     *
     * @param mPaddingRight 间距，单位px
     */
    /*

     public void setPaddingRight(int mPaddingRight) {
         this.mPaddingRight = mPaddingRight;
         refresh(true, true);
     }

     */
    /**
     * @return 获取边框与上字左边间距，单位px
     */
    /*

     public int getPaddingTop() {
         return mPaddingTop;
     }

     */
    /**
     * 设置边框与文字上边间距
     *
     * @param mPaddingTop 间距，单位px
     */
    /*

     public void setPaddingTop(int mPaddingTop) {
         this.mPaddingTop = mPaddingTop;
         refresh(true, true);
     }

     */
    /**
     * @return 获取边框与文字下边间距，单位px
     */
    /*

     public int getPaddingBottom() {
         return mPaddingBottom;
     }

     */
    /**
     * 设置边框与文字下边间距
     *
     * @param mPaddingBottom 间距，单位px
     */
    /*

     public void setPaddingBottom(int mPaddingBottom) {
         this.mPaddingBottom = mPaddingBottom;
         refresh(true, true);
     }
    */

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
        if (this.mBorderColor == borderColor) {
            return;
        }
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
        if (this.mIconSize == iconSize) {
            return;
        }
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
        if (this.mLeftTopBmp == bitmap) {
            return;
        }
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
        if (this.mRightTopBmp == bitmap) {
            return;
        }
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

    public Bitmap getRightBottomBmp() {
        return mRightBottomBmp;
    }

    /**
     * 设置右下角删除功能图标
     *
     * @param bitmap 位图对象
     */
    public void setRightBottomIcon(Bitmap bitmap) {
        if (this.mRightBottomBmp == bitmap) {
            return;
        }
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
        this.mFocus = focus;
        // 外部调用导致的焦点变更，通知父容器
        notifyParentFocusChange(focus);
        refresh(false, false);
    }

    protected void setFocusFromParent(boolean focus) {
        if (this.mFocus == focus) {
            return;
        }
        this.mFocus = focus;
        refresh(false, false);
    }

    private void notifyParentFocusChange(boolean focus) {
        if (getParent() instanceof CaptionLayout) {
            CaptionLayout parent = (CaptionLayout) getParent();
            parent.onChildFocusChangeWithoutTouchEvent(this, focus);
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
        Rect targetRect = getTargetRect(scale);
        // 创建字幕图层
        Bitmap imageCaptionBitmap =
            Bitmap.createBitmap(targetRect.width(), targetRect.height(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(imageCaptionBitmap);
        canvas.drawBitmap(mImgCaptionBitmap, null, new Rect(0, 0, targetRect.width(), targetRect.height()),
            mBitmapPaint);
        // 构建导出对象
        float relativeCenterX = mCenterPoint.x / getWidth();
        float relativeCenterY = mCenterPoint.y / getHeight();
        int width = getIntrinsicRect().width();
        int height = getIntrinsicRect().height();
        return new ImageCaptionInfo(imageCaptionBitmap, mTotalDegree, relativeCenterX, relativeCenterY, width, height,
            mImgCaptionBitmap);
    }

    private TextCaptionInfo buildTextCaptionInfo(float scale) {
        Rect targetRect = getTargetRect(scale);
        // 创建字幕图层
        Bitmap textCaptionBitmap =
            Bitmap.createBitmap(targetRect.width(), targetRect.height(), Bitmap.Config.ARGB_8888);
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
        float textSize = mTextPaint.getTextSize();
        mTextPaint.setTextSize(textSize * scale);
        mTextLayout =
            new StaticLayout(mText, mTextPaint, getStaticLayoutBreakWidth(scale), mLayoutTextAlignment, 1.0f, 0f, false);
        mTextLayout.draw(canvas);
        mTextPaint.setTextSize(textSize);
        // 构建导出对象
        float relativeCenterX = mCenterPoint.x / getWidth();
        float relativeCenterY = mCenterPoint.y / getHeight();
        int width = getIntrinsicRect().width();
        int height = getIntrinsicRect().height();
        int padding = (int) (mPaddingLeft * mTotalScale);
        return new TextCaptionInfo(textCaptionBitmap, mTotalDegree, relativeCenterX, relativeCenterY, width, height,
            mText.toString(), textSize, mTextColor, mBorderColor, mTextTypeface, mLayoutTextAlignment, padding);
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

    private Rect getIntrinsicRect() {
        mExportMatrix.set(mUpdateMatrix);
        // 摆正
        mExportMatrix.postRotate(-mTotalDegree, mCenterPoint.x, mCenterPoint.y);
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
        if (!mResetData && reset) {
            this.mResetData = reset;
        }
        if (!mUpdateBaseData && update) {
            this.mUpdateBaseData = update;
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
            // 未指定控件宽度，与父控件一样大
            measureWidth = widthSize;
        }
        if (heightMode != MeasureSpec.EXACTLY) {
            // 未指定高度，与父控件一样大
            measureHeight = measureWidth;
        }
        log("measureWidth=" + measureWidth + ",measureHeight=" + measureHeight);
        setMeasuredDimension(measureWidth, measureHeight);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        log("onSizeChanged,w=" + w + ",h=" + h + ",oldw=" + oldw + ",oldh=" + oldh);
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
        if (mUpdateBaseData) {
            updateBaseData();
            mUpdateBaseData = false;
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
        // 中点默认在中间
        mCenterPoint.set(getWidth() / 2f, getHeight() / 2f);

        if (mIsImportCaption) {
            loadImportCaptionInfo();
        } else {
            mUpdateMatrix.reset();
            mTotalScale = 1f;
            mTotalDegree = 0f;
        }
        if (mImgCaptionBitmap != null) {
            mIsImgCaption = mBlockRotateScaleEvent = true;
        } else {
            mIsImgCaption = mBlockRotateScaleEvent = false;
            mTextPaint.setTextSize(mTextSize);
            mTextPaint.setColor(mTextColor);
            mTextPaint.setTypeface(mTextTypeface);

            mBorderPaint.setColor(mBorderColor);
        }
    }

    private void loadImportCaptionInfo() {
        // 加载导入的字幕信息
        mTotalScale = 1f;
        mTotalDegree = mFingerDegree = mCaptionInfo.degree;
        if (mCaptionInfo instanceof TextCaptionInfo) {
            TextCaptionInfo textCaptionInfo = (TextCaptionInfo) mCaptionInfo;
            mTextSize = textCaptionInfo.textSize;
            mText = textCaptionInfo.text;

            mTextTypeface = textCaptionInfo.textTypeface;
            mLayoutTextAlignment = textCaptionInfo.textAlignment;
            mBorderColor = textCaptionInfo.textBorderColor;
            mTextColor = textCaptionInfo.textColor;
            mPaddingLeft = mPaddingRight = mPaddingTop = mPaddingBottom = textCaptionInfo.textPadding;
        } else if (mCaptionInfo instanceof ImageCaptionInfo) {
            ImageCaptionInfo imageCaptionInfo = (ImageCaptionInfo) mCaptionInfo;
            mImgCaptionBitmap = imageCaptionInfo.intrinsicBitmap;
        }

    }

    private void updateBaseData() {
        initBorderRect();
        updateBorderVertexData();
        updateCornerLocationData();

        // 恢复导出时的位置
        if (mIsImportCaption) {
            float currentCenterX = mCaptionInfo.relativeCenterX * getWidth();
            float currentCenterY = mCaptionInfo.relativeCenterY * getHeight();
            mUpdateMatrix.reset();
            mUpdateMatrix.postRotate(mTotalDegree, mCenterPoint.x, mCenterPoint.y);
            mUpdateMatrix.postTranslate(currentCenterX - mCenterPoint.x, currentCenterY - mCenterPoint.y);
            updateLocationDataAndRefresh();
            mIsImportCaption = false;
        }
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
        mUpdateMatrix.mapPoints(mUpdateTextStartPoint, new float[] {mInitTextStartPoint.x, mInitTextStartPoint.y});
        canvas.save();
        // 旋转画布
        canvas.rotate(mTotalDegree, mCenterPoint.x, mCenterPoint.y);
        canvas.translate(mUpdateTextStartPoint[0], mUpdateTextStartPoint[1]);

        mTextLayout =
            new StaticLayout(mText, mTextPaint, getStaticLayoutBreakWidth(1.0f), mLayoutTextAlignment, 1.0f, 0f, false);
        mTextLayout.draw(canvas);
        canvas.restore();
        // 恢复矩阵的改变
        mUpdateMatrix.postRotate(mTotalDegree, mCenterPoint.x, mCenterPoint.y);
    }

    private int getStaticLayoutBreakWidth(float scale) {
        // 根据当前边框宽度和最宽一行测量出宽度用来切割，防止不同字体导致的换行问题
        return (int) Math.max(mBorderRect.width() * mTotalScale * scale,
            mTextPaint.measureText(mMaxWidthLineText.toString()));
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
        int mTextBorderWidth, mTextBorderHeight;

        if (mIsImgCaption) {
            // 贴图字幕
            mTextBorderWidth = mImgCaptionBitmap.getWidth();
            mTextBorderHeight = mImgCaptionBitmap.getHeight();
        } else {
            float availableTextWidth = (getWidth() - mPaddingLeft - mPaddingRight) * MAX_BORDER_SCALE;
            float availableTextHeight = (getHeight() - mPaddingTop - mPaddingBottom) * MAX_BORDER_SCALE;
            adjustTextSizeToFitMaxBorder(availableTextWidth, availableTextHeight);
            mTextLayout =
                new StaticLayout(mText, mTextPaint, Integer.MAX_VALUE, Layout.Alignment.ALIGN_NORMAL, 1.0f, 0f, false);
            // 确定边框宽高
            float maxWidth = mTextLayout.getLineWidth(0);
            int lineStart = mTextLayout.getLineStart(0), lineEnd = mTextLayout.getLineEnd(0);
            for (int i = 1; i < mTextLayout.getLineCount(); i++) {
                if (maxWidth < mTextLayout.getLineWidth(i)) {
                    maxWidth = mTextLayout.getLineWidth(i);
                    lineStart = mTextLayout.getLineStart(i);
                    lineEnd = mTextLayout.getLineEnd(i);
                }
            }
            mMaxWidthLineText = mText.subSequence(lineStart, lineEnd);
            mTextBorderWidth = (int) (maxWidth + mPaddingLeft + mPaddingRight);
            mTextBorderHeight = mTextLayout.getHeight() + mPaddingTop + mPaddingBottom;
        }
        log("mTextBorderWidth=" + mTextBorderWidth + ",mTextBorderHeight=" + mTextBorderHeight);

        // 根据中心点获取边框位置
        float rectLeft = mCenterPoint.x - mTextBorderWidth / 2f;
        float rectRight = rectLeft + mTextBorderWidth;
        float rectTop = mCenterPoint.y - mTextBorderHeight / 2f;
        float rectBottom = rectTop + mTextBorderHeight;
        mBorderRect.set(rectLeft, rectTop, rectRight, rectBottom);
        determineTextStartPoint();

        resetUpdateMatrixExceptRotate();
    }

    private void adjustTextSizeToFitMaxBorder(float availableTextWidth, float availableTextHeight) {
        if (availableTextWidth == 0 || availableTextHeight == 0) {
            return;
        }
        while (true) {
            mTextLayout = new StaticLayout(mText, mTextPaint, Integer.MAX_VALUE, mLayoutTextAlignment, 1.0f, 0f, false);
            // 获取需要缩小倍数
            float resizeScale = getResizeScale(mTextLayout, availableTextWidth, availableTextHeight);
            if (resizeScale < 1) {
                mTextPaint.setTextSize(mTextPaint.getTextSize() * resizeScale);
            } else {
                break;
            }
        }
        log("mTextPaint.getTextSize()=" + mTextPaint.getTextSize());
    }

    private float getResizeScale(StaticLayout mTextLayout, float availableTextWidth, float availableTextHeight) {
        float maxWidth = mTextLayout.getLineWidth(0);
        for (int i = 1; i < mTextLayout.getLineCount(); i++) {
            float lineWidth = mTextLayout.getLineWidth(i);
            if (lineWidth > maxWidth) {
                maxWidth = lineWidth;
            }
        }
        float widthScale = availableTextWidth / maxWidth;
        float heightScale = availableTextHeight / mTextLayout.getHeight();
        return Math.min(widthScale, heightScale);
    }

    // 重置矩阵，只保留旋转变换
    private void resetUpdateMatrixExceptRotate() {
        mUpdateMatrix.reset();
        mTotalScale = 1f;
        mUpdateMatrix.postRotate(mTotalDegree, mCenterPoint.x, mCenterPoint.y);
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

    private float mDownX, mDownY;
    private float mLastX, mLastY; // 上次点击的坐标
    private float mDownDistance; // 按下时两指间的距离
    private float mPointerDegree; // 按下时两指的旋转角度
    private int mStillDownPointId = 0;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!mEnable) {
            return false;
        }
        if (shouldGiveUpTheEvent()) {
            return false;
        }
        boolean consume = true;
        float curX = event.getX();
        float curY = event.getY();
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                mDownX = curX;
                mDownY = curY;
                consume = determineTouchRegion(curX, curY);
                if (mTouchRegion == TouchRegion.INSIDE) {
                    mTouchMode = TouchMode.DRAG;
                }
                mTranslateStart = false;
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
                if (leavePointerId < 2) {
                    // 前一个按住的点离开，切换点击坐标
                    if (leavePointerId == mStillDownPointId) {
                        mStillDownPointId = mStillDownPointId == 0 ? 1 : 0;
                    }
                    curX = event.getX(mStillDownPointId);
                    curY = event.getY(mStillDownPointId);
                    if (mCurrentRightBottomRect.contains(curX, curY)) {
                        // 留下的点在右下角，变更按下区域
                        mTouchRegion = TouchRegion.RIGHT_BOTTOM_ICON;
                    } else if (isInBorderRegion(curX, curY)) {
                        mTouchRegion = TouchRegion.INSIDE;
                        // 留下的点在区域内，转为拖拽
                        mTouchMode = TouchMode.DRAG;
                    } else {
                        mTouchRegion = TouchRegion.OUTSIDE;
                        mTouchMode = TouchMode.NONE;
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
                // 刚获得焦点，不允许点击事件
                if (!mCancelClick) {
                    long clickTimeout = event.getEventTime() - event.getDownTime();
                    if (clickTimeout <= CLICK_TIMEOUT) {
                        notifyRegionClick();
                    }
                }
                mCancelClick = false;

                mTouchMode = TouchMode.NONE;
                mStillDownPointId = 0;
                if (mTranslateStart) {
                    notifyTranslateEnd();
                }
                break;
        }
        mLastX = curX;
        mLastY = curY;
        return consume;
    }

    private void notifyRegionClick() {
        if (mOnCaptionClickListener == null) {
            return;
        }
        switch (mTouchRegion) {
            case INSIDE:
                mOnCaptionClickListener.onInsideClick(this);
                break;
            case LEFT_TOP_ICON:
                mOnCaptionClickListener.onLeftTopClick(this);
                break;
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
                    processMove(curX, curY);
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

    private void notifyTranslateStart() {
        if (mOnCaptionTranslateListener != null) {
            mOnCaptionTranslateListener.onStart(this);
        }
    }

    private void notifyTranslateEnd() {
        if (mOnCaptionTranslateListener != null) {
            mOnCaptionTranslateListener.onEnd(this);
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
        return (float) Math.sqrt(x * x + y * y);
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
            }
        } else if (isInBorderRegion(curX, curY)) {
            if (!needToSetFocusFirstly()) {
                // 内容与角落重叠区域优先响应角落事件
                mTouchRegion = TouchRegion.INSIDE;
            }
        } else {
            mTouchRegion = TouchRegion.OUTSIDE;
            consume = false;
        }
        log("determineTouchRegion,curX=" + curX + ",curY=" + curY + ",mTouchRegion=" + mTouchRegion.name());
        return consume;
    }

    private boolean needToSetFocusFirstly() {
        if (!mFocus) {
            if (getParent() instanceof CaptionLayout) {
                CaptionLayout parent = (CaptionLayout) getParent();
                parent.markChildFocus(this);
            }
            mTouchRegion = TouchRegion.INSIDE;
            // 想要选中，而不是点击
            mCancelClick = true;
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

    private void processMove(float curX, float curY) {
        float totalX = Math.abs(curX - mDownX), totalY = Math.abs(curY - mDownY);
        if (Math.abs(totalX) >= ViewConfiguration.get(getContext()).getScaledTouchSlop()
            || Math.abs(totalY) >= ViewConfiguration.get(getContext()).getScaledTouchSlop()) {
            // 猜测用户想要移动，而不是单击
            mCancelClick = true;
            // 平移动作开始，通知
            if (!mTranslateStart) {
                mTranslateStart = true;
                notifyTranslateStart();
            }
        }

        float dx = curX - mLastX;
        float dy = curY - mLastY;
        // 检查平移是否超出边界
        float[] adjustDxDy = checkMoveBounds(dx, dy);
        dx = adjustDxDy[0];
        dy = adjustDxDy[1];
        if (dx == 0 && dy == 0) {
            return;
        }
        mUpdateMatrix.postTranslate(dx, dy);
        updateLocationDataAndRefresh();
    }

    private float[] checkMoveBounds(float dx, float dy) {
        // 防止中点移除边界
        float afterMoveX = mCenterPoint.x + dx;
        if (afterMoveX < 0) {
            dx = -mCenterPoint.x;
        } else if (afterMoveX > getWidth()) {
            dx = getWidth() - mCenterPoint.x;
        }
        float afterMoveY = mCenterPoint.y + dy;
        if (afterMoveY < 0) {
            dy = -mCenterPoint.y;
        } else if (afterMoveY > getHeight()) {
            dy = getHeight() - mCenterPoint.y;
        }
        return new float[] {dx, dy};
    }

    private void processScale(float scale) {
        scale = checkScaleBounds(scale);
        if (scale == 1) {
            return;
        }
        this.mTotalScale *= scale;
        mUpdateMatrix.postScale(scale, scale, mCenterPoint.x, mCenterPoint.y);
        updateTextPaint(scale);
        updateLocationDataAndRefresh();
    }

    private float checkScaleBounds(float scale) {
        // 边框宽度不能大于控件宽度的设置倍数
        if (mBorderRect.width() * mTotalScale * scale > getWidth() * MAX_BORDER_SCALE) {
            return getWidth() * MAX_BORDER_SCALE / (mBorderRect.width() * mTotalScale);
        }
        // 边框高度不能大于控件高度的设置倍数
        if (mBorderRect.height() * mTotalScale * scale > getHeight() * MAX_BORDER_SCALE) {
            return getHeight() * MAX_BORDER_SCALE / (mBorderRect.height() * mTotalScale);
        }
        return scale;
    }

    private void processRotate(float degree) {
        degree = adjustDegreeToSkipOffset(degree);
        if (degree == 0) {
            return;
        }
        mTotalDegree = (mTotalDegree + degree) % 360;
        mUpdateMatrix.postRotate(degree, mCenterPoint.x, mCenterPoint.y);
        updateLocationDataAndRefresh();
    }

    private float adjustDegreeToSkipOffset(float degree) {
        /*
        * 当处于水平或竖直方向时，往范围外旋转累积达到范围外时，移动，否则保持不动
        * 当处于范围外时，一到范围内的边界，立马旋转成水平或竖直方向
        * 一个变量保持手指累积的旋转角度
        * */

        // 当前处于水平或竖直方向
        mFingerDegree = (mFingerDegree + degree) % 360;
        if (mTotalDegree % 90 == 0) {
            for (int i = -270; i <= 360; i += 90) {
                // 如果当前手指的累积的角度在范围外，移动
                if (mTotalDegree == i) {
                    if (mFingerDegree >= i - OFFSET_DEGREE && mFingerDegree <= i + OFFSET_DEGREE) {
                        return 0;
                    } else {
                        // 一次性返回需要旋转的角度
                        return mFingerDegree - i;
                    }
                }
            }
        }

        // 检查是否进入水平或竖直区域
        float afterTotalDegree = (mTotalDegree + degree) % 360;
        for (int i = -270; i <= 360; i += 90) {
            if (afterTotalDegree >= i - OFFSET_DEGREE && afterTotalDegree <= i + OFFSET_DEGREE) {
                afterTotalDegree = i - mTotalDegree;
                return afterTotalDegree;
            }
        }
        return degree;
    }

    private float getMax(float[] values) {
        float maxValue = values[0];
        for (int i = 1; i < values.length; i++) {
            if (maxValue < values[i]) {
                maxValue = values[i];
            }
        }
        return maxValue;
    }

    private float getMin(float[] values) {
        float minValue = values[0];
        for (int i = 1; i < values.length; i++) {
            if (values[i] < minValue) {
                minValue = values[i];
            }
        }
        return minValue;
    }

    // 字体相关参数改变
    private void updateTextPaint(float scale) {
        mTextSize = mTextPaint.getTextSize() * scale;
        mTextPaint.setTextSize(mTextSize);
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
        if (!mDebug) {
            return;
        }
        Log.i(TAG, text);
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
         * 字幕边框区域被点击时调用
         *
         * @param captionView 字幕控件
         */
        void onInsideClick(FlexibleCaptionView captionView);

        /**
         * 字幕左上角被点击时调用
         *
         * @param captionView 字幕控件
         */
        void onLeftTopClick(FlexibleCaptionView captionView);
    }

    /**
     * 字幕平移监听器
     */
    public interface OnCaptionTranslateListener {

        /**
         * 开始
         *
         * @param captionView 字幕控件
         */
        void onStart(FlexibleCaptionView captionView);

        /**
         * 结束
         *
         * @param captionView 字幕控件
         */
        void onEnd(FlexibleCaptionView captionView);
    }

}
