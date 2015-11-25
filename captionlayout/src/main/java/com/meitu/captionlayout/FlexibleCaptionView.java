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
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.FloatMath;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

/**
 * 字幕控件，支持移动、旋转、缩放
 * Created by wrs on 2015/11/18.
 */
public class FlexibleCaptionView extends View {

    private static final String TAG = FlexibleCaptionView.class.getSimpleName();

    private Matrix updateMatrix = new Matrix(); // 变化矩阵，用来获取最新的点

    private Paint borderPaint; // 画矩形的笔
    private Region borderRegion = new Region();
    private Path borderPath = new Path();
    private int borderColor = Color.GRAY;

    private PointF centerPoint = new PointF(); // 矩形中心坐标

    private PointF leftTopPoint = new PointF();
    private PointF rightTopPoint = new PointF();
    private PointF leftBottomPoint = new PointF();
    private PointF rightBottomPoint = new PointF();

    private RectF borderRect = new RectF(); // 初始位置
    private RectF currentLeftTopIconRect = new RectF();
    private RectF currentRightTopIconRect = new RectF();
    private RectF currentRightBottomRect = new RectF();

    private Paint bitmapPaint;
    private Bitmap leftTopBmp, rightTopBmp, rightBottomBmp;
    private int iconSize;

    private TextPaint textPaint; // 写字幕的笔
    private String text = "";
    private float textStrokeWidth = 1;
    private float textSize = 50f;
    private int textPadding = 10;
    private int textColor = Color.BLACK;
    private ArrayList<OneLineText> multiLines = new ArrayList<>();
    private int oneLineHeight;
    private int baselineHeight;

    private TouchRegion touchRegion; // 触摸的区域
    private TouchMode touchMode = TouchMode.NONE; // 触摸模式
    private boolean focus = true;

    private float totalScale;
    private float totalDegree;
    private boolean neverDraw = true;
    private boolean reInit = true; // 是否需要重新初始化

    public FlexibleCaptionView(Context context) {
        this(context, null);
    }

    public FlexibleCaptionView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FlexibleCaptionView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        // 设置默认属性值
        iconSize = (int) convertDp2Px(30f);

