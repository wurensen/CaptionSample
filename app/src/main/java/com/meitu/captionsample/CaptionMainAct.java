package com.meitu.captionsample;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.meitu.captionlayout.CaptionInfo;
import com.meitu.captionlayout.CaptionLayout;
import com.meitu.captionlayout.FlexibleCaptionView;
import com.meitu.captionlayout.FlexibleCaptionView.OnCaptionClickListener;

public class CaptionMainAct extends Activity {

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
        captionLayoutContainer = getView(R.id.captionLayout_container);
        captionView1 = getView(R.id.captionView1);
        imgViewShow = getView(R.id.imgView_show);
        labelExportInfo = getView(R.id.label_export_info);

        registerMonitor();
    }

    private void registerMonitor() {
        captionView1.setOnCaptionClickListener(onCaptionClickListener);
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
    }

    private OnCaptionClickListener onCaptionClickListener = new OnCaptionClickListener() {
        @Override
        public void onClick(FlexibleCaptionView captionView) {
            edit(captionView);
        }
    };

    private <T extends View> T getView(int id) {
        return (T) findViewById(id);
    }

    public void addCaption(View view) {
        intent2EditCaptionAct(true, null);
    }

    public void addImgCaption(View view) {
        FlexibleCaptionView imgCaptionView = FlexibleCaptionView.Builder.create(this)
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
            Log.w("", captionInfo.toString());
            imgViewShow.setImageBitmap(captionInfo.captionBitmap);
            String info = "locationRect=" + captionInfo.locationRect.toShortString() + ",degree="
                    + captionInfo.degree;
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
        FlexibleCaptionView captionView = FlexibleCaptionView.Builder.create(this)
                .loadConfig(captionInfo)
                .icon(android.R.drawable.ic_delete, android.R.drawable.checkbox_on_background,
                        android.R.drawable.ic_menu_crop)
                .build();
        captionLayoutContainer.addCaptionView(captionView);
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
            Log.d("Flex", captionConfig.toString());
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
                        .paddingLeft(TypedValue.COMPLEX_UNIT_PX, config.paddingLeft)
                        .paddingRight(TypedValue.COMPLEX_UNIT_PX, config.paddingRight)
                        .paddingTop(TypedValue.COMPLEX_UNIT_PX, config.paddingTop)
                        .paddingBottom(TypedValue.COMPLEX_UNIT_PX, config.paddingBottom)
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
        captionView.setBorderColor(Color.RED);
        captionView.setIconSizeDp(40);
        captionView.setLeftTopIcon(android.R.drawable.ic_delete);
        captionView.setRightTopIcon(android.R.drawable.checkbox_on_background);
        captionView.setRightBottomIcon(android.R.drawable.ic_menu_crop);
    }

}
