package com.meitu.captionsample;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Layout;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.meitu.captionlayout.CaptionInfo;
import com.meitu.captionlayout.CaptionLayout;
import com.meitu.captionlayout.FlexibleCaptionView;
import com.meitu.captionlayout.FlexibleCaptionView.OnCaptionClickListener;
import com.meitu.captionlayout.FlexibleCaptionView.OnCaptionTranslateListener;

public class CaptionMainAct extends Activity {

    private LinearLayout linear_btn_container;
    private CaptionLayout captionLayoutContainer;
    private FlexibleCaptionView captionView1;
    private ImageView imgViewShow;
    private TextView labelExportInfo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.act_caption_main);

        initView();
    }

    private void initView() {
        linear_btn_container = getView(R.id.linear_btn_container);
        captionLayoutContainer = getView(R.id.captionLayout_container);
        captionView1 = getView(R.id.captionView1);
        imgViewShow = getView(R.id.imgView_show);
        labelExportInfo = getView(R.id.label_export_info);

        registerMonitor();
    }

    private void registerMonitor() {
        captionView1.setOnCaptionClickListener(onCaptionClickListener);
        captionView1.setOnCaptionGestureListener(onCaptionTranslateListener);
        captionLayoutContainer.setOnCaptionFocusChangeListener(new CaptionLayout.OnCaptionFocusChangeListener() {
            @Override
            public void onCaptionFocusChange(CaptionLayout captionLayout, FlexibleCaptionView lastFocusCaptionView,
                FlexibleCaptionView curFocusCaptionView) {
                CharSequence lastText = lastFocusCaptionView != null ? lastFocusCaptionView.getText() : null;
                CharSequence curText = curFocusCaptionView != null ? curFocusCaptionView.getText() : null;
                Log.e("Flex", "onCaptionFocusChange...lastFocusCaptionView=" + lastText + ",curFocusCaptionView="
                    + curText);
            }
        });
        captionLayoutContainer.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (captionInfo != null) {
                    Log.e(
                        "flex",
                        "captionInfo.isPointInIntrinsicRect="
                            + captionInfo.isPointInIntrinsicRect(v.getWidth(), v.getHeight(), event.getX(),
                                event.getY()));
                    if (captionInfo.isPointInIntrinsicRect(v.getWidth(), v.getHeight(), event.getX(), event.getY())) {
                        importCaption(null);
                    }
                }
                return false;
            }
        });
    }

    private OnCaptionTranslateListener onCaptionTranslateListener = new OnCaptionTranslateListener() {
        @Override
        public void onStart(FlexibleCaptionView captionView) {
            Log.e("flex", "onGestureStart");
            linear_btn_container.setVisibility(View.GONE);
        }

        @Override
        public void onEnd(FlexibleCaptionView captionView) {
            Log.e("flex", "onGestureEnd");
            linear_btn_container.setVisibility(View.VISIBLE);
        }
    };

    private OnCaptionClickListener onCaptionClickListener = new OnCaptionClickListener() {
        @Override
        public void onInsideClick(FlexibleCaptionView captionView) {
            Log.e("flex", "onInsideClick");
            edit(captionView);
        }

        @Override
        public void onLeftTopClick(FlexibleCaptionView captionView) {
            Log.e("flex", "onLeftTopClick");
            captionLayoutContainer.removeCaptionView(captionView);
        }

    };

    private <T extends View> T getView(int id) {
        return (T) findViewById(id);
    }

    public void addCaption(View view) {
        intent2EditCaptionAct(true, null);
    }

    public void addImgCaption(View view) {
        FlexibleCaptionView imgCaptionView =
            FlexibleCaptionView.Builder.create(this)
                .icon(android.R.drawable.ic_delete, android.R.drawable.checkbox_on_background,
                    android.R.drawable.ic_menu_crop)
                .imgCaption(android.R.drawable.ic_input_add)
                .build();
        captionLayoutContainer.addCaptionView(imgCaptionView);
    }

    public void edit(View view) {
        FlexibleCaptionView captionView = captionLayoutContainer.getCurrentFocusCaptionView();
        if (captionView != null) {
            intent2EditCaptionAct(false, captionView.getText().toString());
        }
    }

    private CaptionInfo captionInfo;

    public void export(View view) {
        FlexibleCaptionView captionView = captionLayoutContainer.getCurrentFocusCaptionView();
        if (captionView != null) {
            captionInfo = captionView.exportCaptionInfo(0.5f);
            imgViewShow.setImageBitmap(captionInfo.captionBitmap);
            String info = "targetRect=" + captionInfo.targetRect.toShortString() + "\ndegree=" + captionInfo.degree;
            labelExportInfo.setText(info);
        } else {
            captionInfo = null;
            imgViewShow.setImageBitmap(null);
            labelExportInfo.setText(null);
        }
    }

    public void importCaption(View view) {
        if (captionInfo == null) {
            return;
        }
        FlexibleCaptionView captionView =
            FlexibleCaptionView.Builder.create(this)
                .loadConfig(captionInfo)
                .icon(android.R.drawable.ic_delete, android.R.drawable.checkbox_on_background,
                    android.R.drawable.ic_menu_crop)
                .build();
        captionView.setOnCaptionClickListener(onCaptionClickListener);
        captionView.setOnCaptionGestureListener(onCaptionTranslateListener);
//        captionLayoutContainer.addView(captionView, new FrameLayout.LayoutParams(captionLayoutContainer.getWidth() / 2,
//            captionLayoutContainer.getHeight() / 2));
        captionLayoutContainer.addCaptionView(captionView);
    }

    public void clearFocus(View view) {
        if (captionLayoutContainer.getCurrentFocusCaptionView() != null) {
            captionLayoutContainer.getCurrentFocusCaptionView().setFocus(false);
        }
    }

    private void intent2EditCaptionAct(boolean isAdd, String caption) {
        Intent intent = new Intent(this, AddEditCaptionAct.class);
        intent.putExtra("isAdd", isAdd);
        intent.putExtra("caption", caption);
        startActivityForResult(intent, 0);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            boolean isAdd = data.getBooleanExtra("isAdd", true);
            CaptionConfig captionConfig =
                (CaptionConfig) data.getSerializableExtra(CaptionConfig.class.getSimpleName());
            if (isAdd) {
                // 1.直接根据EditText的参数来创建字幕
                // addCaptionWidthEditText();
                // 2.通过Builder的方式创建
                addCaptionWithBuilder(captionConfig);
            } else {
                updateCaption(captionConfig);
            }
        }
    }

    private void addCaptionWidthEditText() {
        FlexibleCaptionView addCaptionView =
            FlexibleCaptionView.Builder.create(this)
                .loadConfig(AddEditCaptionAct.ediTxtCaption)
                .icon(android.R.drawable.ic_delete, android.R.drawable.checkbox_on_background,
                    android.R.drawable.ic_menu_crop)
                .build();
        captionLayoutContainer.addCaptionView(addCaptionView);
    }

    private void addCaptionWithBuilder(CaptionConfig config) {
        Typeface typeface =
            Typeface.create(AddEditCaptionAct.typefaces[config.typefaceIndex],
                AddEditCaptionAct.typefaceStyles[config.typefaceStyleIndex]);
        FlexibleCaptionView addCaptionView =
            FlexibleCaptionView.Builder.create(this)
                .text(config.text)
                .textSize(TypedValue.COMPLEX_UNIT_PX, config.textSize)
                .textBorderWidth(config.textBorderWidth)
                .textColor(config.textColor)
                .textTypeface(typeface)
                .icon(android.R.drawable.ic_delete, android.R.drawable.checkbox_on_background,
                    android.R.drawable.ic_menu_crop)
                .iconSize(TypedValue.COMPLEX_UNIT_DIP, 35)
                .textBorderColor(Color.MAGENTA)
                .build();
        addCaptionView.setOnCaptionClickListener(onCaptionClickListener);
        captionLayoutContainer.addCaptionView(addCaptionView);
    }

    private void updateCaption(CaptionConfig config) {
        FlexibleCaptionView captionView = captionLayoutContainer.getCurrentFocusCaptionView();
        if (captionView != null) {
            captionView.setText(config.text);
            // captionView.setTextBorderWidth(config.textBorderWidth);
            // captionView.setTextSize(TypedValue.COMPLEX_UNIT_PX, config.textSize);
            captionView.setTextColor(config.textColor);
            Typeface typeface =
                Typeface.create(AddEditCaptionAct.typefaces[config.typefaceIndex],
                    AddEditCaptionAct.typefaceStyles[config.typefaceStyleIndex]);
            captionView.setTextTypeface(typeface);
        }
    }

    private void configCaptionView(FlexibleCaptionView captionView, String caption, float textSize, int textColor,
        Typeface typeFace) {
        captionView.setText(caption);
        captionView.setTextColor(textColor);
        captionView.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize);
        captionView.setTextTypeface(typeFace);
        captionView.setLayoutTextAlignment(Layout.Alignment.ALIGN_CENTER);
        captionView.setBorderColor(Color.RED);
        captionView.setIconSizeDp(40);
        captionView.setLeftTopIcon(android.R.drawable.ic_delete);
        captionView.setRightTopIcon(android.R.drawable.checkbox_on_background);
        captionView.setRightBottomIcon(android.R.drawable.ic_menu_crop);
    }

}