        if (attrs != null) {
            TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.FlexibleCaptionView);
            int count = typedArray.getIndexCount();
            for (int i = 0; i < count; i++) {
                int attr = typedArray.getIndex(i);
                if (attr == R.styleable.FlexibleCaptionView_text) {
                    text = typedArray.getString(attr);
                } else if (attr == R.styleable.FlexibleCaptionView_textSize) {
                    textSize = typedArray.getDimensionPixelSize(attr, (int) textSize);
                } else if (attr == R.styleable.FlexibleCaptionView_textColor) {
                    textColor = typedArray.getColor(attr, textColor);
                } else if (attr == R.styleable.FlexibleCaptionView_textPadding) {
                    textPadding = typedArray.getDimensionPixelOffset(attr, textPadding);
                } else if (attr == R.styleable.FlexibleCaptionView_borderColor) {
                    borderColor = typedArray.getColor(attr, borderColor);
                } else if (attr == R.styleable.FlexibleCaptionView_leftTopIcon) {
                    leftTopBmp = BitmapFactory.decodeResource(getResources(), typedArray.getResourceId(attr, 0));
                } else if (attr == R.styleable.FlexibleCaptionView_rightTopIcon) {
                    rightTopBmp = BitmapFactory.decodeResource(getResources(), typedArray.getResourceId(attr, 0));
                } else if (attr == R.styleable.FlexibleCaptionView_rightBottomIcon) {
                    rightBottomBmp = BitmapFactory.decodeResource(getResources(), typedArray.getResourceId(attr, 0));
                } else if (attr == R.styleable.FlexibleCaptionView_iconSize) {
                    iconSize = typedArray.getDimensionPixelSize(attr, iconSize);
                }
            }
            typedArray.recycle();
        }

        initPaint();

    }

    private float convertDp2Px(float dp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }

    private float converSp2Px(float sp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, getResources().getDisplayMetrics());
    }

    private void initPaint() {
        // 边框画笔
        borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(textStrokeWidth);
        borderPaint.setColor(borderColor);

        // 图标画笔
        bitmapPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bitmapPaint.setDither(true);

        // 文字画笔
        textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(textColor);
        textPaint.setStrokeWidth(textStrokeWidth);
        textPaint.setTextSize(textSize);
    }

    /**
     * 设置文本内容
     *
     * @param text 文本内容
     */
    public void setText(String text) {
        this.text = text;
        refresh(true);
    }

    public String getText() {
        return text;
    }

    public int getTextColor() {
        return textColor;
    }

    public void setTextColor(int textColor) {
        this.textColor = textColor;
        textPaint.setColor(textColor);
        refresh(false);
    }

    public float getTextSize() {
        return textSize;
    }

    public void setTextSizePx(float textSize) {
        this.textSize = textSize;
        textPaint.setTextSize(textSize);
        refresh(true);
    }

    public void setTextSizeDp(float textSize) {
        setTextSizePx(convertDp2Px(textSize));
    }

    public void setTextSizeSp(float textSize) {
        setTextSizePx(converSp2Px(textSize));
    }

    public int getTextPadding() {
        return textPadding;
    }

    public void setTextPaddingPx(int textPadding) {
        this.textPadding = textPadding;
        refresh(true);
    }

    public void setTextPaddingDp(int textPadding) {
        setTextPaddingPx((int) convertDp2Px(textPadding));
    }

    public int getBorderColor() {
        return borderColor;
    }

    public void setBorderColor(int borderColor) {
        this.borderColor = borderColor;
        borderPaint.setColor(borderColor);
        refresh(false);
    }

    public int getIconSize() {
        return iconSize;
    }

    public void setIconSizePx(int iconSize) {
        this.iconSize = iconSize;
        refresh(false);
    }

    public void setIconSizeDp(int iconSize) {
        setIconSizePx((int) convertDp2Px(iconSize));
    }

    public void setLeftTopIcon(Bitmap bitmap) {
        this.leftTopBmp = bitmap;
        refresh(false);
    }

    public void setLeftTopIcon(int id) {
        setLeftTopIcon(BitmapFactory.decodeResource(getResources(), id));
    }

    public void setRightTopIcon(Bitmap bitmap) {
        this.rightTopBmp = bitmap;
        refresh(false);
    }

    public void setRightTopIcon(int id) {
        setRightTopIcon(BitmapFactory.decodeResource(getResources(), id));
    }

    public void setRightBottomIcon(Bitmap bitmap) {
        this.rightBottomBmp = bitmap;
        refresh(false);
    }

    public void setRightBottomIcon(int id) {
        setRightBottomIcon(BitmapFactory.decodeResource(getResources(), id));
    }

    public boolean getFocus() {
        return focus;
    }

    public void setFocus(boolean focus) {
        if (this.focus == focus) {
            return;
        }
        this.focus = focus;
        refresh(false);
        if (focus) {
            // 清除别的控件的
        }
    }

    /**
     * @return 获取字幕信息
     */
    public CaptionInfo getCurrentCaption() {
        return buildCurrentCaptionInfo();
    }

    private CaptionInfo buildCurrentCaptionInfo() {
        // 暂时摆正
        updateMatrix.postRotate(-totalDegree, centerPoint.x, centerPoint.y);
        RectF dst = new RectF();
        updateMatrix.mapRect(dst, borderRect);
        Rect locationRect = new Rect((int) dst.left, (int) dst.top, (int) dst.right, (int) dst.bottom);
        // 更新字幕位置
        updateTextBaselineLocationData(multiLines, updateMatrix);
        Bitmap textBitmap = Bitmap.createBitmap(locationRect.width(), locationRect.height(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(textBitmap);
        canvas.translate(-locationRect.left, -locationRect.top);
        drawText(canvas, multiLines);
        CaptionInfo captionInfo = new CaptionInfo(textBitmap, locationRect, totalDegree);
        // 恢复位置
        updateMatrix.postRotate(totalDegree, centerPoint.x, centerPoint.y);
        updateTextBaselineLocationData(multiLines, updateMatrix);
        return captionInfo;
    }

    /**
     * 重绘控件
     *
     * @param reInit 内部是否需要重新初始化数据
     */
    private void refresh(boolean reInit) {
        this.reInit = reInit;
        postInvalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        log("onMeasure");
        // TODO: 2015/11/20 是否需要支持wrap_content
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
        if (neverDraw || reInit) {
            neverDraw = false;
            reInit = false;
            init();
        }
        drawText(canvas, multiLines);
        if (focus) {
            drawBorderRect(canvas);
            drawCornerIcon(canvas, leftTopBmp, rightTopBmp, rightBottomBmp);
        }
    }

    private void init() {
        log("init");
        updateMatrix.reset();
        textPaint.setTextSize(textSize);
        textPaint.setColor(textColor);
        textPaint.setStrokeWidth(textStrokeWidth);
        totalScale = 1;
        totalDegree = 0;

        initBorderRect();
        updateBorderVertexData();
        determineTextInitBaselineLocation(baselineHeight, oneLineHeight);
        updateCornerLocationData();
    }

    private void drawBorderRect(Canvas canvas) {
        borderPath.reset();
        borderPath.moveTo(leftTopPoint.x, leftTopPoint.y);
        borderPath.lineTo(rightTopPoint.x, rightTopPoint.y);
        borderPath.lineTo(rightBottomPoint.x, rightBottomPoint.y);
        borderPath.lineTo(leftBottomPoint.x, leftBottomPoint.y);
        borderPath.lineTo(leftTopPoint.x, leftTopPoint.y);
        canvas.drawPath(borderPath, borderPaint);
    }

    private void drawText(Canvas canvas, ArrayList<OneLineText> multiLines) {
        Path textPath = new Path();
        for (OneLineText oneLineText : multiLines) {
            textPath.moveTo(oneLineText.drawBaselineStartPoint.x, oneLineText.drawBaselineStartPoint.y);
            textPath.lineTo(oneLineText.drawBaselineEndPoint.x, oneLineText.drawBaselineEndPoint.y);
            canvas.drawTextOnPath(oneLineText.text, textPath, 0, 0, textPaint);
            textPath.reset();
        }
    }

    private void drawCornerIcon(Canvas canvas, Bitmap leftTopBitmap, Bitmap rightTopBitmap, Bitmap rightBottomBitmap) {
        if (leftTopBitmap != null) {
            canvas.drawBitmap(leftTopBitmap, null, currentLeftTopIconRect, bitmapPaint);
        }
        if (rightTopBitmap != null) {
            canvas.drawBitmap(rightTopBitmap, null, currentRightTopIconRect, bitmapPaint);
        }
        if (rightBottomBitmap != null) {
            canvas.drawBitmap(rightBottomBitmap, null, currentRightBottomRect, bitmapPaint);
        }
    }

    private void initBorderRect() {
        determineBorderSize();
    }

    private void determineBorderSize() {
        multiLines.clear();

        Paint.FontMetricsInt fmi = textPaint.getFontMetricsInt(); // 获取字体信息
        oneLineHeight = fmi.bottom - fmi.top;
        baselineHeight = -fmi.top;

        float availableWidthSpace = getWidth() - textPadding * 2; // 每行允许写文字最大空间
        float[] textWidths = new float[text.length()];
        textPaint.getTextWidths(text, 0, text.length(), textWidths);
        float totalWidths = 0;
        int lineStartIndex = 0;
        OneLineText firstLine = new OneLineText();
        multiLines.add(firstLine);
        for (int i = 0; i < textWidths.length; i++) {
            totalWidths += textWidths[i];
            if (totalWidths > availableWidthSpace) {
                // 文字超过一行，记录下该行信息
                multiLines.get(multiLines.size() - 1).text = text.substring(lineStartIndex, i);
                // 开启下一行
                OneLineText nextLine = new OneLineText();
                multiLines.add(nextLine);
                // 换行
                lineStartIndex = i;
                totalWidths = textWidths[i];
            }
        }
        multiLines.get(multiLines.size() - 1).text = text.substring(lineStartIndex, textWidths.length);

        int lineCount = multiLines.size();
        // 根据行数来确定矩形框宽高
        float borderWidth = lineCount > 1 ? getWidth() - 2 : (totalWidths + textPadding * 2);
        float borderHeight = oneLineHeight * lineCount + textPadding * 2;
        log("borderWidth:" + borderWidth + ",borderHeight=" + borderHeight);

        float rectLeft = (getWidth() - borderWidth) / 2;
        float rectRight = rectLeft + borderWidth;
        float rectTop = (getHeight() - borderHeight) / 2;
        float rectBottom = rectTop + borderHeight;
        borderRect.set(rectLeft, rectTop, rectRight, rectBottom);
    }

    // 确定文字初始化时baseline的位置
    private void determineTextInitBaselineLocation(int baselineHeight, int lineHeight) {
        OneLineText firstLine = multiLines.get(0);
        firstLine.setBaselineStartPoint(leftTopPoint.x + textPadding, leftTopPoint.y + textPadding + baselineHeight);
        firstLine.setBaselineEndPoint(rightTopPoint.x - textPadding, rightTopPoint.y + textPadding + baselineHeight);
        for (int i = 1; i < multiLines.size(); i++) {
            OneLineText curLineText = multiLines.get(i);
            OneLineText lastLineText = multiLines.get(i - 1);
            curLineText.setBaselineStartPoint(lastLineText.baselineStartPoint.x, lastLineText.baselineStartPoint.y
                + lineHeight);
            curLineText.setBaselineEndPoint(lastLineText.baselineEndPoint.x, lastLineText.baselineEndPoint.y
                + lineHeight);
        }
    }

    private void updateLeftTopIconRect() {
        int halfIconSize = iconSize / 2;
        float left = leftTopPoint.x - halfIconSize;
        float top = leftTopPoint.y - halfIconSize;
        float right = leftTopPoint.x + halfIconSize;
        float bottom = leftTopPoint.y + halfIconSize;
        currentLeftTopIconRect.set(left, top, right, bottom);
    }

    private void updateRightTopIconRect() {
        int halfIconSize = iconSize / 2;
        float left = rightTopPoint.x - halfIconSize;
        float top = rightTopPoint.y - halfIconSize;
        float right = rightTopPoint.x + halfIconSize;
        float bottom = rightTopPoint.y + halfIconSize;
        currentRightTopIconRect.set(left, top, right, bottom);
    }

    private void updateRightBottomIconRect() {
        int halfIconSize = iconSize / 2;
        float left = rightBottomPoint.x - halfIconSize;
        float top = rightBottomPoint.y - halfIconSize;
        float right = rightBottomPoint.x + halfIconSize;
        float bottom = rightBottomPoint.y + halfIconSize;
        currentRightBottomRect.set(left, top, right, bottom);
    }

    private float lastX, lastY; // 上次点击的坐标
    private float downDistance; // 按下时两指间的距离
    private int alwaysPressPointId = 0;

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
                log("ACTION_DOWN," + event.getPointerId(0));
                consume = determineTouchRegion(curX, curY);
                if (touchRegion == TouchRegion.INSIDE) {
                    touchMode = TouchMode.DRAG;
                }
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                log(event.getX() + "," + event.getY() + "," + event.getX(1) + "," + event.getY(1));
                log("ACTION_POINTER_DOWN," + event.getPointerId(0) + "," + event.getPointerId(1));
                float newPointX = event.getX(alwaysPressPointId == 0 ? 1 : 0);
                float newPointY = event.getY(alwaysPressPointId == 0 ? 1 : 0);
                if (isInBorderRegion(newPointX, newPointY)) {
                    log("ACTION_POINTER_DOWN,isInBorderRegion");
                    downDistance = calculatePointsDistance(event);
                    touchMode = TouchMode.POINTER_SCALE_ROTATE;
                    rotate = calculatePointerRotationDegree(event);
                    curX = event.getX(alwaysPressPointId);
                    curY = event.getY(alwaysPressPointId);
                }
                break;
            case MotionEvent.ACTION_MOVE:
                onMove(event);
                break;
            case MotionEvent.ACTION_POINTER_UP:
                int pointerId =
                    (event.getAction() & MotionEvent.ACTION_POINTER_INDEX_MASK) >>> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
                // 前两个按下的点有效
                if (pointerId < 2) {
                    // 初始手指离开，切换点击坐标
                    if (pointerId == alwaysPressPointId) {
                        alwaysPressPointId = alwaysPressPointId == 0 ? 1 : 0;
                        curX = event.getX(alwaysPressPointId);
                        curY = event.getY(alwaysPressPointId);
                    }
                    // 有手指离开，转为拖拽
                    if (isInBorderRegion(curX, curY)) {
                        touchMode = TouchMode.DRAG;
                    } else {
                        touchMode = TouchMode.NONE;
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
                touchMode = TouchMode.NONE;
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
            if (touchMode == TouchMode.NONE && captionLayout.isPointerDown) {
                return true;
            }
        }
        return false;
    }

    float rotate;

    private void onMove(MotionEvent event) {
        float curX = event.getX();
        float curY = event.getY();
        switch (touchRegion) {
            case INSIDE:
                if (touchMode == TouchMode.DRAG) {
                    processMove(curX - lastX, curY - lastY);
                } else if (touchMode == TouchMode.POINTER_SCALE_ROTATE) {
                    // 双指缩放
                    float curDistance = calculatePointsDistance(event);
                    float scale = curDistance / downDistance;
                    downDistance = curDistance;
                    processScale(scale);
                    // 旋转
                    // float degree = calculateRotationDegree(curX, curY);
                    float curDegree = calculatePointerRotationDegree(event);
                    float degree = curDegree - rotate;
                    rotate = curDegree;
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
        return (float) Math.toDegrees(Math.atan2(curY - centerPoint.y, curX - centerPoint.x))
            - (float) Math.toDegrees(Math.atan2(lastY - centerPoint.y, lastX - centerPoint.x));
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
        double dxPower = Math.pow((x - centerPoint.x), 2);
        double dyPower = Math.pow((y - centerPoint.y), 2);
        return Math.sqrt(dxPower + dyPower);
    }

    // 确定事件点击的区域
    private boolean determineTouchRegion(float curX, float curY) {
        boolean consume = true;
        setFocus(true);
        if (rightBottomBmp != null && currentRightBottomRect.contains(curX, curY)) {
            touchRegion = TouchRegion.RIGHT_BOTTOM_ICON;
        } else if (leftTopBmp != null && currentLeftTopIconRect.contains(curX, curY)) {
            touchRegion = TouchRegion.LEFT_TOP_ICON;
            ((ViewGroup) getParent()).removeView(this);
        } else if (isInBorderRegion(curX, curY)) {
            // 内容与角落重叠区域相应角落事件
            touchRegion = TouchRegion.INSIDE;
        } else {
            touchRegion = TouchRegion.OUTSIDE;
            consume = false;
            setFocus(false);
        }
        log(this + "curX=" + curX + ",curY=" + curY + ",touchRegion=" + touchRegion.name());
        return consume;
    }

    // 判断触摸点是否在边框区域内
    private boolean isInBorderRegion(float curX, float curY) {
        RectF r = new RectF();
        // 计算控制点的边界
        borderPath.computeBounds(r, true);
        // 设置区域路径和剪辑描述的区域
        borderRegion.setPath(borderPath, new Region((int) r.left, (int) r.top, (int) r.right, (int) r.bottom));
        return borderRegion.contains((int) curX, (int) curY);
    }

    private void processMove(float dx, float dy) {
        updateMatrix.postTranslate(dx, dy);
        updateLocationDataAndRefresh();
    }

    private void processScale(float scale) {
        if (scale == 1) {
            return;
        }
        // 检查缩放是否超过限定
        float maxScale = getWidth() * 1.0f / borderRect.width();
        float minScale = iconSize * 2.0f / borderRect.width();
        if (this.totalScale * scale > maxScale) {
            scale = maxScale / this.totalScale;
        } else if (this.totalScale * scale < minScale) {
            scale = minScale / this.totalScale;
        }
        this.totalScale *= scale;
        updateMatrix.postScale(scale, scale, centerPoint.x, centerPoint.y);
        updateTextPaint();
        updateLocationDataAndRefresh();
    }

    private void processRotate(float degree) {
        if (degree == 0) {
            return;
        }
        this.totalDegree = (this.totalDegree + degree) % 360;
        updateMatrix.postRotate(degree, centerPoint.x, centerPoint.y);
        updateLocationDataAndRefresh();
    }

    // 字体相关参数改变
    private void updateTextPaint() {
        textPaint.setStrokeWidth(textStrokeWidth * totalScale);
        textPaint.setTextSize(textSize * totalScale);
    }

    // 更新位置信息并重绘控件
    private void updateLocationDataAndRefresh() {
        updateBorderVertexData();
        updateTextBaselineLocationData(multiLines, updateMatrix);
        updateCornerLocationData();

        invalidate();
    }

    // 更新边框顶点位置
    private void updateBorderVertexData() {
        // 根据矩阵变化，映射到新的顶点位置
        float[] dst = new float[8];
        updateMatrix.mapPoints(dst, new float[] {borderRect.left, borderRect.top, borderRect.right, borderRect.top,
            borderRect.left, borderRect.bottom, borderRect.right, borderRect.bottom});
        leftTopPoint.x = dst[0];
        leftTopPoint.y = dst[1];
        rightTopPoint.x = dst[2];
        rightTopPoint.y = dst[3];
        leftBottomPoint.x = dst[4];
        leftBottomPoint.y = dst[5];
        rightBottomPoint.x = dst[6];
        rightBottomPoint.y = dst[7];

        updateCenterPoint();
    }

    private void updateCenterPoint() {
        // 根据对角的点获取中心点
        centerPoint.x = (leftTopPoint.x + rightBottomPoint.x) / 2;
        centerPoint.y = (leftTopPoint.y + rightBottomPoint.y) / 2;
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
