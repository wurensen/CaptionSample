package com.meitu.captionlayout;

import java.util.ArrayList;

import android.content.Context;
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
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;

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
    private int borderWidth = 200;
    private int borderHeight = 100;

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
    private Bitmap tempBitmap;
    private int iconSize;

    private TextPaint textPaint; // 写字幕的笔
    private String text;
    private float textStrokeWidth = 1;
    private float textSize;
    private float textDegress;
    private int textPadding = 20; // 单位px

    private TouchRegion touchRegion; // 触摸的区域
    private boolean focus = true;

    public FlexibleCaptionView(Context context) {
        this(context, null);
    }

    public FlexibleCaptionView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FlexibleCaptionView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        // TODO: 2015/11/20 获取自定义属性
        if (attrs != null) {

        }

        init();
    }

    private void init() {
        initPaint();

        iconSize =
                (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 50f, getResources().getDisplayMetrics());

        // TODO: 2015/11/20 图标和文字通过外部传入
        tempBitmap = BitmapFactory.decodeResource(getResources(), android.R.drawable.ic_delete);
        text = "测试文字,eD，jP";
        textSize = 40;
    }

    private void initPaint() {
        // 边框画笔
        borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setColor(Color.GRAY);

        // 图标画笔
        bitmapPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        bitmapPaint.setDither(true);

        // 文字画笔
        textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(Color.BLACK);
        textPaint.setStrokeWidth(textStrokeWidth);
        textPaint.setTextSize(textSize);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        // TODO: 2015/11/20 是否需要支持wrap_content
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        log("onLayout");
        super.onLayout(changed, left, top, right, bottom);
        initBorderRect();
        updateBorderVertexData();
        updateCornerLocationData();
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
        log("onDraw");
        drawText(canvas);
        if (focus) {
            drawBorderRect(canvas);
            drawCornerIcon(canvas, tempBitmap, tempBitmap, tempBitmap);
        }
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

    private void drawText(Canvas canvas) {
        // TODO: 2015/11/20 写字
        canvas.save();
        Path textPath = new Path();
        textPath.moveTo(leftBottomPoint.x, leftBottomPoint.y);
        textPath.lineTo(rightBottomPoint.x, rightBottomPoint.y);
        canvas.drawTextOnPath(text, textPath, 20, 20, textPaint);
        canvas.restore();
        testDrawText(canvas);
    }

    private void testDrawText(Canvas canvas) {
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStrokeWidth(textStrokeWidth);
        paint.setTextSize(40);
        Paint.FontMetricsInt fmi = paint.getFontMetricsInt();
        String testString = "测试：ijkJQKA:1234";
        Rect bounds1 = new Rect();
        paint.getTextBounds("测", 0, 1, bounds1);
        log(bounds1.toShortString());
        Rect bounds2 = new Rect();
        paint.getTextBounds("测试：ijk", 0, 6, bounds2);
        log(bounds2.toShortString());
        // 随意设一个位置作为baseline
        int x = 0;
        int y = 100;
        // 把testString画在baseline上
        canvas.drawText(testString, x, y, paint);
        // bounds1
        paint.setStyle(Paint.Style.STROKE); // 画空心矩形
        canvas.save();
        canvas.translate(x, y); // 注意这里有translate。getTextBounds得到的矩形也是以baseline为基准的
        paint.setColor(Color.GREEN);
        canvas.drawRect(bounds1, paint);
        canvas.restore();
        // bounds2
        canvas.save();
        paint.setColor(Color.MAGENTA);
        canvas.translate(x, y);
        canvas.drawRect(bounds2, paint);
        canvas.restore();
        // baseline
        paint.setColor(Color.RED);
        canvas.drawLine(x, y, 1024, y, paint);
        // ascent
        paint.setColor(Color.YELLOW);
        canvas.drawLine(x, y + fmi.ascent, 1024, y + fmi.ascent, paint);
        // descent
        paint.setColor(Color.BLUE);
        canvas.drawLine(x, y + fmi.descent, 1024, y + fmi.descent, paint);
        // top
        paint.setColor(Color.DKGRAY);
        canvas.drawLine(x, y + fmi.top, 1024, y + fmi.top, paint);
        // bottom
        paint.setColor(Color.GREEN);
        canvas.drawLine(x, y + fmi.bottom, 1024, y + fmi.bottom, paint);
    }

    private void drawCornerIcon(Canvas canvas, Bitmap leftTopBitmap, Bitmap rightTopBitmap, Bitmap rightBottomBitmap) {
        canvas.drawBitmap(leftTopBitmap, null, currentLeftTopIconRect, bitmapPaint);
        canvas.drawBitmap(rightTopBitmap, null, currentRightTopIconRect, bitmapPaint);
        canvas.drawBitmap(rightBottomBitmap, null, currentRightBottomRect, bitmapPaint);
    }

    private void initBorderRect() {
        determineBorderSize();
        int width = getWidth();
        int height = getHeight();
        float rectLeft = (width - borderWidth) / 2;
        float rectRight = rectLeft + borderWidth;
        float rectTop = (height - borderHeight) / 2;
        float rectBottom = rectTop + borderHeight;
        borderRect.set(rectLeft, rectTop, rectRight, rectBottom);
    }

    private void determineBorderSize() {
        // TODO: 2015/11/20 根据文字的个数和大小来确定宽高

        Paint.FontMetricsInt fmi = textPaint.getFontMetricsInt();
    }

    ArrayList<OneLineText> multiLines = new ArrayList<>();

    private void cutToMultiLinesIfNeed() {
        multiLines.clear();
        int availableWidthSpace = getWidth() - textPadding * 2;
        float[] textWidths = new float[text.length()];
        textPaint.getTextWidths(text, 0, text.length(), textWidths);
        float totalWidths = 0;
        for (int i = 0; i <= textWidths.length; i++) {
            totalWidths += textWidths[i];
            if (totalWidths > availableWidthSpace) {
                // 文字超过一行，记录下该行信息，换行
                OneLineText oneLine = new OneLineText();
                oneLine.text = text.substring(0, i);
                multiLines.add(oneLine);
                i--;
                totalWidths = 0;
            }
        }
    }

    private class OneLineText {
        public String text;
        public Path textPath = new Path();
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

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float curX = event.getX();
        float curY = event.getY();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                determineTouchRegion(curX, curY);
                break;
            case MotionEvent.ACTION_MOVE:
                onMove(curX, curY);
                break;
            case MotionEvent.ACTION_UP:
                break;
        }
        lastX = curX;
        lastY = curY;
        return true;
    }

    private void onMove(float curX, float curY) {
        switch (touchRegion) {
            case INSIDE:
                processMove(curX - lastX, curY - lastY);
                break;
            case OUTSIDE:
                break;
            case LEFT_TOP_ICON:
                break;
            case RIGHT_TOP_ICON:
                break;
            case RIGHT_BOTTOM_ICON:
                onTouchRightBottomIcon(curX, curY);
                break;
        }
    }

    private void onTouchRightBottomIcon(float curX, float curY) {
        float scale = calculateScale(curX, curY);
        processScale(scale);
        float degree = calculateRotationDegree(curX, curY);
        processRotate(degree);
    }

    // 计算旋转角度
    private float calculateRotationDegree(float curX, float curY) {
        // 根据斜率算夹角
        float degree =
                (float) Math.toDegrees(Math.atan2(curY - centerPoint.y, curX - centerPoint.x))
                        - (float) Math.toDegrees(Math.atan2(lastY - centerPoint.y, lastX - centerPoint.x));
        log("degree=" + degree);
        return degree;
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
    private void determineTouchRegion(float curX, float curY) {
        log("curX=" + curX + ",curY=" + curY);
        setFocus(true);
        if (currentLeftTopIconRect.contains(curX, curY)) {
            touchRegion = TouchRegion.LEFT_TOP_ICON;
        } else if (currentRightTopIconRect.contains(curX, curY)) {
            touchRegion = TouchRegion.RIGHT_TOP_ICON;
        } else if (currentRightBottomRect.contains(curX, curY)) {
            touchRegion = TouchRegion.RIGHT_BOTTOM_ICON;
        } else if (isInBorderRegion(curX, curY)) {
            // 内容与角落重叠区域相应角落事件
            touchRegion = TouchRegion.INSIDE;
        } else {
            touchRegion = TouchRegion.OUTSIDE;
            setFocus(false);
        }
        log("touchRegion=" + touchRegion.name());
    }

    private void setFocus(boolean checked) {
        if (this.focus == checked) {
            return;
        }
        this.focus = checked;
        invalidate();
    }

    // 判断触摸点是否在边框区域内
    private boolean isInBorderRegion(float curX, float curY) {
        // 构造一个区域对象，左闭右开的。
        RectF r = new RectF();
        // 计算控制点的边界
        borderPath.computeBounds(r, true);
        log("borderPath.computeBounds,RectF" + r.toShortString());
        // 设置区域路径和剪辑描述的区域
        borderRegion.setPath(borderPath, new Region((int) r.left, (int) r.top, (int) r.right, (int) r.bottom));
        // 判断触摸点是否在封闭的path内 在返回true 不在返回false
        return borderRegion.contains((int) curX, (int) curY);
    }

    private void processMove(float dx, float dy) {
        updateMatrix.postTranslate(dx, dy);
        updateLocationDataAndRefresh();
    }

    private void processScale(float scale) {
        updateMatrix.postScale(scale, scale, centerPoint.x, centerPoint.y);
        updateTextPaint(scale);
        updateLocationDataAndRefresh();
    }

    private void processRotate(float degree) {
        log("processRotate before:" + updateMatrix.toShortString());
        updateMatrix.postRotate(degree, centerPoint.x, centerPoint.y);
        log("processRotate after:" + updateMatrix.toShortString());
        // 字体角度
        textDegress += degree;
        updateLocationDataAndRefresh();
    }

    // 字体相关参数改变
    private void updateTextPaint(float scale) {
        textStrokeWidth *= scale;
        textSize *= scale;
        textPadding *= scale;
        textPaint.setStrokeWidth(textStrokeWidth);
        textPaint.setTextSize(textSize);
    }

    // 更新位置信息
    private void updateLocationDataAndRefresh() {
        updateBorderVertexData();
        updateCornerLocationData();

        invalidate();
    }

    // 更新边框顶点位置
    private void updateBorderVertexData() {
        // 根据矩阵变化，映射到新的顶点位置
        float[] dst = new float[8];
        updateMatrix.mapPoints(dst, new float[]{borderRect.left, borderRect.top, borderRect.right, borderRect.top,
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
        log("centerPoint=" + centerPoint.toString());
    }

    private void log(String text) {
        Log.d(TAG, text);
    }

    /**
     * 触摸事件的区域
     */
    enum TouchRegion {
        LEFT_TOP_ICON, RIGHT_TOP_ICON, RIGHT_BOTTOM_ICON, INSIDE, OUTSIDE
    }
}
