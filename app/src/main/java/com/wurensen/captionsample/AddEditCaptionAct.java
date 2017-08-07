package com.wurensen.captionsample;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.RadioGroup;

import com.wurensen.captionlayout.FlexibleCaptionView;

/**
 * 新增编辑字幕
 * Created by wrs on 2015/11/26.
 */
public class AddEditCaptionAct extends Activity {

    public static Typeface[] typefaces = new Typeface[] {Typeface.DEFAULT, Typeface.SANS_SERIF, Typeface.SERIF,
        Typeface.MONOSPACE};
    public static int[] typefaceStyles = new int[] {Typeface.NORMAL, Typeface.BOLD, Typeface.ITALIC,
        Typeface.BOLD_ITALIC};

    public static EditText ediTxtCaption;
    private RadioGroup rdoGroupColor, rdoGroupTypeFace, rdoGroupStyle;
    private FrameLayout framePreview;

    boolean isAdd;
    float textSize;
    int textColor = Color.BLACK;
    int typefaceIndex = 0;
    int typefaceStyleIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.act_add_edit_caption);
        initView();
        registerMonitor();
        initData();
    }

    private void initData() {
        isAdd = getIntent().getBooleanExtra("isAdd", true);
        if (!isAdd) {
            String caption = getIntent().getStringExtra("caption");
            ediTxtCaption.setText(caption);
            ediTxtCaption.setSelection(caption.length());
        }

    }

    private void initView() {
        ediTxtCaption = getView(R.id.edit_caption);
        rdoGroupColor = getView(R.id.rdo_group_color);
        rdoGroupTypeFace = getView(R.id.rdo_group_typeface);
        rdoGroupStyle = getView(R.id.rdo_group_style);
        framePreview = getView(R.id.frame_preview);
        addCaptionPreviewView();
    }

    private FlexibleCaptionView captionPreview;

    private void addCaptionPreviewView() {
        String text = ediTxtCaption.getText().toString();
        if (TextUtils.isEmpty(text)) {
            text = ediTxtCaption.getHint().toString();
        }
        Typeface typeface =
            Typeface.create(AddEditCaptionAct.typefaces[typefaceIndex],
                AddEditCaptionAct.typefaceStyles[typefaceStyleIndex]);
        // 没有设置边框宽度textBorderWidth，默认为根据文字的宽度和padding来确定，但最大不会超过（字幕宽度-默认间距）；
        captionPreview =
            FlexibleCaptionView.Builder.create(this)
                .text(text)
                .textSize(TypedValue.COMPLEX_UNIT_PX, ediTxtCaption.getTextSize())
                .textColor(ediTxtCaption.getCurrentTextColor())
                .textBorderColor(Color.BLACK)
                .textTypeface(typeface)
                .build();
        // 屏蔽事件
        captionPreview.setEnable(false);
        // captionPreview.setText(text);
        framePreview.addView(captionPreview);
    }

    private void registerMonitor() {
        rdoGroupColor.setOnCheckedChangeListener(checkedChangeListener);
        rdoGroupTypeFace.setOnCheckedChangeListener(checkedChangeListener);
        rdoGroupStyle.setOnCheckedChangeListener(checkedChangeListener);
        ediTxtCaption.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String text = s.toString();
                if (s.length() == 0) {
                    text = ediTxtCaption.getHint().toString();
                }
                captionPreview.setText(text);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private RadioGroup.OnCheckedChangeListener checkedChangeListener = new RadioGroup.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(RadioGroup group, int checkedId) {
            switch (group.getId()) {
                case R.id.rdo_group_color:
                    setCheckedColor(checkedId);
                    break;
                case R.id.rdo_group_typeface:
                    setCheckedTypeFace(checkedId);
                    break;
                case R.id.rdo_group_style:
                    setCheckedTypeFaceStyle(checkedId);
                    break;
            }
        }
    };

    public void setCheckedColor(int checkedId) {
        switch (checkedId) {
            case R.id.rdo_btn_color_black:
                textColor = Color.BLACK;
                break;
            case R.id.rdo_btn_color_blue:
                textColor = Color.BLUE;
                break;
            case R.id.rdo_btn_color_red:
                textColor = Color.RED;
                break;
            case R.id.rdo_btn_color_yellow:
                textColor = Color.YELLOW;
                break;
        }
        ediTxtCaption.setTextColor(textColor);
        captionPreview.setTextColor(textColor);
    }

    public void setCheckedTypeFace(int checkedId) {
        switch (checkedId) {
            case R.id.rdo_btn_typeface_normal:
                typefaceIndex = 0;
                break;
            case R.id.rdo_btn_typeface_sans:
                typefaceIndex = 1;
                break;
            case R.id.rdo_btn_typeface_serif:
                typefaceIndex = 2;
                break;
            case R.id.rdo_btn_typeface_monospace:
                typefaceIndex = 3;
                break;
        }
        ediTxtCaption.setTypeface(typefaces[typefaceIndex], typefaceStyles[typefaceStyleIndex]);
        captionPreview.setTextTypeface(ediTxtCaption.getTypeface());
    }

    public void setCheckedTypeFaceStyle(int checkedId) {
        switch (checkedId) {
            case R.id.rdo_btn_style_normal:
                typefaceStyleIndex = 0;
                break;
            case R.id.rdo_btn_style_bold:
                typefaceStyleIndex = 1;
                break;
            case R.id.rdo_btn_style_italic:
                typefaceStyleIndex = 2;
                break;
            case R.id.rdo_btn_style_bold_italic:
                typefaceStyleIndex = 3;
                break;
        }
        ediTxtCaption.setTypeface(typefaces[typefaceIndex], typefaceStyles[typefaceStyleIndex]);
        captionPreview.setTextTypeface(ediTxtCaption.getTypeface());
    }

    public void finish(View view) {
        String caption = ediTxtCaption.getText().toString();
        if (!TextUtils.isEmpty(caption)) {
            textSize = ediTxtCaption.getTextSize();
            Intent data = new Intent();
            data.putExtra("isAdd", isAdd);
            int textColor = ediTxtCaption.getCurrentTextColor();
            int paddingLeft = ediTxtCaption.getPaddingLeft();
            int paddingRight = ediTxtCaption.getPaddingRight();
            int paddingTop = ediTxtCaption.getPaddingTop();
            int paddingBottom = ediTxtCaption.getPaddingBottom();
            data.putExtra(CaptionConfig.class.getSimpleName(), new CaptionConfig(caption, textSize, textColor,
                typefaceIndex, typefaceStyleIndex, paddingLeft, paddingRight, paddingTop, paddingBottom));
            setResult(RESULT_OK, data);
            finish();
        }
    }

    private <T extends View> T getView(int id) {
        return (T) findViewById(id);
    }
}
